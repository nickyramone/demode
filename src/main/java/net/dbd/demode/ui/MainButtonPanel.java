package net.dbd.demode.ui;

import net.dbd.demode.ui.common.SwingEventSupport;
import net.dbd.demode.util.event.EventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class MainButtonPanel extends JPanel {

    public enum EventType {
        OPTION_SELECTED
    }

    public enum Option {
        HOME, UNPACK, CLEAN, INFO
    }

    private static final Color BUTTON_BAR_BG_COLOR = new Color(105, 105, 105);
    private static final Color BUTTON_BAR_HOVER_BG_COLOR = new Color(93, 93, 93);
    private static final Color BUTTON_BAR_SELECTED_BG_COLOR = new Color(0x42, 0x83, 0xDE);
    private static final Color SEPARATOR_COLOR = new Color(0x72, 0x72, 0x72);

    private final SwingEventSupport eventSupport = new SwingEventSupport();


    class MainButton extends JPanel {

        JPanel innerPanel;

        MainButton(ResourceFactory.Icon icon, String title) {
            JLabel button = new JLabel();
            button.setIcon(ResourceFactory.getIcon(icon));
            button.setAlignmentX(Component.CENTER_ALIGNMENT);

            innerPanel = new JPanel();
            innerPanel.setBackground(BUTTON_BAR_BG_COLOR);
            innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
            innerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            innerPanel.setMaximumSize(new Dimension(80, 100));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setForeground(Color.WHITE);

            innerPanel.add(button);
            innerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            innerPanel.add(titleLabel);

            setLayout(new GridBagLayout()); // we use GridBag to center the component within the panel
            setPreferredSize(new Dimension(80, 80));
            setMaximumSize(new Dimension(80, 80));
            add(innerPanel);
            setBackground(BUTTON_BAR_BG_COLOR);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void highlight() {
            setBackground(BUTTON_BAR_HOVER_BG_COLOR);
            innerPanel.setBackground(BUTTON_BAR_HOVER_BG_COLOR);
        }

        void unhighlight() {
            setBackground(BUTTON_BAR_BG_COLOR);
            innerPanel.setBackground(BUTTON_BAR_BG_COLOR);
        }

        void select() {
            setBackground(BUTTON_BAR_SELECTED_BG_COLOR);
            innerPanel.setBackground(BUTTON_BAR_SELECTED_BG_COLOR);
        }
    }


    private Option selectedOption = Option.HOME;
    private MainButton homeButton;
    private MainButton unpackButton;
    private MainButton cleanButton;
    private MainButton infoButton;


    public MainButtonPanel() {

        setBackground(BUTTON_BAR_BG_COLOR);
        setBorder(strutBorder(Color.YELLOW));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(80, 300));

        homeButton = createHomeButton();
        unpackButton = createUnpackButton();
        cleanButton = createCleanButton();
        infoButton = createInfoButton();
        homeButton.select();

        add(Box.createRigidArea(new Dimension(0, 2)));
        add(homeButton);
        add(createSeparator());
        add(unpackButton);
        add(createSeparator());
        add(cleanButton);
        add(createSeparator());
        add(infoButton);
        add(createSeparator());
    }


    private JSeparator createSeparator() {
        var separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(100, 1));
        separator.setForeground(SEPARATOR_COLOR);

        return separator;
    }


    private MainButton createHomeButton() {
        return createMainOptionButton(Option.HOME, ResourceFactory.Icon.HOME, "Home");
    }

    private MainButton createUnpackButton() {
        return createMainOptionButton(Option.UNPACK, ResourceFactory.Icon.UNPACK, "Unpack DBD");
    }

    private MainButton createCleanButton() {
        return createMainOptionButton(Option.CLEAN, ResourceFactory.Icon.BROOM, "Clean files");
    }

    private MainButton createInfoButton() {
        return createMainOptionButton(Option.INFO, ResourceFactory.Icon.INFO, "App info");
    }


    private MainButton createMainOptionButton(Option option, ResourceFactory.Icon icon, String title) {
        var button = new MainButton(icon, title);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (selectedOption != option) {
                    button.highlight();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedOption != option) {
                    button.unhighlight();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectedOption != option) {
                    unselectAll();
                    button.select();
                    selectedOption = option;
                }
                button.select();
                eventSupport.fireEvent(EventType.OPTION_SELECTED, option);
            }
        });

        return button;
    }

    private void unselectAll() {
        homeButton.unhighlight();
        unpackButton.unhighlight();
        cleanButton.unhighlight();
        infoButton.unhighlight();
    }


    public void registerListener(Object eventType, EventListener eventListener) {
        eventSupport.registerListener(eventType, eventListener);
    }


}
