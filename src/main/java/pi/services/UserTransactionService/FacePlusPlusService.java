package pi.services.UserTransactionService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class FacePlusPlusService {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRIES = 3;
    private static volatile Map<String, String> dotenvCache;

    private final HttpClient httpClient;
    private final String apiKey;
    private final String apiSecret;
    private final String baseUrl;
    private final double minConfidence;

    public FacePlusPlusService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        this.apiKey = requireConfig("FACEPP_API_KEY");
        this.apiSecret = requireConfig("FACEPP_API_SECRET");
        this.baseUrl = readConfig("FACEPP_API_BASE_URL", "https://api-us.faceplusplus.com");
        this.minConfidence = readConfigDouble("FACEPP_MIN_CONFIDENCE", 80.0);
    }

    public String extractFaceToken(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            throw new IllegalArgumentException("Image de visage introuvable.");
        }
        JsonObject json = postMultipart(baseUrl + "/facepp/v3/detect", imagePath);
        JsonArray faces = json.has("faces") ? json.getAsJsonArray("faces") : null;
        if (faces == null || faces.isEmpty()) {
            throw new IllegalStateException("Aucun visage detecte sur l'image.");
        }
        JsonObject firstFace = faces.get(0).getAsJsonObject();
        if (!firstFace.has("face_token")) {
            throw new IllegalStateException("Face token absent dans la reponse Face++.");
        }
        return firstFace.get("face_token").getAsString();
    }

    public boolean verifyFaceAgainstToken(Path liveImagePath, String storedFaceToken) {
        if (storedFaceToken == null || storedFaceToken.isBlank()) {
            throw new IllegalStateException("Aucun face token enregistre pour cet utilisateur.");
        }
        String liveToken = extractFaceToken(liveImagePath);
        String body = "api_key=" + urlEnc(apiKey)
                + "&api_secret=" + urlEnc(apiSecret)
                + "&face_token1=" + urlEnc(liveToken)
                + "&face_token2=" + urlEnc(storedFaceToken.trim());
        JsonObject json = postForm(baseUrl + "/facepp/v3/compare", body);
        if (!json.has("confidence")) {
            throw new IllegalStateException("Confidence absente dans la reponse Face++.");
        }
        double confidence = json.get("confidence").getAsDouble();
        return confidence >= minConfidence;
    }

    private JsonObject postMultipart(String url, Path imagePath) {
        String boundary = "----FacePlusBoundary" + System.currentTimeMillis();
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(imagePath);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture image impossible: " + imagePath, e);
        }

        byte[] body = buildMultipartBody(boundary, imagePath.getFileName().toString(), fileBytes);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return sendJson(request);
    }

    private byte[] buildMultipartBody(String boundary, String fileName, byte[] fileBytes) {
        String part1 = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"api_key\"\r\n\r\n"
                + apiKey + "\r\n";
        String part2 = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"api_secret\"\r\n\r\n"
                + apiSecret + "\r\n";
        String part3Header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"image_file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";

        byte[] p1 = part1.getBytes(StandardCharsets.UTF_8);
        byte[] p2 = part2.getBytes(StandardCharsets.UTF_8);
        byte[] p3h = part3Header.getBytes(StandardCharsets.UTF_8);
        byte[] pe = end.getBytes(StandardCharsets.UTF_8);

        byte[] all = new byte[p1.length + p2.length + p3h.length + fileBytes.length + pe.length];
        int pos = 0;
        System.arraycopy(p1, 0, all, pos, p1.length);
        pos += p1.length;
        System.arraycopy(p2, 0, all, pos, p2.length);
        pos += p2.length;
        System.arraycopy(p3h, 0, all, pos, p3h.length);
        pos += p3h.length;
        System.arraycopy(fileBytes, 0, all, pos, fileBytes.length);
        pos += fileBytes.length;
        System.arraycopy(pe, 0, all, pos, pe.length);
        return all;
    }

    private JsonObject postForm(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return sendJson(request);
    }

    private JsonObject sendJson(HttpRequest request) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                String errorMessage = json.has("error_message") ? json.get("error_message").getAsString() : null;
                boolean concurrencyLimit = isConcurrencyLimit(response.statusCode(), errorMessage);
                if (concurrencyLimit && attempt < MAX_RETRIES) {
                    sleepBackoff(attempt);
                    continue;
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Face++ HTTP " + response.statusCode() + ": " + response.body());
                }
                if (errorMessage != null && !errorMessage.isBlank()) {
                    throw new IllegalStateException("Face++ error: " + errorMessage);
                }
                return json;
            } catch (IllegalStateException e) {
                if (attempt < MAX_RETRIES && isRetryableMessage(e.getMessage())) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new IllegalStateException("Face++ request failed: " + e.getMessage(), e);
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new IllegalStateException("Face++ request failed: " + e.getMessage(), e);
            }
        }
        throw new IllegalStateException("Face++ request failed after retries.");
    }

    private boolean isConcurrencyLimit(int statusCode, String errorMessage) {
        if (statusCode == 429) {
            return true;
        }
        if (statusCode == 403 && errorMessage != null) {
            return errorMessage.toUpperCase().contains("CONCURRENCY_LIMIT_EXCEEDED");
        }
        return false;
    }

    private boolean isRetryableMessage(String message) {
        if (message == null) {
            return false;
        }
        String value = message.toUpperCase();
        return value.contains("CONCURRENCY_LIMIT_EXCEEDED") || value.contains("HTTP 429");
    }

    private void sleepBackoff(int attempt) {
        long delayMs = 450L * attempt;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String requireConfig(String key) {
        String value = readConfig(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configuration manquante: " + key + " (.env/.env.local).");
        }
        return value;
    }

    private String readConfig(String key, String defaultValue) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProperty = System.getProperty(key);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromDotenv = loadDotenv().get(key);
        if (fromDotenv != null && !fromDotenv.isBlank()) {
            return fromDotenv.trim();
        }
        return defaultValue;
    }

    private double readConfigDouble(String key, double defaultValue) {
        String value = readConfig(key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> localCache = dotenvCache;
        if (localCache != null) {
            return localCache;
        }
        synchronized (FacePlusPlusService.class) {
            if (dotenvCache != null) {
                return dotenvCache;
            }
            Map<String, String> values = new HashMap<>();
            loadDotenvFile(values, Path.of(".env"));
            loadDotenvFile(values, Path.of(".env.local"));
            dotenvCache = values;
            return values;
        }
    }

    private static void loadDotenvFile(Map<String, String> values, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Lecture .env impossible: " + path, e);
        }
    }

    private String urlEnc(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
