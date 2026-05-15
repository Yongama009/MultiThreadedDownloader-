package gui;

import java.nio.file.Path;

final class JavaFxRuntime {
    private JavaFxRuntime() {
    }

    static void configureNativeCache() {
        if (System.getProperty("javafx.cachedir") == null) {
            Path cachePath = Path.of("target", "openjfx-cache").toAbsolutePath();
            System.setProperty("javafx.cachedir", cachePath.toString());
        }
    }
}
