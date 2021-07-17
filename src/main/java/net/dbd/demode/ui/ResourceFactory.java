package net.dbd.demode.ui;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for obtaining singleton instances of different resources.
 *
 * @author NickyRamone
 */
@Slf4j
@UtilityClass
public final class ResourceFactory {

    private static final Map<Icon, ImageIcon> iconCache = new HashMap<>();

    public enum Icon {
        APP("/icons/app.png"),
        FILE_BROWSER("/icons/folder.png"),
        HOME("/icons/home.png"),
        UNPACK("/icons/unpack.png");

        final String imagePath;

        Icon(String imagePath) {
            this.imagePath = imagePath;
        }
    }


    public Image getAppIcon() {
        URL imageUrl = ResourceFactory.class.getResource(Icon.APP.imagePath);

        return Toolkit.getDefaultToolkit().getImage(imageUrl);
    }

    public ImageIcon getIcon(Icon icon) {
        ImageIcon imageIcon = iconCache.get(icon);

        if (imageIcon == null) {
            URL imageUrl = ResourceFactory.class.getResource(icon.imagePath);

            if (imageUrl != null) {
                imageIcon = new ImageIcon(imageUrl);
                iconCache.put(icon, imageIcon);
            } else {
                log.error("Failed to load '{}' icon.", icon.name());
            }
        }

        return imageIcon;
    }

}
