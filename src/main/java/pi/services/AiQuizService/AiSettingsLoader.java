package pi.services.AiQuizService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AiSettingsLoader {

    private AiSettingsLoader() {
    }

    public static AiSettings load() {
        Properties properties = new Properties();
        boolean loaded = false;
        String loadedFrom = "";

        try (InputStream in = AiSettingsLoader.class.getClassLoader().getResourceAsStream("ai.properties")) {
            if (in != null) {
                properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                loaded = true;
                loadedFrom = "classpath:ai.properties";
            }
        } catch (IOException e) {
            System.out.println("[AI] Impossible de charger ai.properties depuis le classpath: " + e.getMessage());
        }

        if (!loaded) {
            Path[] candidates = new Path[] {
                    Paths.get("ai.properties"),
                    Paths.get("config", "ai.properties")
            };
            for (Path candidate : candidates) {
                if (!Files.exists(candidate)) {
                    continue;
                }
                try (InputStream in = Files.newInputStream(candidate)) {
                    properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                    loaded = true;
                    loadedFrom = candidate.toAbsolutePath().toString();
                    System.out.println("[AI] ai.properties chargé depuis: " + candidate.toAbsolutePath());
                    break;
                } catch (IOException e) {
                    System.out.println("[AI] Impossible de charger " + candidate.toAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        String apiKey = normalizeSecret(getString(properties, "gemini.api.key", ""));
        String model = getString(properties, "gemini.model", "");

        String envKey = System.getenv("GEMINI_API_KEY");
        String propKey = System.getProperty("gemini.api.key");

        apiKey = normalizeSecret(fallback(apiKey, envKey));
        apiKey = normalizeSecret(fallback(apiKey, propKey));

        model = fallback(model, System.getProperty("gemini.model"));
        model = fallback(model, System.getenv("GEMINI_MODEL"));

        if (apiKey.isBlank()) {
            System.out.println("[AI] Clé Gemini manquante. Sources: "
                    + "fileLoaded=" + loaded
                    + (loaded ? " (" + loadedFrom + ")" : "")
                    + ", env(GEMINI_API_KEY)=" + (envKey != null && !envKey.isBlank())
                    + ", sysprop(gemini.api.key)=" + (propKey != null && !propKey.isBlank()));
        }

        return new AiSettings(apiKey, model);
    }

    private static String getString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private static String fallback(String currentValue, String overrideValue) {
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        return overrideValue == null ? "" : overrideValue.trim();
    }

    private static String normalizeSecret(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "").replace("\t", "").trim();
    }
}
