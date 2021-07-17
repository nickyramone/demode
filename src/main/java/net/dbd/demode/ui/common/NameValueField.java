package net.dbd.demode.ui.common;

import javax.swing.*;

/**
 * @author Nicky Ramone
 */
public class NameValueField extends JPanel {

    private final JLabel leftLabel;
    private final JLabel rightLabel;


    public NameValueField(String name) {
        this(name, null);
    }

    public NameValueField(String name, String value) {
        this.leftLabel = new JLabel(name);
        this.rightLabel = new JLabel(value);

        add(leftLabel);
        add(rightLabel);
    }

    public void setValue(int value) {
        rightLabel.setText(String.valueOf(value));
    }


}
