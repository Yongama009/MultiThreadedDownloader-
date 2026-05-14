package downloader;

import util.FileUtils;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadManager {
    private final String fileURL;
    private final String outputFile;
    private final int numberOfThreads;
    private final int maxRetries;
    private final Object pauseLock = new Object();

    private volatile boolean paused;
    private volatile DownloadStatus status = DownloadStatus.READY;
    private volatile DownloadProgress progress;
    private ExecutorService executorService;

    public DownloadManager(
            String fileURL,
            String outputFile,
            int numberOfThreads
    ) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.numberOfThreads = Math.max(1, numberOfThreads);
        this.maxRetries = 3;
    }

    public void download() {
        status = DownloadStatus.DOWNLOADING;

        FileInfo fileInfo = FileUtils.getFileInfo(fileURL);

        if (fileInfo == null) {
            System.out.println("Could not get file info");
            status = DownloadStatus.FAILED;
            return;
        }

        long fileSize = fileInfo.getFileSize();

        if (fileSize <= 0) {
            System.out.println("Could not determine file size");
            status = DownloadStatus.FAILED;
            return;
        }

        if (!fileInfo.isRangeSupported()) {
            System.out.println(
                    "Warning: server did not report byte-range support. Multi-thread download may fail."
            );
        }

        progress = new DownloadProgress(fileSize);

        System.out.println("Starting download...");
        System.out.println("File: " + fileInfo.getFileName());
        System.out.println("Size: " + fileSize + " bytes");

        try (RandomAccessFile file = new RandomAccessFile(outputFile, "rw")) {
            file.setLength(fileSize);
        } catch (Exception e) {
            System.out.println("Could not create output file");
            e.printStackTrace();
            status = DownloadStatus.FAILED;
            return;
        }

        int activeThreads = (int) Math.min(numberOfThreads, fileSize);
        long chunkSize = Math.max(1, fileSize / activeThreads);
        executorService = Executors.newFixedThreadPool(activeThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < activeThreads; i++) {
            long startByte = i * chunkSize;
            long endByte;

            if (i == activeThreads - 1) {
                endByte = fileSize - 1;
            } else {
                endByte = startByte + chunkSize - 1;
            }

            DownloadTask task =
                    new DownloadTask(
                            fileURL,
                            outputFile,
                            startByte,
                            endByte,
                            i + 1,
                            maxRetries,
                            progress,
                            this
                    );

            futures.add(executorService.submit(task));
        }

        boolean success = waitForTasks(futures);
        executorService.shutdown();

        if (success) {
            status = DownloadStatus.COMPLETED;
            System.out.println("Download completed!");
        } else {
            status = DownloadStatus.FAILED;
            System.out.println("Download failed.");
        }
    }

    private boolean waitForTasks(List<Future<Boolean>> futures) {
        boolean allDone = false;

        while (!allDone) {
            allDone = true;

            for (Future<Boolean> future : futures) {
                if (!future.isDone()) {
                    allDone = false;
                    break;
                }
            }

            printProgress();

            if (!allDone) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        for (Future<Boolean> future : futures) {
            try {
                if (!future.get()) {
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void printProgress() {
        if (progress == null) {
            return;
        }

        System.out.println(
                String.format(
                        "Progress: %.2f%% | Speed: %s",
                        progress.getPercent(),
                        progress.getFormattedSpeed()
                )
        );
    }

    public void pause() {
        paused = true;
        status = DownloadStatus.PAUSED;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            status = DownloadStatus.DOWNLOADING;
            pauseLock.notifyAll();
        }
    }

    void waitIfPaused() throws InterruptedException {
        synchronized (pauseLock) {
            while (paused) {
                pauseLock.wait();
            }
        }
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public DownloadProgress getProgress() {
        return progress;
    }
}
