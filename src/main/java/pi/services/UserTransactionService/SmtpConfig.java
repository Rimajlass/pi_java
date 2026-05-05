package pi.services.UserTransactionService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record SmtpConfig(
        String host,
        int port,
        String username,
        String password,
        String from,
        boolean auth,
        boolean startTls,
        String appBaseUrl
) {
    public static SmtpConfig fromEnvironment() {
        Properties merged = loadConfigFiles();
        String host = read("SMTP_HOST", "smtp.gmail.com", merged);
        int port = parsePort(read("SMTP_PORT", "587", merged));
        String username = read("SMTP_USERNAME", "", merged);
        String password = read("SMTP_PASSWORD", "", merged);
        String from = read("SMTP_FROM", username, merged);
        boolean auth = Boolean.parseBoolean(read("SMTP_AUTH", "true", merged));
        boolean startTls = Boolean.parseBoolean(read("SMTP_STARTTLS", "true", merged));
        String appBaseUrl = read("APP_BASE_URL", "https://app.example.com/reset-password", merged);
        return new SmtpConfig(host, port, username, password, from, auth, startTls, appBaseUrl);
    }

    public boolean isReady() {
        return host != null && !host.isBlank()
                && from != null && !from.isBlank()
                && (!auth || (username != null && !username.isBlank() && password != null && !password.isBlank()));
    }

    private static String read(String key, String defaultValue, Properties fileProps) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }

        String file = fileProps.getProperty(key);
        if (file != null && !file.isBlank()) {
            return file.trim();
        }

        return defaultValue;
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 587;
        }
    }

    private static Properties loadConfigFiles() {
        Properties props = new Properties();

        // Load mail.properties first.
        loadPropertiesFile(props, Path.of("mail.properties"));

        // Then .env, so .env overrides mail.properties when both exist.
        loadDotEnvFile(props, Path.of(".env"));
        // Finally .env.local for local developer overrides.
        loadDotEnvFile(props, Path.of(".env.local"));

        // Optional classpath fallback for mail.properties.
        try (InputStream in = SmtpConfig.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                Properties classpath = new Properties();
                classpath.load(in);
                classpath.putAll(props);
                return classpath;
            }
        } catch (IOException ignored) {
            // Keep local properties.
        }

        return props;
    }

    private static void loadPropertiesFile(Properties props, Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Properties fromFile = new Properties();
            fromFile.load(in);
            props.putAll(fromFile);
        } catch (IOException ignored) {
            // Ignore invalid file and keep other sources.
        }
    }

    private static void loadDotEnvFile(Properties props, Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isBlank()) {
                    props.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Ignore invalid file and keep other sources.
        }
    }
}
