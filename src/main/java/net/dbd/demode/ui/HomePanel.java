package net.dbd.demode.ui;

import net.dbd.demode.config.AppProperties;
import net.dbd.demode.config.UserSettings;
import net.dbd.demode.service.DbdPathService;
import net.dbd.demode.ui.common.UiHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class HomePanel extends JPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private final AppProperties appProperties;
    private final UserSettings userSettings;
    private final DbdPathService dbdPathService;


    public HomePanel(AppProperties appProperties, UserSettings userSettings, DbdPathService dbdPathService) {
        this.appProperties = appProperties;
        this.userSettings = userSettings;
        this.dbdPathService = dbdPathService;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_COLOR);

        add(drawContentPanelTop());
        add(drawContentPanelBottom());
    }


    private JPanel drawContentPanelTop() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setMinimumSize(new Dimension(Integer.MAX_VALUE, 200));
        panel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        panel.setPreferredSize(new Dimension(0, 100));
        panel.setBorder(strutBorder(Color.BLACK));

        JLabel label = new JLabel("Welcome to " + appProperties.getAppName());
        UiHelper.changeFontSize(label, 16);
        panel.add(label);

        return panel;
    }

    private JPanel drawContentPanelBottom() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(strutBorder(Color.YELLOW));

        JLabel label = new JLabel("Please, configure paths below.", SwingConstants.CENTER);
        panel.add(label);


        JPanel dbdFolderRow = new JPanel();
        dbdFolderRow.setBackground(BG_COLOR);
        dbdFolderRow.setLayout(new FlowLayout());
        panel.add(dbdFolderRow);

        JLabel dbdFolderInputLabel = new JLabel();
        dbdFolderInputLabel.setText("DBD home path:");
        dbdFolderRow.add(dbdFolderInputLabel);

        JTextField dirInput = createDbdFolderTextField();
        dbdFolderRow.add(dirInput);

        JLabel button = new JLabel();
        button.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.FILE_BROWSER));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                if (fileChooser.showOpenDialog(button) == JFileChooser.APPROVE_OPTION) {
                    dirInput.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        dbdFolderRow.add(button);


        return panel;
    }


    private JTextField createDbdFolderTextField() {
        String dbdPath = userSettings.getDbdHomePath();

        if (dbdPath == null) {
            dbdPath = dbdPathService.findDbdInstallationPath()
                    .map(Path::toString)
                    .orElse(null);

            if (dbdPath != null) {
                userSettings.setDbdHomePath(dbdPath);
            }
        }


        JTextField dirInput = new JTextField();
        dirInput.setPreferredSize(new Dimension(400, 25));
        dirInput.setText(dbdPath);

        dirInput.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                userSettings.setDbdHomePath(dirInput.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                userSettings.setDbdHomePath(dirInput.getText());
            }

            public void insertUpdate(DocumentEvent e) {
                userSettings.setDbdHomePath(dirInput.getText());
            }
        });

        return dirInput;
    }
}
