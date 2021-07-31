package net.dbd.demode.ui.unpacker;

import net.dbd.demode.ui.common.NameValuePartialIntField;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class SimpleProgressPanel extends JPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private NameValuePartialIntField partialValueField;
    private JProgressBar progressBar;

    public SimpleProgressPanel() {
        setMaximumSize(new Dimension(400, 50));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        draw();
    }

    private void draw() {
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setMinimumSize(new Dimension(400, 20));
        progressBar.setPreferredSize(new Dimension(400, 20));

        partialValueField = new NameValuePartialIntField("files:");
        var progressValueRow = createAndDecorate(() -> partialValueField);
//        var mainLabel = new JLabel("files:");
//        var progressValueLabel = new JLabel("0");
////        label.setPreferredSize(new Dimension(20, 25));
////        label.setMinimumSize(new Dimension(20, 25));
//
//        var labelRow = new JPanel(new FlowLayout());
//        labelRow.add(mainLabel);
//        labelRow.add(progressValueLabel);

        add(progressBar);
        add(progressValueRow);
    }


    private <T extends JComponent> T createAndDecorate(Supplier<T> factory) {
        T component = factory.get();
        component.setBackground(BG_COLOR);
        component.setBorder(strutBorder(Color.BLACK));

        return component;
    }

    public void reset() {
        partialValueField.reset();
        progressBar.setValue(0);
    }

    public void updateTotalValue(int totalValue) {
        partialValueField.setTotalValue(totalValue);
    }

    public void incrementPartialValue() {
        partialValueField.incrementCurrentValue();
        refreshProgressBar();
    }

    private void refreshProgressBar() {
        int percentComplete = (int) (((double) partialValueField.getCurrentValue() / partialValueField.getTotalValue()) * 100);
        progressBar.setValue(percentComplete);
    }

}
