package uitesting.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages application configuration from properties file.
 */
public class ConfigManager {

    private static ConfigManager instance;
    private final Properties properties;

    private ConfigManager() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                throw new RuntimeException("application.properties not found in classpath");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    // Convenience getters
    public String getBrowserType()        { return get("browser.type", "chromium"); }
    public boolean isHeadless()           { return getBoolean("browser.headless", false); }
    public int getSlowMo()                { return getInt("browser.slow_mo", 0); }
    public int getTimeout()               { return getInt("browser.timeout", 30000); }
    public String getBaseUrl()            { return get("app.base.url"); }
    public String getDbServer()           { return get("db.server"); }
    public int getDbPort()                { return getInt("db.port", 1433); }
    public String getDbName()             { return get("db.name"); }
    public String getDbUsername()         { return get("db.username"); }
    public String getDbPassword()         { return get("db.password"); }
    public String getLoginEmail()         { return get("login.email"); }
    public String getLoginPassword()      { return get("login.password"); }
    public String getExcelFile()          { return get("test.excel.file"); }
    public String getManualFile()         { return get("test.manual.file"); }
    public String getValidationFile()     { return get("test.validation.file"); }
    public String getScreenshotsDir()     { return get("test.screenshots.dir"); }
    public String getNavigationDbTable()  { return get("navigation.db.table", "[SideMenu]"); }

    public String getDbConnectionString() {
        return String.format(
            "jdbc:sqlserver://%s:%d;databaseName=%s;user=%s;password=%s;encrypt=false",
            getDbServer(), getDbPort(), getDbName(), getDbUsername(), getDbPassword()
        );
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
