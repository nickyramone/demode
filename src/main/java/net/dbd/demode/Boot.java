package net.dbd.demode;

import net.dbd.demode.ui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * @author Nicky Ramone
 */
public class Boot {

    private static Logger log;
    private static MainWindow ui;

    public static void main(String[] args) {
        try {
            configureLogger();
            init();
        } catch (Exception e) {
            log.error("Failed to initialize application: {}", e.getMessage(), e);
            fatalErrorDialog("Failed to initialize application: " + e.getMessage());
            exitApplication(1);
        }
    }

    private static void configureLogger() throws URISyntaxException {
        URI execUri = Boot.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path appHome = new File(execUri).toPath().getParent();
        System.setProperty("app.home", appHome.toString());
        log = LoggerFactory.getLogger(Boot.class);
    }

    private static void init() throws Exception {
        log.info("Initializing...");
        initUi();
    }

    private static void initUi() {
        log.info("Starting UI...");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UIManager.put("ProgressBar.foreground", new Color(0x6f, 0xcc, 0x9f));
                UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
            } catch (Exception e) {
                log.error("Failed to initialize UI.", e);
                exitApplication(1);
                System.out.println("error!");
            }

            ui = Factory.mainWindow();
            ui.registerListener(MainWindow.Event.CLOSED, e -> exitApplication(0));
            log.info(Factory.appProperties().getAppName() + " is ready.");
        });
    }

    private static void exitApplication(int status) {
        log.info("Terminated UI.");
        System.exit(status);
    }

    private static void fatalErrorDialog(String msg) {
        msg += "\nExiting application.";
        JOptionPane.showMessageDialog(null, msg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

}
