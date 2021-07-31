package net.dbd.demode.ui;

import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.config.UserSettings;
import net.dbd.demode.service.DbdPathService;
import net.dbd.demode.service.FileCleaner;
import net.dbd.demode.service.FileCleaner.CleanerMonitor;
import net.dbd.demode.ui.common.LogPanel;
import net.dbd.demode.ui.common.NameValueField;
import net.dbd.demode.ui.common.UiHelper;
import net.dbd.demode.util.lang.OperationAbortedException;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static javax.swing.SwingUtilities.invokeLater;
import static net.dbd.demode.ui.common.UiHelper.changeFontSize;
import static net.dbd.demode.ui.common.UiHelper.strutBorder;
import static net.dbd.demode.util.Constants.KB;
import static net.dbd.demode.util.format.NumberFormatUtil.format;

/**
 * @author Nicky Ramone
 */
@Slf4j
public class CleanerPanel extends JPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private final UserSettings userSettings;
    private final DbdPathService dbdPathService;
    private final FileCleaner fileCleaner;

    private NameValueField filesScannedField;
    private NameValueField filesDeletedField;
    private NameValueField bytesFreedField;
    private JButton startButton;
    private JButton stopButton;
    private LogPanel logPanel;

    private CleanerMonitor monitor;


    public CleanerPanel(UserSettings userSettings, DbdPathService dbdPathService, FileCleaner fileCleaner) {
        this.userSettings = userSettings;
        this.dbdPathService = dbdPathService;
        this.fileCleaner = fileCleaner;

        setBackground(BG_COLOR);
        setBorder(strutBorder(Color.BLUE));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(drawContentPanelTop());
        add(drawContentPanelBottom());
    }

    private JPanel drawContentPanelTop() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(strutBorder(Color.RED));
        panel.setBackground(BG_COLOR);
        panel.setPreferredSize(new Dimension(200, 100));
        panel.setMaximumSize(new Dimension(200, 100));
        panel.setMinimumSize(new Dimension(200, 100));

        JLabel label = new JLabel("Cleaner", JLabel.CENTER);
        changeFontSize(label, 16);
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private JPanel drawContentPanelBottom() {

        filesScannedField = new NameValueField("files scanned:", "0");
        filesScannedField.setBackground(BG_COLOR);

        filesDeletedField = new NameValueField("files deleted:", "0");
        filesDeletedField.setBackground(BG_COLOR);

        bytesFreedField = new NameValueField("kB freed:", "0");
        bytesFreedField.setBackground(BG_COLOR);

        var fileScanPanel = new JPanel();
        fileScanPanel.setBackground(BG_COLOR);
        fileScanPanel.setLayout(new BoxLayout(fileScanPanel, BoxLayout.X_AXIS));
        fileScanPanel.add(filesScannedField);
        fileScanPanel.add(filesDeletedField);
        fileScanPanel.add(bytesFreedField);

        logPanel = new LogPanel();
        logPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        startButton = UiHelper.createButton("Clean Files");
        startButton.addActionListener(e -> {
            cleanFiles();
        });

        stopButton = UiHelper.createButton("Cancel");
        stopButton.addActionListener(e -> {
            cancelCleaning();
        });
        stopButton.setVisible(false);


        var buttonPanel = new JPanel();
        buttonPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        buttonPanel.setBorder(strutBorder(Color.GREEN));
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        JPanel panel = new JPanel();
        panel.setBackground(BG_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panel.add(fileScanPanel);
        panel.add(logPanel);
        panel.add(buttonPanel);

        return panel;
    }

    private void cleanFiles() {
        startButton.setVisible(false);
        stopButton.setVisible(true);
        logPanel.clear();
        String dbdHome = userSettings.getDbdHomePath();

        if (!dbdPathService.isValidDbdHomePath(Path.of(dbdHome))) {
            logPanel.log("Invalid DBD home path. Cannot continue.");
            finishCleaning();
            return;
        }

        monitor = fileCleaner.clean(Path.of(dbdHome));
        monitor.registerListener(FileCleaner.EventType.FILE_SCANNED, e -> invokeLater(this::handleFileScannedEvent));

        logPanel.log("Started cleaning.");
        monitor.start()
                .thenRun(() -> {
                    logPanel.log("Cleaning finished.");
                    finishCleaning();
                })
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause();

                    if (cause instanceof OperationAbortedException) {
                        log.error("Aborted");
                    }
                    else {
                        log.error("Unexpected error occurred while cleaning files.", cause);
                        logPanel.log("Unexpected error occurred. Cannot continue.");
                    }
                    finishCleaning();
                    return null;
                });
    }

    private void handleFileScannedEvent() {
        filesScannedField.setValue(format(monitor.getTotalFiles()));
        filesDeletedField.setValue(format(monitor.getFilesDeleted()));
        bytesFreedField.setValue(format(monitor.getBytesFreed() / KB));
    }

    private void finishCleaning() {
        startButton.setVisible(true);
        stopButton.setVisible(false);
    }

    private void cancelCleaning() {
        if (monitor != null) {
            monitor.abort();
        }
    }


}
