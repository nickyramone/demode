package net.dbd.demode.ui.common;

import javax.swing.*;
import java.awt.*;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class LogPanel extends JPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private JTextArea console;

    public LogPanel() {
        super(new GridBagLayout());
        setBackground(BG_COLOR);
        draw();
    }

    private void draw() {
        console = new JTextArea(null, 10, 70);
        console.setBorder(strutBorder(Color.BLUE));
        console.setFont(console.getFont().deriveFont(12f));
        console.setEditable(false);

        var scrollPane = new JScrollPane(console);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        add(scrollPane, gbc);
    }


    public void clear() {
        console.setText(null);
    }

    public void log(String message) {
        console.append(message + "\n");
    }

}
