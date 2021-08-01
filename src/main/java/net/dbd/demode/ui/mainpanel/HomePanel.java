package net.dbd.demode.ui.mainpanel;

import net.dbd.demode.config.AppProperties;
import net.dbd.demode.config.UserSettings;
import net.dbd.demode.service.DbdPathService;
import net.dbd.demode.ui.ResourceFactory;
import net.dbd.demode.ui.common.UiHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import static net.dbd.demode.ui.common.UiHelper.changeFontSize;
import static net.dbd.demode.ui.common.UiHelper.strutBorder;

/**
 * @author Nicky Ramone
 */
public class HomePanel extends JPanel {

    private static final Color BG_COLOR = Color.WHITE;

    private final AppProperties appProperties;
    private final UserSettings userSettings;
    private final DbdPathService dbdPathService;

    private JTextField homePathInput;


    public HomePanel(AppProperties appProperties, UserSettings userSettings, DbdPathService dbdPathService) {
        this.appProperties = appProperties;
        this.userSettings = userSettings;
        this.dbdPathService = dbdPathService;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_COLOR);

        add(drawContentPanelTop());
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(drawContentPanelBottom());
    }


    private JPanel drawContentPanelTop() {
        String title = String.format("<html>Welcome to <b><i>%s</i></b></html", appProperties.getAppName());
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        changeFontSize(label, 16);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(strutBorder(Color.RED));
        panel.setPreferredSize(new Dimension(0, 100));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private JPanel drawContentPanelBottom() {
        JLabel label = new JLabel("Please, configure paths below.");
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        label.setBorder(strutBorder(Color.RED));

        JLabel dbdFolderInputLabel = new JLabel();
        dbdFolderInputLabel.setText("DBD home path:");

        homePathInput = createDbdFolderTextField();

        JLabel browseButton = new JLabel();
        browseButton.setToolTipText("Browse file system to select folder");
        browseButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.FILE_BROWSER));
        browseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        browseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                if (fileChooser.showOpenDialog(browseButton) == JFileChooser.APPROVE_OPTION) {
                    homePathInput.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });


        JLabel findButton = new JLabel();
        findButton.setToolTipText("Try to locate folder automatically");
        findButton.setIcon(ResourceFactory.getIcon(ResourceFactory.Icon.MAGNIFYING_GLASS));
        findButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        findButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                locateDbdHomePath();
            }
        });

        JPanel dbdFolderRow = new JPanel();
        dbdFolderRow.setLayout(new FlowLayout());
        dbdFolderRow.setBackground(BG_COLOR);
        dbdFolderRow.add(dbdFolderInputLabel);
        dbdFolderRow.add(homePathInput);
        dbdFolderRow.add(browseButton);
        dbdFolderRow.add(findButton);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setBackground(BG_COLOR);
        panel.setBorder(strutBorder(Color.YELLOW));

        panel.add(label);
        panel.add(dbdFolderRow);

        return panel;
    }


    private JTextField createDbdFolderTextField() {
        String dbdPath = userSettings.getDbdHomePath();

        if (dbdPath == null) {
            dbdPath = dbdPathService.findDbdHomePath()
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


    public void locateDbdHomePath() {
        homePathInput.setText(dbdPathService.findDbdHomePath().map(Object::toString).orElse(null));
    }

}
