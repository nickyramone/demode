package net.dbd.demode.config;

import lombok.extern.slf4j.Slf4j;
import net.dbd.demode.Boot;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Manage application properties.
 *
 * @author Nicky Ramone
 */
@Slf4j
public class AppProperties {

    private Properties properties;

    public AppProperties() throws IOException, URISyntaxException {
        properties = new Properties();
        properties.load(AppProperties.class.getClassLoader().getResourceAsStream("app.properties"));

        URI execUri = Boot.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        String appHomePath = new File(execUri).toPath().getParent().toString();
        System.setProperty("app.home", appHomePath);

        properties.put("app.home", appHomePath);

        log.info("App home: {}", properties.getProperty("app.home"));
        log.info("App version: {}", properties.getProperty("app.version"));
    }

    public String getAppName() {
        return get("app.name");
    }

    public String getAppSubtitle() {
        return get("app.name.subtitle");
    }

    public String getAppVersion() {
        return get("app.version");
    }

    private String get(String key) {
        return (String) properties.get(key);
    }

    private boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);

        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private int getInt(String key) {
        String value = properties.getProperty(key);

        return value == null ? 0 : Integer.parseInt(value);
    }

}
