package pi.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AppEnv {

    private static final Map<String, String> FILE_VALUES = loadEnvFile();

    private AppEnv() {
    }

    public static String get(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return FILE_VALUES.get(key);
    }

    public static boolean has(String key) {
        String value = get(key);
        return value != null && !value.isBlank();
    }

    private static Map<String, String> loadEnvFile() {
        Map<String, String> values = new HashMap<>();
        List<Path> envPaths = findEnvPaths();
        if (envPaths.isEmpty()) {
            maybeLogMissingEnv();
            return values;
        }

        for (Path envPath : envPaths) {
            try {
                List<String> lines = Files.readAllLines(envPath);
                for (String rawLine : lines) {
                    String line = rawLine == null ? "" : rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int separator = line.indexOf('=');
                    String key = line.substring(0, separator).trim();
                    // Strip UTF-8 BOM if present on first key in file
                    if (!key.isEmpty() && key.charAt(0) == '\ufeff') {
                        key = key.substring(1).trim();
                    }
                    // Support "export KEY=VALUE" style lines
                    if (key.regionMatches(true, 0, "export", 0, 6)) {
                        key = key.substring(6).trim();
                    }
                    String value = line.substring(separator + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (!key.isEmpty() && !values.containsKey(key)) {
                        values.put(key, value);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        maybeLogLoadedEnv(envPaths.get(0), values);
        return values;
    }

    /**
     * JavaFX/IDE launches often use a working directory that is not the project root.
     * Walk up a few parents to find a {@code .env} next to the repo.
     */
    private static List<Path> findEnvPaths() {
        Set<Path> unique = new LinkedHashSet<>();

        addEnvWalk(unique, Path.of("").toAbsolutePath().normalize());
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            addEnvWalk(unique, Path.of(userDir).toAbsolutePath().normalize());
        }

        // Common OneDrive desktop layout on Windows (best-effort)
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            Path homePath = Path.of(home);
            addEnvWalk(unique, homePath.resolve("OneDrive").resolve("Bureau").resolve("pi_java"));
            addEnvWalk(unique, homePath.resolve("OneDrive").resolve("Desktop").resolve("pi_java"));
        }

        return new ArrayList<>(unique);
    }

    private static void addEnvWalk(Set<Path> out, Path start) {
        if (start == null) {
            return;
        }
        Path cursor = start;
        for (int depth = 0; depth < 10; depth++) {
            Path candidate = cursor.resolve(".env");
            if (Files.exists(candidate)) {
                out.add(candidate.toAbsolutePath().normalize());
                return;
            }
            Path parent = cursor.getParent();
            if (parent == null || parent.equals(cursor)) {
                break;
            }
            cursor = parent;
        }
    }

    private static void maybeLogMissingEnv() {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("ENV_DEBUG", "false"))
                && !"1".equals(System.getenv("ENV_DEBUG"))
                && !Boolean.getBoolean("ENV_DEBUG")) {
            return;
        }
        Path start = Path.of("").toAbsolutePath().normalize();
        Path userDir = null;
        try {
            String ud = System.getProperty("user.dir");
            if (ud != null && !ud.isBlank()) {
                userDir = Path.of(ud).toAbsolutePath().normalize();
            }
        } catch (Exception ignored) {
        }
        System.err.println("[ENV] no .env found while walking parents from:");
        System.err.println("[ENV] - relative path base: " + start);
        if (userDir != null) {
            System.err.println("[ENV] - user.dir base: " + userDir);
        }
    }

    private static void maybeLogLoadedEnv(Path envPath, Map<String, String> values) {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("ENV_DEBUG", "false"))
                && !"1".equals(System.getenv("ENV_DEBUG"))
                && !Boolean.getBoolean("ENV_DEBUG")) {
            return;
        }
        String dsn = values.get("MAILER_DSN");
        String from = values.get("MAILER_FROM_ADDRESS");
        System.err.println("[ENV] loaded .env from: " + envPath.toAbsolutePath().normalize());
        System.err.println("[ENV] MAILER_DSN present=" + (dsn != null && !dsn.isBlank())
                + (dsn == null ? "" : (" (length=" + dsn.length() + ")")));
        System.err.println("[ENV] MAILER_FROM_ADDRESS present=" + (from != null && !from.isBlank()));
    }
}
