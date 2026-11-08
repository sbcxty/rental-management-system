package com.gudosnest.backend.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads database settings from config.properties in the project folder.
 */
public final class AppConfig {

    public static final Path CONFIG_FILE = Path.of("config.properties");

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private AppConfig(Properties props) {
        this.dbUrl = required(props, "db.url");
        this.dbUser = required(props, "db.user");
        this.dbPassword = props.getProperty("db.password", "");
    }

    /** Loads config.properties, or throws a clear message if it is missing or incomplete. */
    public static AppConfig load() {
        if (!Files.exists(CONFIG_FILE)) {
            throw new IllegalStateException("config.properties not found in "
                    + CONFIG_FILE.toAbsolutePath().getParent()
                    + ". Copy config.properties.example to config.properties and fill in your MySQL details.");
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read config.properties: " + e.getMessage(), e);
        }
        return new AppConfig(props);
    }

    private static String required(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("config.properties is missing \"" + key + "\".");
        }
        return value.trim();
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }
}
