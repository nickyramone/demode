package net.dbd.demode.ui.unpacker;

import lombok.Builder;
import net.dbd.demode.pak.ExtractionStats;
import net.dbd.demode.ui.common.NameValueElapsedField;
import net.dbd.demode.ui.common.NameValueField;
import net.dbd.demode.ui.common.NameValuePartialIntField;

import javax.swing.*;
import java.awt.*;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class UnpackingProgressPanel extends JPanel {

    private static final int BYTES_PER_KBYTE = 1000;
    private static final int MILLIS_PER_SECOND = 1000;

    @Builder
    private static final class ProgressPanelElements {

        private JProgressBar progressBar;
        private NameValuePartialIntField filesField;
        private NameValuePartialIntField sizeField;
        private NameValueElapsedField elapsedField;
        private NameValueElapsedField etaField;
        private Timer elapsedTimer;
        private ExtractionStats stats;
    }

    private ProgressPanelElements partialProgress;
    private ProgressPanelElements totalProgress;
    private NameValueField totalPakPanelTitle;
    private NameValueField currentPakPanelTitle;


    public UnpackingProgressPanel() {

        setLayout(new GridBagLayout());
        setBorder(strutBorder(Color.MAGENTA));

        GridBagConstraints gbc = new GridBagConstraints();
        drawPartialProgressPanel(gbc);
        drawTotalProgressPanel(gbc);
    }


    private void drawPartialProgressPanel(GridBagConstraints gbc) {
        currentPakPanelTitle = new NameValueField("Current package:");

        var progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setMinimumSize(new Dimension(300, 20));
        progressBar.setPreferredSize(new Dimension(300, 20));

        partialProgress = ProgressPanelElements.builder()
                .filesField(new NameValuePartialIntField("files:"))
                .sizeField(new NameValuePartialIntField("size [kB]:"))
                .elapsedField(new NameValueElapsedField("elapsed:"))
                .etaField(new NameValueElapsedField("ETA:"))
                .progressBar(progressBar)
                .elapsedTimer(new Timer(MILLIS_PER_SECOND, e -> refreshElapsed(partialProgress)))
                .build();

        gbc.gridx = 1;
        gbc.gridy = 0;
        add(partialProgress.filesField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        add(partialProgress.sizeField, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        add(partialProgress.elapsedField, gbc);

        gbc.gridx = 4;
        gbc.gridy = 0;
        add(partialProgress.etaField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(currentPakPanelTitle, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        add(partialProgress.progressBar, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 5;
        add(Box.createRigidArea(new Dimension(1, 15)), gbc);
        gbc.gridwidth = 1;
    }

    private void drawTotalProgressPanel(GridBagConstraints gbc) {

        totalPakPanelTitle = new NameValueField("Total packages:");

        var progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setMinimumSize(new Dimension(300, 20));
        progressBar.setPreferredSize(new Dimension(300, 20));

        totalProgress = ProgressPanelElements.builder()
                .filesField(new NameValuePartialIntField("files:"))
                .sizeField(new NameValuePartialIntField("size [kB]:"))
                .elapsedField(new NameValueElapsedField("elapsed:"))
                .etaField(new NameValueElapsedField("ETA:"))
                .progressBar(progressBar)
                .elapsedTimer(new Timer(MILLIS_PER_SECOND, e -> refreshElapsed(totalProgress)))
                .build();

        gbc.gridx = 1;
        gbc.gridy = 3;
        add(totalProgress.filesField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        add(totalProgress.sizeField, gbc);

        gbc.gridx = 3;
        gbc.gridy = 3;
        add(totalProgress.elapsedField, gbc);

        gbc.gridx = 4;
        gbc.gridy = 3;
        add(totalProgress.etaField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        add(totalPakPanelTitle, gbc);


        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 4;
        add(totalProgress.progressBar, gbc);
    }

    public void startTotal(ExtractionStats stats, int totalPaks) {
        totalProgress.stats = stats;
        totalProgress.elapsedTimer.start();
        totalProgress.filesField.setTotalValue(stats.getFilesToExtract());
        totalProgress.filesField.setCurrentValue(0);
        totalProgress.sizeField.setTotalValue(bytesToKilobytes(stats.getBytesToExtract()));
        totalProgress.sizeField.setCurrentValue(0);
        totalProgress.progressBar.setValue(0);

        totalPakPanelTitle.setValue(totalPaks);
    }

    public void startPackage(ExtractionStats stats, int packageNum) {
        partialProgress.stats = stats;
        partialProgress.elapsedTimer.start();
        partialProgress.filesField.setTotalValue(stats.getFilesToExtract());
        partialProgress.filesField.setCurrentValue(0);
        partialProgress.sizeField.setTotalValue(bytesToKilobytes(stats.getBytesToExtract()));
        partialProgress.sizeField.setCurrentValue(0);
        partialProgress.progressBar.setValue(0);

        currentPakPanelTitle.setValue(packageNum);
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

    private long bytesToKilobytes(long numBytes) {
        return (long) Math.ceil((double) numBytes / BYTES_PER_KBYTE);
    }

}
