package net.dbd.demode;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.config.AppProperties;
import net.dbd.demode.config.UserSettings;
import net.dbd.demode.service.*;
import net.dbd.demode.ui.CleanerPanel;
import net.dbd.demode.ui.HomePanel;
import net.dbd.demode.ui.InfoPanel;
import net.dbd.demode.ui.MainWindow;
import net.dbd.demode.ui.unpacker.UnpackerPanel;
import net.dbd.demode.util.Gson;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.dbd.demode.util.lang.LangUtil.unchecked;

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
                throw new RuntimeException("Failed to instantiate class.", e);
            }
        }

        return instance;
    }


    public static AppProperties appProperties() {
        return getInstance(AppProperties.class);
    }

    public static UserSettings userSettings() {
        return getInstance(UserSettings.class, unchecked(() -> new UserSettings(appProperties().getAppHome())));
    }

    public static FileMetadataManager fileMetadataManager() {
        return getInstance(FileMetadataManager.class);
    }

    public static DbdPathService dbdPathService() {
        return getInstance(DbdPathService.class);
    }

    public static FileCleaner fileCleaner() {
        return getInstance(FileCleaner.class, () -> new FileCleaner(fileMetadataManager()));
    }


    public static MainWindow mainWindow() {
        return getInstance(MainWindow.class, () ->
                new MainWindow(appProperties(), userSettings(), homePanel(), unpackerPanel(), cleanerPanel(), infoPanel()));
    }

    public static HomePanel homePanel() {
        return getInstance(HomePanel.class, () -> new HomePanel(appProperties(), userSettings(), dbdPathService()));
    }

    public static UnpackerPanel unpackerPanel() {
        return getInstance(UnpackerPanel.class, () -> new UnpackerPanel(appProperties(), userSettings()));
    }

    public static CleanerPanel cleanerPanel() {
        return getInstance(CleanerPanel.class, () -> new CleanerPanel(userSettings(), dbdPathService(), fileCleaner()));
    }

    public static InfoPanel infoPanel() {
        return getInstance(InfoPanel.class, () -> new InfoPanel(appProperties()));
    }

    public static DbdUnpacker newDbdUnpacker(DbdPakManager dbdPakManager) {
        return new DbdUnpacker(fileMetadataManager(), dbdPathService(), dbdPakManager);
    }

    public static DbdPakManager newDbdPakManager(Path dbdHomePath) {
        return new DbdPakManager(fileMetadataManager(), dbdPathService(), dbdHomePath);
    }


    public static Gson gson() {
        return getInstance(Gson.class);
    }
}
