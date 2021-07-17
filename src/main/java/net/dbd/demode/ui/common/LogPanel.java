package net.dbd.demode.ui.common;

import javax.swing.*;
import java.awt.*;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class LogPanel extends JPanel {

    private final JTextArea console;

    public LogPanel() {
        console = new JTextArea(null, 10, 70);
        console.setBorder(strutBorder(Color.BLUE));

        var scrollPane = new JScrollPane(console);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane);
    }


    public void clear() {
        console.setText(null);
    }

    public void log(String message) {
        console.append(message + "\n");
    }

}
