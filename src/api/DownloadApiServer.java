package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import downloader.DownloadManager;
import downloader.DownloadProgress;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DownloadApiServer {
    private static DownloadManager manager;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/download", DownloadApiServer::startDownload);
        server.createContext("/pause", DownloadApiServer::pauseDownload);
        server.createContext("/resume", DownloadApiServer::resumeDownload);
        server.createContext("/status", DownloadApiServer::getStatus);

        server.start();
        System.out.println("REST API running at http://localhost:8080");
    }

    private static void startDownload(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Use POST\"}");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String url = query.get("url");
        String output = query.getOrDefault("output", "downloads/api-download.bin");
        int threads = Integer.parseInt(query.getOrDefault("threads", "4"));

        if (url == null || url.isBlank()) {
            send(exchange, 400, "{\"error\":\"Missing url query parameter\"}");
            return;
        }

        manager = new DownloadManager(url, output, threads);
        Thread downloadThread = new Thread(manager::download, "download-api-worker");
        downloadThread.start();

        send(exchange, 202, "{\"message\":\"Download started\"}");
    }

    private static void pauseDownload(HttpExchange exchange) throws IOException {
        if (manager == null) {
            send(exchange, 404, "{\"error\":\"No active download\"}");
            return;
        }

        manager.pause();
        send(exchange, 200, "{\"message\":\"Download paused\"}");
    }

    private static void resumeDownload(HttpExchange exchange) throws IOException {
        if (manager == null) {
            send(exchange, 404, "{\"error\":\"No active download\"}");
            return;
        }

        manager.resume();
        send(exchange, 200, "{\"message\":\"Download resumed\"}");
    }

    private static void getStatus(HttpExchange exchange) throws IOException {
        if (manager == null) {
            send(exchange, 200, "{\"status\":\"READY\",\"progress\":0,\"speed\":\"0.00 MB/s\"}");
            return;
        }

        DownloadProgress progress = manager.getProgress();
        double percent = progress == null ? 0.0 : progress.getPercent();
        String speed = progress == null ? "0.00 MB/s" : progress.getFormattedSpeed();

        String response =
                String.format(
                        "{\"status\":\"%s\",\"progress\":%.2f,\"speed\":\"%s\"}",
                        manager.getStatus(),
                        percent,
                        speed
                );

        send(exchange, 200, response);
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new HashMap<>();
        String query = uri.getRawQuery();

        if (query == null || query.isBlank()) {
            return result;
        }

        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            result.put(key, value);
        }

        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange exchange, int statusCode, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
