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
    private static final String APP_BACKGROUND_LOGO_PATH = "/images/background_logo.png";

    public enum Icon {
        APP("/images/icons/app.png"),
        BROOM("/images/icons/broom.png"),
        FILE_BROWSER("/images/icons/folder.png"),
        HOME("/images/icons/home.png"),
        INFO("/images/icons/info.png"),
        MAGNIFYING_GLASS("/images/icons/magnifying-glass.png"),
        UNPACK("/images/icons/unpack.png");

        final String imagePath;

        Icon(String imagePath) {
            this.imagePath = imagePath;
        }
    }


    public Image getAppIcon() {
        URL imageUrl = ResourceFactory.class.getResource(Icon.APP.imagePath);

        return Toolkit.getDefaultToolkit().getImage(imageUrl);
    }

    public Image getAppBackgroundLogo() {
        URL imageUrl = ResourceFactory.class.getResource(APP_BACKGROUND_LOGO_PATH);

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
