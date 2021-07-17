package net.dbd.demode.ui.unpacker;

import net.dbd.demode.pak.PakMetaReader;
import net.dbd.demode.service.DbdUnpacker;
import net.dbd.demode.service.UserSettings;
import net.dbd.demode.ui.ResourceFactory;
import net.dbd.demode.ui.common.LogPanel;
import net.dbd.demode.ui.common.UiHelper;
import net.dbd.demode.util.TimeFormatUtil;
import net.dbd.demode.util.event.Event;
import net.dbd.demode.util.event.EventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.Consumer;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class UnpackerPanel extends JPanel {

    private final UserSettings userSettings;
    private final PakMetaReader pakMetaReader;

    private JTextField dirInput;
    private JButton startButton;
    private JButton stopButton;
    private UnpackingProgressPanel progressPanel;
    private LogPanel logPanel;
    private UnpackingWorker unpackingWorker;


    public UnpackerPanel(UserSettings userSettings, PakMetaReader pakMetaReader) {
        this.userSettings = userSettings;
        this.pakMetaReader = pakMetaReader;

        setBorder(strutBorder(Color.BLUE));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(drawContentPanelTop());
        add(drawContentPanelBottom());

    }

    private JPanel drawContentPanelTop() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(strutBorder(Color.RED));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 100));
        panel.setMaximumSize(new Dimension(200, 100));
        panel.setMinimumSize(new Dimension(200, 100));

        JLabel label = new JLabel("Unpacker", JLabel.CENTER);
        UiHelper.changeFontSize(label, 16);
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private JPanel drawContentPanelBottom() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(strutBorder(Color.YELLOW));
        panel.add(createOutputFolderPanel());

        JPanel progressPanelContainer = new JPanel();

        progressPanel = new UnpackingProgressPanel();
        progressPanelContainer.add(progressPanel);
        progressPanelContainer.setBorder(strutBorder(Color.CYAN));
        progressPanelContainer.setMaximumSize(new Dimension(600, 200));
        panel.add(progressPanelContainer);

        logPanel = new LogPanel();
        panel.add(logPanel);

        startButton = new JButton();
        startButton.setText("Start");
        startButton.addActionListener(e -> {
            startUnpacking();
        });
        panel.add(startButton);

        stopButton = new JButton();
        stopButton.setText("Cancel");
        stopButton.addActionListener(e -> {
            stopUnpacking();
        });
        stopButton.setVisible(false);
        panel.add(stopButton);


        return panel;
    }


    private JPanel createOutputFolderPanel() {
        JPanel dbdFolderRow = new JPanel();
        dbdFolderRow.setLayout(new BoxLayout(dbdFolderRow, BoxLayout.X_AXIS));
        dbdFolderRow.setPreferredSize(new Dimension(700, 25));
        dbdFolderRow.setMaximumSize(new Dimension(700, 25));

        JLabel dbdFolderInputLabel = new JLabel();
        dbdFolderInputLabel.setText("Output folder:");
        dbdFolderRow.add(dbdFolderInputLabel);
        dbdFolderRow.add(Box.createRigidArea(new Dimension(5, 0)));

        dirInput = new JTextField();
        dirInput.setMaximumSize(new Dimension(400, 25));
        dirInput.setText(userSettings.getUnpackOutputDirectory());
        dbdFolderRow.add(dirInput);
        dbdFolderRow.add(Box.createRigidArea(new Dimension(5, 0)));

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
        dbdFolderRow.add(button);

        return dbdFolderRow;
    }


    public void startUnpacking() {
        startButton.setVisible(false);
        logPanel.clear();
        logPanel.log("Start unpacking...");

        DbdUnpacker unpacker;
        try {

            String dbdDir = userSettings.getDbdDirectory();
            String outputDir = dirInput.getText();

            unpacker = new DbdUnpacker(this.pakMetaReader, dbdDir);
            DbdUnpacker.PaksSelection paksSelection = unpacker.selectPaks(3, 26, 30); // TODO: unpacker.selectAllPaks();

            unpackingWorker = new UnpackingWorker(unpacker, paksSelection, outputDir);
            unpackingWorker.execute();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            startButton.setEnabled(true);
        }
    }


    private void stopUnpacking() {
        if (unpackingWorker != null) {
            unpackingWorker.abort();
        }
    }


    private class UnpackingWorker extends SwingWorker<Void, Void> {

        private final DbdUnpacker dbdUnpacker;
        private final DbdUnpacker.PaksSelection paksSelection;
        private final String outputPath;
        private DbdUnpacker.UnpackStatus unpackStatus;
        private int currentPakNum;

        public UnpackingWorker(DbdUnpacker dbdUnpacker, DbdUnpacker.PaksSelection paksSelection, String outputPath) {
            this.dbdUnpacker = dbdUnpacker;
            this.paksSelection = paksSelection;
            this.outputPath = outputPath;

            registerUiListener(DbdUnpacker.EventType.UNPACK_BEGIN, e -> handleUnpackBeginEvent((DbdUnpacker.UnpackStatus) e.getValue()));
            registerUiListener(DbdUnpacker.EventType.PAK_EXTRACT_BEGIN, e -> handlePakExtractBeginEvent());
            registerUiListener(DbdUnpacker.EventType.FILE_EXTRACTED, e -> handleFileExtractedEvent());
            registerUiListener(DbdUnpacker.EventType.BYTES_EXTRACTED, e -> handleBytesExtractedEvent());
            registerUiListener(DbdUnpacker.EventType.PAK_EXTRACT_FINISH, e -> handlePakExtractedEvent((Integer) e.getValue()));
            registerUiListener(DbdUnpacker.EventType.UNPACK_FINISH, e -> handleUnpackFinishEvent());
            registerUiListener(DbdUnpacker.EventType.ABORTED, e -> handleUnpackAbortEvent());
        }


        private void registerUiListener(DbdUnpacker.EventType eventType, Consumer<Event> f) {
            EventListener listener = event -> SwingUtilities.invokeLater(() -> f.accept(event));
            dbdUnpacker.registerListener(eventType, listener);
        }


        @Override
        protected Void doInBackground() throws Exception {
            dbdUnpacker.unpack(paksSelection, outputPath);
            stopButton.setVisible(false);
            startButton.setVisible(true);

            return null;
        }

        private void handleUnpackBeginEvent(DbdUnpacker.UnpackStatus unpackStatus) {
            this.unpackStatus = unpackStatus;
            stopButton.setVisible(true);
            progressPanel.startTotal(unpackStatus.getTotalStats(), unpackStatus.getTotalPaks());
        }

        private void handlePakExtractBeginEvent() {
            progressPanel.startPackage(unpackStatus.getCurrentPakStats(), ++currentPakNum);
        }

        private void handleFileExtractedEvent() {
            progressPanel.refreshFilesExtracted();
        }

        private void handleBytesExtractedEvent() {
            progressPanel.refreshBytesExtracted();
        }

        private void handlePakExtractedEvent(int elapsedSeconds) {
            progressPanel.stopPackageProgress();
            logPanel.log(String.format("Unpacked pak#%d in %s", currentPakNum, TimeFormatUtil.formatTimeUpToYears(elapsedSeconds)));
        }

        private void handleUnpackFinishEvent() {
            progressPanel.stopTotalProgress();
            logPanel.log("Finished unpacking everything in " + TimeFormatUtil.formatTimeUpToYears(unpackStatus.getTotalStats().elapsed()));
        }

        private void handleUnpackAbortEvent() {
            logPanel.log("Aborted.");
            progressPanel.stop();
            startButton.setVisible(true);
        }

        public void abort() {
            if (unpackStatus != null) {
                unpackStatus.abort();
            }
        }

    }


}
