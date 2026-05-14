package downloader;

import java.util.concurrent.atomic.AtomicLong;

public class DownloadProgress {
    private final long totalBytes;
    private final AtomicLong downloadedBytes = new AtomicLong();
    private final long startedAtMillis;

    public DownloadProgress(long totalBytes) {
        this.totalBytes = totalBytes;
        this.startedAtMillis = System.currentTimeMillis();
    }

    public void addBytes(long bytes) {
        downloadedBytes.addAndGet(bytes);
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public double getPercent() {
        if (totalBytes <= 0) {
            return 0.0;
        }

        return (downloadedBytes.get() * 100.0) / totalBytes;
    }

    public double getSpeedBytesPerSecond() {
        long elapsedMillis = Math.max(1, System.currentTimeMillis() - startedAtMillis);
        return downloadedBytes.get() / (elapsedMillis / 1000.0);
    }

    public String getFormattedSpeed() {
        double bytesPerSecond = getSpeedBytesPerSecond();
        double megabytesPerSecond = bytesPerSecond / (1024 * 1024);
        return String.format("%.2f MB/s", megabytesPerSecond);
    }
}
