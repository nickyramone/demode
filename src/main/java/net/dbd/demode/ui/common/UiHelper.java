package net.dbd.demode.ui.common;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author Nicky Ramone
 */
public class UiHelper {

    private static final boolean DEV_MODE = false;


    public static Border strutBorder(Color color) {
        return DEV_MODE ? BorderFactory.createLineBorder(color) : null;
    }

    public static void changeFontSize(JComponent component, int newFontSize) {
        var oldFont = component.getFont();
        component.setFont(new Font(oldFont.getName(), oldFont.getStyle(), newFontSize));
    }

}
