package net.dbd.demode.ui.unpacker;

import net.dbd.demode.pak.ExtractionStats;
import net.dbd.demode.ui.common.NameValueElapsedField;
import net.dbd.demode.ui.common.NameValuePartialIntField;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class UnpackingProgressPanel extends JPanel {

    private static final int BYTES_PER_KBYTE = 1000;
    private static final int MILLIS_PER_SECOND = 1000;
    private static final Color BG_COLOR = Color.WHITE;

    private static final class ProgressPanelElements {
        private JLabel packagesField;
        private NameValuePartialIntField filesField;
        private NameValuePartialIntField sizeField;
        private NameValueElapsedField elapsedField;
        private NameValueElapsedField etaField;
        private JProgressBar progressBar;
        private Timer elapsedTimer;
        private ExtractionStats stats;
    }

    private ProgressPanelElements partialProgress;
    private ProgressPanelElements totalProgress;


    public UnpackingProgressPanel() {
        setBackground(BG_COLOR);
        setLayout(new GridBagLayout());
        setBorder(strutBorder(Color.MAGENTA));
        setMaximumSize(new Dimension(550, 200));

        partialProgress = createProgressPanelElements();
        totalProgress = createProgressPanelElements();

        drawSingleProgress("Current package:", 0, partialProgress);
        drawSingleProgress("Total packages:", 3, totalProgress);
    }


    private void drawSingleProgress(String title, int startRow, ProgressPanelElements elements) {

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        gbc.gridx = 0;
        gbc.gridy = startRow + 1;
        add(new JLabel(title), gbc);

        gbc.gridx = 1;
        add(elements.packagesField, gbc);

        gbc.gridx = 2;
        gbc.gridy = startRow;
        add(elements.filesField, gbc);

        gbc.gridx = 3;
        gbc.gridy = startRow;
        add(elements.sizeField, gbc);

        gbc.gridx = 4;
        gbc.gridy = startRow;
        add(elements.elapsedField, gbc);

        gbc.gridx = 5;
        gbc.gridy = startRow;
        add(elements.etaField, gbc);

        gbc.gridx = 2;
        gbc.gridy = startRow + 1;
        gbc.gridwidth = 4;
        add(elements.progressBar, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = startRow + 2;
        gbc.gridwidth = 6;
        add(Box.createRigidArea(new Dimension(1, 15)), gbc);
        gbc.gridwidth = 1;
    }

    private ProgressPanelElements createProgressPanelElements() {
        var progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setMinimumSize(new Dimension(400, 20));
        progressBar.setPreferredSize(new Dimension(400, 20));

        var packagesLabel = new JLabel("0");
        packagesLabel.setPreferredSize(new Dimension(20, 25));
        packagesLabel.setMinimumSize(new Dimension(20, 25));

        var elements = new ProgressPanelElements();
        elements.packagesField = createAndDecorate(() -> packagesLabel);
        elements.filesField = createAndDecorate(() -> new NameValuePartialIntField("files:"));
        elements.sizeField = createAndDecorate(() -> new NameValuePartialIntField("size [kB]:"));
        elements.elapsedField = createAndDecorate(() -> new NameValueElapsedField("elapsed:"));
        elements.etaField = createAndDecorate(() -> new NameValueElapsedField("ETA:"));
        elements.progressBar = progressBar;
        elements.elapsedTimer = new Timer(MILLIS_PER_SECOND, e -> refreshElapsed(elements));

        return elements;
    }


    private <T extends JComponent> T createAndDecorate(Supplier<T> factory) {
        T component = factory.get();
        component.setBackground(BG_COLOR);
        component.setBorder(strutBorder(Color.BLACK));

        return component;
    }


    public void startTotal(ExtractionStats stats, int totalPaks) {
        initProgressElements(totalProgress, stats, totalPaks);
    }

    public void startPackage(ExtractionStats stats, int packageNum) {
        initProgressElements(partialProgress, stats, packageNum);
    }

    private void initProgressElements(ProgressPanelElements elements, ExtractionStats stats, int packageNum) {
        elements.stats = stats;
        elements.packagesField.setText(String.valueOf(packageNum));
        elements.elapsedTimer.start();
        elements.filesField.setTotalValue(stats.getFilesToExtract());
        elements.filesField.setCurrentValue(0);
        elements.sizeField.setTotalValue(bytesToKilobytes(stats.getBytesToExtract()));
        elements.sizeField.setCurrentValue(0);
        elements.progressBar.setValue(0);
    }


    public void stopPackageProgress() {
        partialProgress.elapsedTimer.stop();
    }

    public void stopTotalProgress() {
        totalProgress.elapsedTimer.stop();
    }

    public void stop() {
        stopPackageProgress();
        stopTotalProgress();
    }

    private void refreshElapsed(ProgressPanelElements progressPanel) {
        int elapsed = progressPanel.stats.elapsed();
        progressPanel.elapsedField.updateTime(elapsed);

        if (elapsed % 5 == 0) {
            progressPanel.etaField.updateTime(progressPanel.stats.eta());
        }
    }

    public void refreshFilesExtracted() {
        partialProgress.filesField.setCurrentValue(partialProgress.stats.getFilesExtracted());
        totalProgress.filesField.setCurrentValue(totalProgress.stats.getFilesExtracted());
    }

    public void refreshBytesExtracted() {
        partialProgress.sizeField.setCurrentValue(bytesToKilobytes(partialProgress.stats.getBytesExtracted()));
        totalProgress.sizeField.setCurrentValue(bytesToKilobytes(totalProgress.stats.getBytesExtracted()));
        refreshProgressBars();
    }

    private void refreshProgressBars() {
        refreshProgressBar(partialProgress.progressBar, partialProgress.stats);
        refreshProgressBar(totalProgress.progressBar, totalProgress.stats);
    }

    private void refreshProgressBar(JProgressBar progressBar, ExtractionStats stats) {
        int percentComplete = (int) (((double) stats.getBytesExtracted() / stats.getBytesToExtract()) * 100);

        if (percentComplete != progressBar.getValue()) {
            progressBar.setValue(percentComplete);
        }
    }

    public void showPartialProgress() {
        setPartialProgressVisible(true);
    }

    public void hidePartialProgress() {
        setPartialProgressVisible(false);
    }

    private void setPartialProgressVisible(boolean visible) {
        Component[] components = getComponents();
        for (int i = 0; i < 8; i++) {
            components[i].setVisible(visible);
        }
    }

    private long bytesToKilobytes(long numBytes) {
        return (long) Math.ceil((double) numBytes / BYTES_PER_KBYTE);
    }

}
