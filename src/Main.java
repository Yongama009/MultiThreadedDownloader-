import downloader.DownloadManager;

public class Main {

    public static void main(String[] args) {

        String fileURL =
                "http://speedtest.tele2.net/100MB.zip";

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
