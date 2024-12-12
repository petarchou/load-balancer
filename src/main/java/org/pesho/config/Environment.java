package org.pesho.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Environment {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")  // path to .env file
            .ignoreIfMissing()
            .load();

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Environment.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public static String getJedisUser() {
        return dotenv.get("JEDIS_USER");
    }

    public static String getJedisPassword() {
        return dotenv.get("JEDIS_PASSWORD");
    }

    public static String getJedisUrl() {
        return dotenv.get("JEDIS_URL");
    }

    public static Integer getJedisPort() {
        return Integer.parseInt(dotenv.get("JEDIS_PORT"));
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static Integer getIntProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}
