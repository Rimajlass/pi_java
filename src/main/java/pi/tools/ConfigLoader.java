package pi.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class ConfigLoader {

    private static final String LOCAL_CONFIG_FILE = "config.properties";
    private static final String DOT_ENV_FILE = ".env";
    private static final String DEFAULTS_RESOURCE = "/decides-defaults.properties";
    private static final Properties DEFAULTS = loadDefaults();
    private static final Properties LOCAL = loadLocal();
    private static final Properties DOT_ENV = loadDotEnv();

    private ConfigLoader() {
    }

    public static String get(String key, String fallback) {
        String envValue = System.getenv(key);
        if (hasText(envValue)) {
            return envValue.trim();
        }

        String propertyValue = System.getProperty(key);
        if (hasText(propertyValue)) {
            return propertyValue.trim();
        }

        String localValue = LOCAL.getProperty(key);
        if (hasText(localValue)) {
            return localValue.trim();
        }

        String dotEnvValue = DOT_ENV.getProperty(key);
        if (hasText(dotEnvValue)) {
            return dotEnvValue.trim();
        }

        String defaultValue = DEFAULTS.getProperty(key);
        if (hasText(defaultValue)) {
            return defaultValue.trim();
        }

        return fallback;
    }

    public static int getInt(String key, int fallback) {
        String value = get(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String diagnostics() {
        StringBuilder builder = new StringBuilder();
        builder.append("user.dir=").append(System.getProperty("user.dir", "")).append(" | ");
        builder.append("checked .env: ");
        for (Path path : candidatePaths(DOT_ENV_FILE)) {
            builder.append(path.toAbsolutePath()).append("[").append(Files.exists(path) ? "found" : "missing").append("] ");
        }
        builder.append("| checked config.properties: ");
        for (Path path : candidatePaths(LOCAL_CONFIG_FILE)) {
            builder.append(path.toAbsolutePath()).append("[").append(Files.exists(path) ? "found" : "missing").append("] ");
        }
        return builder.toString().trim();
    }

    private static Properties loadDefaults() {
        Properties properties = new Properties();
        try (InputStream inputStream = ConfigLoader.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static Properties loadLocal() {
        Properties properties = new Properties();
        for (Path localPath : candidatePaths(LOCAL_CONFIG_FILE)) {
            if (!Files.exists(localPath)) {
                continue;
            }
            try (InputStream inputStream = Files.newInputStream(localPath)) {
                properties.load(inputStream);
                return properties;
            } catch (IOException ignored) {
            }
        }
        return properties;
    }

    private static Properties loadDotEnv() {
        Properties properties = new Properties();
        for (Path dotEnvPath : candidatePaths(DOT_ENV_FILE)) {
            if (!Files.exists(dotEnvPath)) {
                continue;
            }

            try {
                for (String rawLine : Files.readAllLines(dotEnvPath)) {
                    String line = rawLine == null ? "" : rawLine.trim();
                    if (line.isBlank() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    if (line.startsWith("export ")) {
                        line = line.substring("export ".length()).trim();
                    } else if (line.startsWith("set ")) {
                        line = line.substring("set ".length()).trim();
                    }

                    int separatorIndex = line.indexOf('=');
                    String key = line.substring(0, separatorIndex).trim();
                    String value = line.substring(separatorIndex + 1).trim();
                    int commentIndex = value.indexOf(" #");
                    if (commentIndex >= 0) {
                        value = value.substring(0, commentIndex).trim();
                    }
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (hasText(key) && hasText(value)) {
                        properties.setProperty(key, value);
                    }
                }
                return properties;
            } catch (IOException ignored) {
            }
        }
        return properties;
    }

    private static List<Path> candidatePaths(String fileName) {
        Set<Path> paths = new LinkedHashSet<>();
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path userDir = Path.of(System.getProperty("user.dir", "")).toAbsolutePath().normalize();
        paths.add(cwd.resolve(fileName));
        paths.add(userDir.resolve(fileName));

        Path walk = userDir;
        for (int i = 0; i < 4 && walk != null; i++) {
            paths.add(walk.resolve(fileName));
            walk = walk.getParent();
        }

        return new ArrayList<>(paths);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
