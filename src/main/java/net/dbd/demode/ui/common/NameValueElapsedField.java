package net.dbd.demode.ui.common;

import net.dbd.demode.util.format.TimeFormatUtil;

import javax.swing.*;

/**
 * @author Nicky Ramone
 */
public class NameValueElapsedField extends JPanel {

    private final JLabel leftLabel;
    private final JLabel rightLabel;


    public NameValueElapsedField(String name) {
        this.leftLabel = new JLabel(name);
        this.rightLabel = new JLabel();
        updateTime(0);

        add(leftLabel);
        add(rightLabel);
    }


    public void updateTime(int seconds) {
        this.rightLabel.setText(TimeFormatUtil.formatTimeUpToHours(seconds));
    }

}
