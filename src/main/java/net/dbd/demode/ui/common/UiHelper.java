package net.dbd.demode.ui.common;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Nicky Ramone
 */
public class UiHelper {

    private static final boolean DEV_MODE = false;
    private static final Color BUTTON_DEFAULT_BG_COLOR = new Color(0x42, 0x83, 0xDE);
    private static final Color BUTTON_HIGHLIGHT_BG_COLOR = new Color(0x58, 0x92, 0xE2);


    public static Border strutBorder(Color color) {
        return DEV_MODE ? BorderFactory.createLineBorder(color) : null;
    }

    public static void changeFontSize(JComponent component, int newFontSize) {
        var oldFont = component.getFont();
        component.setFont(oldFont.deriveFont((float) newFontSize));
    }

    public static JButton createButton(String text) {
        var button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(BUTTON_DEFAULT_BG_COLOR);
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(120, 30));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_HIGHLIGHT_BG_COLOR);
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_DEFAULT_BG_COLOR);
                super.mouseExited(e);
            }
        });

        return button;
    }

}
