package net.dbd.demode.ui;

import lombok.SneakyThrows;
import net.dbd.demode.config.AppProperties;
import net.dbd.demode.ui.unpacker.UnpackerPanel;
import net.dbd.demode.util.event.EventListener;
import net.dbd.demode.util.event.EventSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Nicky Ramone
 */
public class MainWindow extends JFrame {

    public enum Event {
        CLOSED
    }

    private final HomePanel homePanel;
    private final UnpackerPanel unpackerPanel;
    private final EventSupport eventSupport;

    private JPanel mainPanel;
    private JPanel contentPanel;


    @SneakyThrows
    public MainWindow(AppProperties appProperties, HomePanel homePanel, UnpackerPanel unpackerPanel) {
        super(String.format("%s (v%s) - %s",
                appProperties.getAppName(),
                appProperties.getAppVersion(),
                appProperties.getAppSubtitle())
        );
        this.homePanel = homePanel;
        this.unpackerPanel = unpackerPanel;
        this.eventSupport = new EventSupport();

        setIconImage(ResourceFactory.getAppIcon());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(new Dimension(700, 500));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                eventSupport.fireEvent(Event.CLOSED);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        draw();
        setVisible(true);
    }

    private void draw() {
        contentPanel = homePanel;

        var buttonPanel = new MainButtonPanel();
        buttonPanel.registerListener(MainButtonPanel.EventType.OPTION_SELECTED, e -> handleOptionSelected((MainButtonPanel.Option) e.getValue()));

        mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }


    private void handleOptionSelected(MainButtonPanel.Option option) {
        if (option == MainButtonPanel.Option.HOME) {
            setContentPanel(homePanel);
        } else if (option == MainButtonPanel.Option.UNPACK) {
            setContentPanel(unpackerPanel);
        }
    }


    private void setContentPanel(JPanel panel) {
        mainPanel.remove(contentPanel);
        contentPanel = panel;
        mainPanel.add(contentPanel);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void close() {
        dispose();
    }

    public void registerListener(Event eventType, EventListener eventListener) {
        eventSupport.registerListener(eventType, eventListener);
    }


}
