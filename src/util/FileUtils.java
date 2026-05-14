package util;

import downloader.FileInfo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileUtils {

    public static FileInfo getFileInfo(String fileURL) {

        try {

            URL url = new URL(fileURL);

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("HEAD");
            connection.connect();

            long fileSize =
                    connection.getContentLengthLong();

            boolean rangeSupported =
                    "bytes".equalsIgnoreCase(
                            connection.getHeaderField("Accept-Ranges")
                    );

            String fileName =
                    fileURL.substring(
                            fileURL.lastIndexOf("/") + 1
                    );

            if (fileName.isBlank()) {
                fileName = "download.bin";
            }

            connection.disconnect();

            return new FileInfo(fileSize, fileName, rangeSupported);

        } catch (IOException e) {

            System.out.println(
                    "Error getting file info"
            );

            return null;
        }
    }
}
