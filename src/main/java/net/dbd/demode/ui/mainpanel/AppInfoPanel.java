package net.dbd.demode.ui.mainpanel;

import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.config.AppProperties;
import net.dbd.demode.ui.ResourceFactory;
import net.dbd.demode.ui.common.BackgroundPanel;

import javax.swing.*;
import java.awt.*;

import static net.dbd.demode.ui.common.UiHelper.changeFontSize;
import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
@Slf4j
public class AppInfoPanel extends JPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private final AppProperties appProperties;


    public AppInfoPanel(AppProperties appProperties) {
        this.appProperties = appProperties;

        setBackground(BG_COLOR);
        setBorder(strutBorder(Color.BLUE));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(drawContentPanelTop());
        add(drawContentPanelBottom());
    }

    private JPanel drawContentPanelTop() {
        String title = String.format("<html>About <b><i>%s</i></b></html", appProperties.getAppName());
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        changeFontSize(label, 20);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(strutBorder(Color.RED));
        panel.setPreferredSize(new Dimension(0, 100));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }


    private JPanel drawContentPanelBottom() {
        var textArea = new JTextArea(null, 10, 70);
        textArea.setOpaque(false);
        textArea.setBorder(strutBorder(Color.BLUE));
        textArea.setFont(textArea.getFont().deriveFont(Font.BOLD, 12f));
        textArea.setForeground(new Color(80, 168, 50));
        textArea.setEditable(false);
        textArea.setText(appProperties.getAppAboutInfo());

        var scrollPane = new JScrollPane(textArea);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        var panel = new BackgroundPanel(ResourceFactory.getAppBackgroundLogo(), BackgroundPanel.ACTUAL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panel.setBackground(BG_COLOR);
        panel.add(scrollPane);

        return panel;
    }
}