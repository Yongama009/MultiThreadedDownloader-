# MultiThreadedDownloader

Java multi-threaded file downloader with CLI, JavaFX GUI, and REST API entry
points.

## Features

- Download progress percentage
- Download speed meter, for example `5.00 MB/s`
- Pause and resume support
- Retry failed download threads
- `ExecutorService` thread pool
- JavaFX GUI
- REST API version

## Project Structure

```text
MultiThreadedDownloader/
├── src/
│   ├── Main.java
│   ├── api/
│   │   └── DownloadApiServer.java
│   ├── downloader/
│   │   ├── DownloadManager.java
│   │   ├── DownloadTask.java
│   │   ├── DownloadProgress.java
│   │   ├── DownloadStatus.java
│   │   └── FileInfo.java
│   ├── gui/
│   │   └── DownloaderApp.java
│   └── util/
│       └── FileUtils.java
├── downloads/
├── pom.xml
├── README.md
└── .gitignore
```

Downloaded files should be saved in the `downloads/` directory.

## Where To Update Features

- Progress and speed: `src/downloader/DownloadProgress.java`
- Thread pool and chunk planning: `src/downloader/DownloadManager.java`
- Pause, resume, and retry behavior: `src/downloader/DownloadTask.java`
- File size and HTTP metadata: `src/util/FileUtils.java`
- CLI version: `src/Main.java`
- GUI version: `src/gui/DownloaderApp.java`
- REST API version: `src/api/DownloadApiServer.java`

## Run

Compile everything with Maven:

```bash
mvn -DskipTests compile
```

Run the CLI version:

```bash
java -cp target/classes Main
```

Run the JavaFX GUI:

```bash
mvn javafx:run
```

Run the REST API:

```bash
java -cp target/classes api.DownloadApiServer
```

Start a REST download:

```bash
curl -X POST "http://localhost:8080/download?url=https%3A%2F%2Fspeed.hetzner.de%2F100MB.bin&output=downloads/api-download.bin&threads=4"
```

Check status:

```bash
curl "http://localhost:8080/status"
```

Pause and resume:

```bash
curl -X POST "http://localhost:8080/pause"
curl -X POST "http://localhost:8080/resume"
```
