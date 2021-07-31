package net.dbd.demode.config;

import lombok.extern.slf4j.Slf4j;
import org.ini4j.Profile;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Nicky Ramone
 */
@Slf4j
public class UserSettings {

    private static final String SETTINGS_FILENAME = "demode.ini";
    private static final long SAVE_INTERVAL_SECONDS = 10;

    private final Wini ini;
    private final Profile.Section globalSection;

    private volatile boolean dirty;
    private Instant lastChange = Instant.now();


    public UserSettings(Path appHome) throws IOException {
        File settingsFile = appHome.resolve(SETTINGS_FILENAME).toFile();

        if (!settingsFile.exists()) {
            if (!settingsFile.createNewFile()) {
                throw new IOException("Failed to initialize settings manager. File does not exist "
                        + "and it could not be created.");
            }
        }

        ini = new Wini(settingsFile);

        if (ini.isEmpty()) {
            ini.add("?");
        }
        globalSection = ini.get("?");

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_INTERVAL_SECONDS * 1000, SAVE_INTERVAL_SECONDS * 1000);

    }


    public String getDbdHomePath() {
        return globalSection.get("dbd.home");
    }

    public void setDbdHomePath(String path) {
        set("dbd.home", path);
    }

    public String get(String key) {
        return globalSection.get(key);
    }

    public String get(String key, String defaultValue) {
        String value = globalSection.get(key);

        return value != null ? value : defaultValue;
    }

    public void set(String key, Object value) {
        String oldValue = get(key);
        String newValue = (value == null || value instanceof String) ? (String) value : String.valueOf(value);

        // only set it if the new value differs from the old one
        if (!Objects.equals(oldValue, newValue)) {
            globalSection.put(key, value);
            dirty = true;
            lastChange = Instant.now();
        }
    }

    private void save() {
        int secondsElapsedSinceLastChange = (int) Duration.between(lastChange, Instant.now()).toSeconds();

        if (secondsElapsedSinceLastChange > SAVE_INTERVAL_SECONDS) {
            forceSave();
        }
    }

    public void forceSave() {
        if (dirty) {
            try {
                log.debug("Saving settings.");
                ini.store();
                dirty = false;
            } catch (IOException e) {
                log.error("Failed to save settings.", e);
            }
        }
    }

}
