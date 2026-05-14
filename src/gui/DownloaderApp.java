package gui;

import downloader.DownloadManager;
import downloader.DownloadProgress;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DownloaderApp extends Application {
    private DownloadManager manager;
    private final TextField urlField = new TextField("https://speed.hetzner.de/100MB.bin");
    private final TextField outputField = new TextField("downloads/gui-download.bin");
    private final Spinner<Integer> threadSpinner = new Spinner<>(1, 32, 4);
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("READY");
    private final Label speedLabel = new Label("0.00 MB/s");

    @Override
    public void start(Stage stage) {
        Button startButton = new Button("Start");
        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Resume");

        startButton.setOnAction(event -> startDownload());
        pauseButton.setOnAction(event -> {
            if (manager != null) {
                manager.pause();
            }
        });
        resumeButton.setOnAction(event -> {
            if (manager != null) {
                manager.resume();
            }
        });

        GridPane root = new GridPane();
        root.setPadding(new Insets(16));
        root.setHgap(10);
        root.setVgap(10);

        root.add(new Label("URL"), 0, 0);
        root.add(urlField, 1, 0, 3, 1);
        root.add(new Label("Output"), 0, 1);
        root.add(outputField, 1, 1, 3, 1);
        root.add(new Label("Threads"), 0, 2);
        root.add(threadSpinner, 1, 2);
        root.add(startButton, 0, 3);
        root.add(pauseButton, 1, 3);
        root.add(resumeButton, 2, 3);
        root.add(progressBar, 0, 4, 4, 1);
        root.add(statusLabel, 0, 5);
        root.add(speedLabel, 1, 5);

        Timeline timeline =
                new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshStatus()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        stage.setScene(new Scene(root, 640, 240));
        stage.setTitle("MultiThreadedDownloader");
        stage.show();
    }

    private void startDownload() {
        manager =
                new DownloadManager(
                        urlField.getText(),
                        outputField.getText(),
                        threadSpinner.getValue()
                );

        Thread thread =
                new Thread(
                        () -> {
                            manager.download();
                            Platform.runLater(this::refreshStatus);
                        },
                        "download-gui-worker"
                );

        thread.start();
    }

    private void refreshStatus() {
        if (manager == null) {
            return;
        }

        statusLabel.setText(manager.getStatus().name());
        DownloadProgress progress = manager.getProgress();

        if (progress == null) {
            return;
        }

        progressBar.setProgress(progress.getPercent() / 100.0);
        speedLabel.setText(progress.getFormattedSpeed());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
