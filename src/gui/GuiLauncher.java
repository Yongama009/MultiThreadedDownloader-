package gui;

import javafx.application.Application;

public class GuiLauncher {

    public static void main(String[] args) {
        JavaFxRuntime.configureNativeCache();

        if (isLinuxWithoutDisplay()) {
            printDisplayHelp();
            return;
        }

        try {
            Application.launch(DownloaderApp.class, args);
        } catch (UnsupportedOperationException e) {
            if (e.getMessage() != null && e.getMessage().contains("DISPLAY")) {
                printDisplayHelp();
                return;
            }

            throw e;
        }
    }

    private static boolean isLinuxWithoutDisplay() {
        String osName = System.getProperty("os.name", "").toLowerCase();

        if (!osName.contains("linux")) {
            return false;
        }

        return isBlank(System.getenv("DISPLAY"))
                && isBlank(System.getenv("WAYLAND_DISPLAY"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void printDisplayHelp() {
        System.out.println("JavaFX GUI cannot start because no graphical display is available.");
        System.out.println("Run mvn javafx:run from a desktop terminal, not a headless/sandboxed terminal.");
        System.out.println("CLI fallback: java -cp target/classes Main");
    }
}
