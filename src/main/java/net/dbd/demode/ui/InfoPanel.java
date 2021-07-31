package net.dbd.demode.ui;

import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.config.AppProperties;
import net.dbd.demode.ui.common.BackgroundPanel;

import javax.swing.*;
import java.awt.*;

import static net.dbd.demode.ui.common.UiHelper.changeFontSize;
import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
@Slf4j
public class InfoPanel extends BackgroundPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private final AppProperties appProperties;


    public InfoPanel(AppProperties appProperties) {
        super(ResourceFactory.getAppBackgroundLogo(), BackgroundPanel.ACTUAL);
        this.appProperties = appProperties;

        setBackground(BG_COLOR);
        setBorder(strutBorder(Color.BLUE));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(drawContentPanelTop());
        add(drawContentPanelBottom());
    }

    private JPanel drawContentPanelTop() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(strutBorder(Color.RED));
        panel.setBackground(BG_COLOR);
        panel.setPreferredSize(new Dimension(200, 100));
        panel.setMaximumSize(new Dimension(200, 100));
        panel.setMinimumSize(new Dimension(200, 100));

        JLabel label = new JLabel("About " + appProperties.getAppName(), JLabel.CENTER);
        changeFontSize(label, 20);
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }


    private JPanel drawContentPanelBottom() {

        var textArea = new JTextArea(null, 10, 70);
        textArea.setOpaque(false);
        textArea.setBorder(strutBorder(Color.BLUE));
        textArea.setFont(textArea.getFont().deriveFont(12f));
        textArea.setEditable(false);
        textArea.setText("Version: " + appProperties.getAppVersion() + "\n" + appProperties.getAppAboutInfo());

        var scrollPane = new JScrollPane(textArea);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        var panel = new JPanel();
        panel.setBackground(BG_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panel.add(scrollPane);

        return panel;
    }
}