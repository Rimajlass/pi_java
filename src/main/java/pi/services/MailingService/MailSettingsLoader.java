package pi.services.MailingService;

import pi.tools.AppEnv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class MailSettingsLoader {

    private MailSettingsLoader() {
    }

    public static MailSettings load() {
        Properties properties = new Properties();

        boolean loaded = false;
        try (InputStream in = MailSettingsLoader.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                loaded = true;
            }
        } catch (IOException e) {
            System.out.println("[MAIL] Impossible de charger mail.properties depuis le classpath: " + e.getMessage());
        }

        if (!loaded) {
            Path[] candidates = new Path[] {
                    Paths.get("mail.properties"),
                    Paths.get("config", "mail.properties")
            };

            for (Path candidate : candidates) {
                if (!Files.exists(candidate)) {
                    continue;
                }
                try (InputStream in = Files.newInputStream(candidate)) {
                    properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    loaded = true;
                    System.out.println("[MAIL] mail.properties charge depuis: " + candidate.toAbsolutePath());
                    break;
                } catch (IOException e) {
                    System.out.println("[MAIL] Impossible de charger " + candidate.toAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        boolean enabled = getBoolean(properties, "mail.enabled", true);
        String host = getString(properties, "mail.smtp.host", "");
        int port = getInt(properties, "mail.smtp.port", 587);
        boolean startTls = getBoolean(properties, "mail.smtp.starttls", true);
        boolean ssl = getBoolean(properties, "mail.smtp.ssl", false);
        boolean debug = getBoolean(properties, "mail.debug", false);
        boolean debugPopup = getBoolean(properties, "mail.debugPopup", false);
        String username = getString(properties, "mail.smtp.username", "");
        String password = normalizeSecret(getString(properties, "mail.smtp.password", ""));
        String from = getString(properties, "mail.from", username);
        String fromName = getString(properties, "mail.fromName", "Pi App");

        enabled = enabled || getEnvBoolean("PI_MAIL_ENABLED", false) || getAppEnvBoolean("PI_MAIL_ENABLED", false);
        host = fallback(host, System.getenv("PI_MAIL_SMTP_HOST"));
        host = fallback(host, AppEnv.get("PI_MAIL_SMTP_HOST"));
        port = fallback(port, System.getenv("PI_MAIL_SMTP_PORT"));
        port = fallback(port, AppEnv.get("PI_MAIL_SMTP_PORT"));
        startTls = startTls || getEnvBoolean("PI_MAIL_SMTP_STARTTLS", false) || getAppEnvBoolean("PI_MAIL_SMTP_STARTTLS", false);
        ssl = ssl || getEnvBoolean("PI_MAIL_SMTP_SSL", false) || getAppEnvBoolean("PI_MAIL_SMTP_SSL", false);
        debug = debug || getEnvBoolean("PI_MAIL_DEBUG", false) || getAppEnvBoolean("PI_MAIL_DEBUG", false) || getAppEnvBoolean("MAIL_DEBUG", false);
        debugPopup = debugPopup || getEnvBoolean("PI_MAIL_DEBUG_POPUP", false) || getAppEnvBoolean("PI_MAIL_DEBUG_POPUP", false);
        username = fallback(username, System.getenv("PI_MAIL_SMTP_USERNAME"));
        username = fallback(username, AppEnv.get("PI_MAIL_SMTP_USERNAME"));
        password = normalizeSecret(fallback(password, System.getenv("PI_MAIL_SMTP_PASSWORD")));
        password = normalizeSecret(fallback(password, AppEnv.get("PI_MAIL_SMTP_PASSWORD")));
        from = fallback(from, System.getenv("PI_MAIL_FROM"));
        from = fallback(from, AppEnv.get("PI_MAIL_FROM"));
        fromName = fallback(fromName, System.getenv("PI_MAIL_FROM_NAME"));
        fromName = fallback(fromName, AppEnv.get("PI_MAIL_FROM_NAME"));

        String mailerDsn = AppEnv.get("MAILER_DSN");
        if ((host == null || host.isBlank() || username == null || username.isBlank() || password == null || password.isBlank())
                && mailerDsn != null && !mailerDsn.isBlank()) {
            DsnMailConfig dsnConfig = parseMailerDsn(mailerDsn);
            host = fallback(host, dsnConfig.host());
            port = dsnConfig.port() > 0 ? portFallback(port, dsnConfig.port()) : port;
            startTls = startTls || dsnConfig.startTls();
            ssl = ssl || dsnConfig.ssl();
            username = fallback(username, dsnConfig.username());
            password = normalizeSecret(fallback(password, dsnConfig.password()));
        }

        from = fallback(from, AppEnv.get("MAILER_FROM_ADDRESS"));
        fromName = fallback(fromName, AppEnv.get("MAILER_FROM_NAME"));

        MailSettings settings = new MailSettings(enabled, host, port, startTls, ssl, debug, debugPopup, username, password, from, fromName);
        if (settings.isDebug()) {
            System.out.println("[MAIL] Config: enabled=" + settings.isEnabled()
                    + ", host=" + settings.getHost()
                    + ", port=" + settings.getPort()
                    + ", startTls=" + settings.isStartTls()
                    + ", ssl=" + settings.isSsl()
                    + ", debugPopup=" + settings.isDebugPopup()
                    + ", username=" + (settings.getUsername() == null ? "" : settings.getUsername()));
        }
        return settings;
    }

    private static String getString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private static int getInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean getEnvBoolean(String env, boolean defaultValue) {
        String value = System.getenv(env);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private static boolean getAppEnvBoolean(String key, boolean defaultValue) {
        String value = AppEnv.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private static String fallback(String currentValue, String candidateValue) {
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        return candidateValue == null ? "" : candidateValue.trim();
    }

    private static int fallback(int currentValue, String candidateValue) {
        if (candidateValue == null || candidateValue.isBlank()) {
            return currentValue;
        }
        try {
            return Integer.parseInt(candidateValue.trim());
        } catch (NumberFormatException e) {
            return currentValue;
        }
    }

    private static int portFallback(int currentValue, int candidateValue) {
        return currentValue > 0 ? currentValue : candidateValue;
    }

    private static String normalizeSecret(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "").replace("\t", "").trim();
    }

    private static DsnMailConfig parseMailerDsn(String dsn) {
        try {
            String trimmed = dsn.trim();
            URI uri = URI.create(trimmed);
            String authorityPart = trimmed;
            int schemeSeparator = authorityPart.indexOf("://");
            if (schemeSeparator >= 0) {
                authorityPart = authorityPart.substring(schemeSeparator + 3);
            }
            int querySeparator = authorityPart.indexOf('?');
            if (querySeparator >= 0) {
                authorityPart = authorityPart.substring(0, querySeparator);
            }

            String userInfo = "";
            String hostPortPart = authorityPart;
            int atIndex = authorityPart.lastIndexOf('@');
            if (atIndex >= 0) {
                userInfo = authorityPart.substring(0, atIndex);
                hostPortPart = authorityPart.substring(atIndex + 1);
            }

            String username = "";
            String password = "";
            if (userInfo != null && !userInfo.isBlank()) {
                int separator = userInfo.indexOf(':');
                if (separator >= 0) {
                    username = decodeUrlPart(userInfo.substring(0, separator));
                    password = decodeUrlPart(userInfo.substring(separator + 1));
                } else {
                    username = decodeUrlPart(userInfo);
                }
            }

            String host = uri.getHost();
            int port = uri.getPort();
            if ((host == null || host.isBlank()) && !hostPortPart.isBlank()) {
                int colonIndex = hostPortPart.lastIndexOf(':');
                if (colonIndex >= 0) {
                    host = hostPortPart.substring(0, colonIndex).trim();
                    try {
                        port = Integer.parseInt(hostPortPart.substring(colonIndex + 1).trim());
                    } catch (NumberFormatException ignored) {
                        port = 587;
                    }
                } else {
                    host = hostPortPart.trim();
                }
            }

            Map<String, String> query = parseQuery(uri.getRawQuery());
            String encryption = query.getOrDefault("encryption", "").trim().toLowerCase();
            boolean startTls = "tls".equals(encryption) || "starttls".equals(encryption);
            boolean ssl = "ssl".equals(encryption) || "smtps".equalsIgnoreCase(uri.getScheme());

            return new DsnMailConfig(
                    host == null ? "" : host.trim(),
                    port > 0 ? port : 587,
                    username,
                    password,
                    startTls,
                    ssl
            );
        } catch (Exception e) {
            System.out.println("[MAIL] MAILER_DSN invalide: " + e.getMessage());
            return new DsnMailConfig("", 587, "", "", false, false);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            int separator = pair.indexOf('=');
            if (separator < 0) {
                map.put(decodeUrlPart(pair), "");
            } else {
                map.put(decodeUrlPart(pair.substring(0, separator)), decodeUrlPart(pair.substring(separator + 1)));
            }
        }
        return map;
    }

    private static String decodeUrlPart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record DsnMailConfig(String host, int port, String username, String password, boolean startTls, boolean ssl) {
    }
}
