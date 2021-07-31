package net.dbd.demode.ui.mainpanel.unpacker;

import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.Factory;
import net.dbd.demode.config.AppProperties;
import net.dbd.demode.config.UserSettings;
import net.dbd.demode.service.*;
import net.dbd.demode.service.DbdUnpacker.EventType;
import net.dbd.demode.service.DbdUnpacker.UnpackMonitor;
import net.dbd.demode.ui.ResourceFactory;
import net.dbd.demode.ui.common.LogPanel;
import net.dbd.demode.ui.common.SimpleProgressPanel;
import net.dbd.demode.ui.common.UiHelper;
import net.dbd.demode.util.format.TimeFormatUtil;
import net.dbd.demode.util.lang.OperationAbortedException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import static javax.swing.SwingUtilities.invokeLater;
import static net.dbd.demode.ui.common.UiHelper.changeFontSize;
import static net.dbd.demode.ui.common.UiHelper.strutBorder;
import static net.dbd.demode.util.format.NumberFormatUtil.format;

/**
 * @author Nicky Ramone
 */
@Slf4j
public class UnpackerPanel extends JPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private final AppProperties appProperties;
    private final UserSettings userSettings;

    private JTextField dirInput;
    private JButton startButton;
    private JButton stopButton;
    private JCheckBox fullExtractionCheckbox;
    private SimpleProgressPanel simpleProgressPanel;
    private UnpackingProgressPanel progressPanel;
    private LogPanel logPanel;

    private UnpackMonitor unpackMonitor;


    public UnpackerPanel(AppProperties appProperties, UserSettings userSettings) {
        this.appProperties = appProperties;
        this.userSettings = userSettings;

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

        JLabel label = new JLabel("Unpacker", JLabel.CENTER);
        changeFontSize(label, 16);
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private JPanel drawContentPanelBottom() {
        simpleProgressPanel = new SimpleProgressPanel("files:");
        simpleProgressPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        simpleProgressPanel.setVisible(false);

        progressPanel = new UnpackingProgressPanel();
        progressPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        progressPanel.setVisible(false);

        logPanel = new LogPanel();
        logPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        startButton = UiHelper.createButton("Unpack Everything");
        startButton.addActionListener(e -> {
            startUnpacking();
        });

        stopButton = UiHelper.createButton("Cancel");
        stopButton.addActionListener(e -> {
            abortUnpacking();
        });
        stopButton.setVisible(false);

        fullExtractionCheckbox = new JCheckBox("Force full extraction");
        fullExtractionCheckbox.setToolTipText(String.format("%s is smart enough to extract only changed files. " +
                "Use this option to re-extract everything from scratch.", appProperties.getAppName()));

        var buttonPanel = new JPanel();
        buttonPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        buttonPanel.setBorder(strutBorder(Color.GREEN));
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(fullExtractionCheckbox);

        JPanel panel = new JPanel();
        panel.setBackground(BG_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panel.add(simpleProgressPanel);
        panel.add(progressPanel);
        panel.add(logPanel);
        panel.add(buttonPanel);

        return panel;
    }


    // TODO: remove
    private JPanel createOutputFolderPanel() {

        JLabel dbdFolderInputLabel = new JLabel();
        dbdFolderInputLabel.setText("Output folder:");

        dirInput = new JTextField();
        dirInput.setMaximumSize(new Dimension(400, 25));

        JLabel button = new JLabel();
        button.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.FILE_BROWSER));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                if (fileChooser.showOpenDialog(button) == JFileChooser.APPROVE_OPTION) {
                    dirInput.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        JPanel dbdFolderRow = new JPanel();
        dbdFolderRow.setBorder(strutBorder(Color.BLUE));
        dbdFolderRow.setBackground(BG_COLOR);
        dbdFolderRow.setLayout(new BoxLayout(dbdFolderRow, BoxLayout.X_AXIS));
        dbdFolderRow.setPreferredSize(new Dimension(700, 25));
        dbdFolderRow.setMaximumSize(new Dimension(700, 25));
        dbdFolderRow.add(dbdFolderInputLabel);
        dbdFolderRow.add(Box.createRigidArea(new Dimension(5, 0)));
        dbdFolderRow.add(dirInput);
        dbdFolderRow.add(Box.createRigidArea(new Dimension(5, 0)));
        dbdFolderRow.add(button);

        return dbdFolderRow;
    }


    public void startUnpacking() {
        startButton.setVisible(false);
        fullExtractionCheckbox.setVisible(false);
        logPanel.clear();

        try {
            Path dbdHomePath = Path.of(userSettings.getDbdHomePath());
            DbdPakManager dbdPakManager = Factory.newDbdPakManager(dbdHomePath);
            DbdUnpacker unpacker = Factory.newDbdUnpacker(dbdPakManager);

            if (fullExtractionCheckbox.isSelected()) {
                unpackMonitor = unpacker.unpackAll(dbdHomePath);
            } else {
                unpackMonitor = unpacker.unpackMissingAndUnverified(dbdHomePath);
            }

            unpackMonitor
                    .registerListener(EventType.FILE_SELECT__BEGIN, e -> invokeLater(() -> handleFileSelectBeginEvent((int) e.getValue())))
                    .registerListener(EventType.FILE_SELECT__FILE_PROCESSED, e -> invokeLater(this::handleSelectFileScannedEvent))
                    .registerListener(EventType.FILE_SELECT__FINISH, e -> invokeLater(() -> handleSelectFinishEvent((MultiPakSelection) e.getValue())))
                    .registerListener(EventType.UNPACK_BEGIN, e -> invokeLater(() -> handleUnpackBeginEvent(unpackMonitor)))
                    .registerListener(EventType.PAK_EXTRACT_BEGIN, e -> invokeLater(() -> handlePakExtractBeginEvent(unpackMonitor)))
                    .registerListener(EventType.FILE_EXTRACTED, e -> invokeLater(this::handleFileExtractedEvent))
                    .registerListener(EventType.BYTES_EXTRACTED, e -> invokeLater(this::handleBytesExtractedEvent))
                    .registerListener(EventType.PAK_EXTRACT_FINISH, e -> invokeLater(() -> handlePakExtractedEvent((int) e.getValue(), unpackMonitor)))
                    .registerListener(EventType.UNPACK_FINISH, e -> invokeLater(() -> handleUnpackFinishEvent(unpackMonitor)))
                    .registerListener(EventType.ABORTED, e -> invokeLater(this::handleUnpackAbortEvent));

            logPanel.log("Target path: " + dbdHomePath);
            unpackMonitor.start()
                    .exceptionally(throwable -> {
                        Throwable cause = throwable.getCause();

                        if (cause instanceof OperationAbortedException) {
                            invokeLater(this::handleUnpackAbortEvent);
                        } else if (cause instanceof InsufficientDiskSpaceException) {
                            logPanel.log(String.format("Insufficient disk space. Need at least %s free bytes. Cannot continue.",
                                    format(((InsufficientDiskSpaceException) cause).getRequiredSpaceInBytes())));
                        } else {
                            log.error("Failed to unpack.", cause);
                            logPanel.log("Encountered an error. Cannot continue.");
                        }
                        return null;
                    });

        } catch (InvalidDbdHomePathException e) {
            logPanel.log("Could not find DBD installed in the specified location. Cannot continue.");
        }
        finally {
            unpackingFinished();
        }
    }


    private void abortUnpacking() {
        if (unpackMonitor != null) {
            unpackMonitor.abort();
        }
    }

    private void unpackingFinished() {
        progressPanel.stop();
        progressPanel.hidePartialProgress();
        simpleProgressPanel.setVisible(false);
        startButton.setVisible(true);
        fullExtractionCheckbox.setVisible(true);
        stopButton.setVisible(false);
    }


    private void handleFileSelectBeginEvent(int totalFilesToScan) {
        logPanel.log("Checking which files are already unpacked...");
        startButton.setVisible(false);
        stopButton.setVisible(true);
        progressPanel.setVisible(false);
        simpleProgressPanel.reset();
        simpleProgressPanel.setVisible(true);
        simpleProgressPanel.updateTotalValue(totalFilesToScan);
    }

    private void handleSelectFileScannedEvent() {
        simpleProgressPanel.incrementPartialValue();
    }

    private void handleSelectFinishEvent(MultiPakSelection selection) {
        if (selection.getTotalFiles() == 0) {
            logPanel.log("All files seem to be already unpacked. Nothing to do.");
            unpackingFinished();
        } else {
            logPanel.log(String.format(
                    "Selected %s files from %d packages with a total of %s bytes.",
                    format(selection.getTotalFiles()), selection.getSinglePakSelections().size(), format(selection.getTotalBytes())));
            simpleProgressPanel.setVisible(false);
        }
    }

    private void handleUnpackBeginEvent(UnpackMonitor unpackMonitor) {
        logPanel.log("Started unpacking.");
        stopButton.setVisible(true);
        progressPanel.showPartialProgress();
        progressPanel.setVisible(true);
        progressPanel.startTotal(unpackMonitor.getTotalStats(), unpackMonitor.getTotalPaks());
    }

    private void handlePakExtractBeginEvent(UnpackMonitor unpackMonitor) {
        logPanel.log(String.format("Unpacking package #%d...", unpackMonitor.getCurrentPak()));
        progressPanel.startPackage(unpackMonitor.getCurrentPakStats(), unpackMonitor.getCurrentPak());
    }

    private void handleFileExtractedEvent() {
        progressPanel.refreshFilesExtracted();
    }

    private void handleBytesExtractedEvent() {
        progressPanel.refreshBytesExtracted();
    }

    private void handlePakExtractedEvent(int elapsedSeconds, UnpackMonitor monitor) {
        progressPanel.stopPackageProgress();
        logPanel.log(String.format("Unpacked package #%d in %s.", monitor.getCurrentPak(), TimeFormatUtil.formatTimeUpToYears(elapsedSeconds)));
    }

    private void handleUnpackFinishEvent(UnpackMonitor monitor) {
        progressPanel.stopTotalProgress();
        logPanel.log("Finished unpacking everything in " + TimeFormatUtil.formatTimeUpToYears(monitor.getTotalStats().elapsed()) + ".");
        unpackingFinished();
    }

    private void handleUnpackAbortEvent() {
        logPanel.log("Aborted.");
        unpackingFinished();
    }

}
