package util;

import downloader.FileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileUtils {
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "MultiThreadedDownloader/1.0";

    public static FileInfo getFileInfo(String fileURL) {
        try {
            URL url = new URL(fileURL);
            FileInfo fileInfo = getFileInfoWithHead(url, fileURL);

            if (fileInfo != null && fileInfo.getFileSize() > 0) {
                return fileInfo;
            }

            return getFileInfoWithRangeRequest(url, fileURL);
        } catch (IOException e) {
            System.out.println("Error getting file info: " + e.getMessage());
            return null;
        }
    }

    private static FileInfo getFileInfoWithHead(URL url, String fileURL) throws IOException {
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("HEAD");

        try {
            connection.connect();
            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 400) {
                return buildFileInfo(fileURL, connection.getContentLengthLong(), connection);
            }

            System.out.println("HEAD request returned HTTP " + responseCode + ". Trying ranged GET.");
            return null;
        } finally {
            connection.disconnect();
        }
    }

    private static FileInfo getFileInfoWithRangeRequest(URL url, String fileURL) throws IOException {
        HttpURLConnection connection = openConnection(url);
        connection.setRequestProperty("Range", "bytes=0-0");

        try {
            connection.connect();
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_PARTIAL
                    && responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Ranged GET returned HTTP " + responseCode);
                return null;
            }

            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                try (InputStream inputStream = connection.getInputStream()) {
                    inputStream.read();
                }
            }

            long fileSize = connection.getContentLengthLong();
            String contentRange = connection.getHeaderField("Content-Range");

            if (contentRange != null) {
                int slashIndex = contentRange.lastIndexOf('/');

                if (slashIndex >= 0 && slashIndex < contentRange.length() - 1) {
                    try {
                        fileSize = Long.parseLong(contentRange.substring(slashIndex + 1));
                    } catch (NumberFormatException e) {
                        fileSize = -1;
                    }
                }
            }

            return buildFileInfo(fileURL, fileSize, connection);
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    private static FileInfo buildFileInfo(
            String fileURL,
            long fileSize,
            HttpURLConnection connection
    ) {
        boolean rangeSupported =
                "bytes".equalsIgnoreCase(connection.getHeaderField("Accept-Ranges"))
                        || connection.getHeaderField("Content-Range") != null;

        return new FileInfo(fileSize, getFileName(fileURL), rangeSupported);
    }

    private static String getFileName(String fileURL) {
        int slashIndex = fileURL.lastIndexOf("/");
        String fileName = slashIndex >= 0 ? fileURL.substring(slashIndex + 1) : fileURL;
        int queryIndex = fileName.indexOf("?");

        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }

        if (fileName.isBlank()) {
            return "download.bin";
        }

        return fileName;
    }
}
