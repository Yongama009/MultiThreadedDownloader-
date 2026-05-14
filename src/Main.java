import downloader.DownloadManager;

public class Main {

    public static void main(String[] args) {

        String fileURL =
                "https://speed.hetzner.de/100MB.bin";

        String outputFile =
                "downloads/test.bin";

        int numberOfThreads = 4;

        DownloadManager manager =
                new DownloadManager(
                        fileURL,
                        outputFile,
                        numberOfThreads
                );

        manager.download();
    }
}
