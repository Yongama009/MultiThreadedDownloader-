package downloader;

public class FileInfo {

    private final long fileSize;
    private final String fileName;
    private final boolean rangeSupported;

    public FileInfo(long fileSize, String fileName, boolean rangeSupported) {
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.rangeSupported = rangeSupported;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isRangeSupported() {
        return rangeSupported;
    }
}
