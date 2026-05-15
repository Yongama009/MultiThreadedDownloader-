package downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<Boolean> {
    private final String fileURL;
    private final String outputFile;
    private final long startByte;
    private final long endByte;
    private final int threadNumber;
    private final int maxRetries;
    private final DownloadProgress progress;
    private final DownloadManager pauseController;

    public DownloadTask(
            String fileURL,
            String outputFile,
            long startByte,
            long endByte,
            int threadNumber,
            int maxRetries,
            DownloadProgress progress,
            DownloadManager pauseController
    ) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.startByte = startByte;
        this.endByte = endByte;
        this.threadNumber = threadNumber;
        this.maxRetries = maxRetries;
        this.progress = progress;
        this.pauseController = pauseController;
    }

    @Override
    public Boolean call() {
        int attempt = 0;
        long currentByte = startByte;

        while (attempt <= maxRetries && currentByte <= endByte) {
            attempt++;

            try {
                currentByte = downloadRange(currentByte);
                return true;
            } catch (Exception e) {
                System.out.println(
                        "Thread "
                                + threadNumber
                                + " failed attempt "
                                + attempt
                                + " of "
                                + (maxRetries + 1)
                );

                if (attempt > maxRetries) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return currentByte > endByte;
    }

    private long downloadRange(long currentByte) throws IOException, InterruptedException {
        System.out.println(
                "Thread "
                        + threadNumber
                        + " downloading bytes "
                        + currentByte
                        + " - "
                        + endByte
        );

        URL url = new URL(fileURL);
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestProperty(
                "Range",
                "bytes=" + currentByte + "-" + endByte
        );

        try (
                InputStream inputStream = connection.getInputStream();
                RandomAccessFile file = new RandomAccessFile(outputFile, "rw")
        ) {
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_PARTIAL
                    && (currentByte != 0 || responseCode != HttpURLConnection.HTTP_OK)) {
                throw new IOException("Unexpected HTTP response " + responseCode);
            }

            file.seek(currentByte);

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                pauseController.waitIfPaused();
                file.write(buffer, 0, bytesRead);
                currentByte += bytesRead;
                progress.addBytes(bytesRead);
            }
        } finally {
            connection.disconnect();
        }

        System.out.println(
                "Thread "
                        + threadNumber
                        + " finished."
        );

        return currentByte;
    }
}
