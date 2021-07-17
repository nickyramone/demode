package net.dbd.demode.ui.common;

import lombok.Getter;

import javax.swing.*;
import java.text.NumberFormat;

/**
 * @author Nicky Ramone
 */
public class NameValuePartialIntField extends JPanel {

    private final NumberFormat numberFormat = NumberFormat.getInstance();
    private final JLabel nameLabel;
    private final JLabel currentValueLabel;
    private final JLabel totalValueLabel;

    @Getter
    private long totalValue;
    @Getter
    private long currentValue;


    public NameValuePartialIntField(String name) {
        this.nameLabel = new JLabel(name);
        this.currentValueLabel = new JLabel();
        this.totalValueLabel = new JLabel();
        this.totalValue = totalValue;

        refreshCurrentValue();
        refreshTotalValue();

        add(this.nameLabel);
        add(this.currentValueLabel);
        add(new JLabel("/"));
        add(this.totalValueLabel);
    }

    public void incrementCurrentValue() {
        incrementCurrentValue(1);
    }

    public void incrementCurrentValue(long added) {
        this.currentValue += added;
        refreshCurrentValue();
    }

    public void setCurrentValue(long value) {
        this.currentValue = value;
        refreshCurrentValue();
    }

    public void setTotalValue(long value) {
        this.totalValue = value;
        refreshTotalValue();
    }


    private void refreshCurrentValue() {
        currentValueLabel.setText(numberFormat.format(currentValue));
    }

    private void refreshTotalValue() {
        totalValueLabel.setText(numberFormat.format(totalValue));
    }

}
