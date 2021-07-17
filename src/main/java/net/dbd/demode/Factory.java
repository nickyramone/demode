package net.dbd.demode;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.config.AppProperties;
import net.dbd.demode.pak.PakMetaReader;
import net.dbd.demode.service.UserSettings;
import net.dbd.demode.ui.HomePanel;
import net.dbd.demode.ui.MainWindow;
import net.dbd.demode.ui.unpacker.UnpackerPanel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for components.
 * Can eventually be replaced by an IOC container like Guice or Spring but, unless I find it very necessary,
 * I'd rather keep it simple to avoid bloating the app.
 *
 * @author Nicky Ramone
 */
@UtilityClass
@Slf4j
public final class Factory {


    private static final Map<Class, Object> instances = new HashMap<>();


    private static <T> T getInstance(Class<T> clazz) {
        return getInstance(clazz, null);
    }


    private static <T> T getInstance(Class<T> clazz, Supplier<T> objFactory) {
        T instance = clazz.cast(instances.get(clazz));

        if (instance == null) {
            try {
                instance = objFactory != null ? objFactory.get() : clazz.getDeclaredConstructor().newInstance();
                instances.put(clazz, instance);
            } catch (Exception e) {
                log.error("Failed to instantiate class.", e);
                throw new RuntimeException("Failed to instantiate class.");
            }
        }

        return instance;
    }


    public static AppProperties appProperties() {
        return getInstance(AppProperties.class);
    }

    public static UserSettings userSettings() {
        return getInstance(UserSettings.class);
    }

    public static MainWindow mainWindow() {
        return getInstance(MainWindow.class, () -> new MainWindow(appProperties(), homePanel(), unpackerPanel()));
    }

    public static HomePanel homePanel() {
        return getInstance(HomePanel.class, () -> new HomePanel(appProperties(), userSettings()));
    }

    public static UnpackerPanel unpackerPanel() {
        return getInstance(UnpackerPanel.class, () -> new UnpackerPanel(userSettings(), pakMetaReader()));
    }

    public static PakMetaReader pakMetaReader() {
        return getInstance(PakMetaReader.class, PakMetaReader::new);
    }

}
