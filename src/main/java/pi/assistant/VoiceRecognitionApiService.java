package pi.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.tools.ConfigLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

public class VoiceRecognitionApiService {

    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/audio/transcriptions";
    private static final String DEFAULT_MODEL = "whisper-1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final String language;
    private final Duration timeout;
    private final String provider;
    private final String googleCredentials;

    public VoiceRecognitionApiService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
        this.provider = ConfigLoader.get("SPEECH_TO_TEXT_PROVIDER", "openai").trim().toLowerCase(Locale.ROOT);
        this.endpoint = ConfigLoader.get("OPENAI_STT_URL", DEFAULT_ENDPOINT);
        this.apiKey = resolveApiKey();
        this.googleCredentials = ConfigLoader.get("GOOGLE_APPLICATION_CREDENTIALS", "").trim();
        this.model = ConfigLoader.get("OPENAI_STT_MODEL", DEFAULT_MODEL);
        this.language = ConfigLoader.get("OPENAI_STT_LANGUAGE", "");
        this.timeout = Duration.ofSeconds(ConfigLoader.getInt("OPENAI_STT_TIMEOUT_SECONDS", 45));
    }

    public boolean isConfigured() {
        if ("google".equals(provider)) {
            return hasText(googleCredentials);
        }
        return hasText(apiKey);
    }

    public boolean isGoogleProviderSelected() {
        return "google".equals(provider);
    }

    public String transcribeAudio(Path audioPath) throws VoiceRecognitionException {
        if (!isConfigured()) {
            throw new VoiceRecognitionException(
                    "Voice service is not configured. Please add your Speech-to-Text API key or type your command."
            );
        }

        if ("google".equals(provider)) {
            System.err.println("[SmartVoiceAgent] Google provider selected but Google STT transport is not yet implemented.");
            throw new VoiceRecognitionException(
                    "Voice service is not configured. Please add your Speech-to-Text API key or type your command."
            );
        }

        try {
            if (!Files.exists(audioPath) || Files.size(audioPath) < 1024) {
                throw new VoiceRecognitionException("Recorded audio is empty. Check microphone input.");
            }
        } catch (IOException exception) {
            throw new VoiceRecognitionException("Unable to read recorded audio.", exception);
        }

        VoiceRecognitionException firstFailure = null;
        for (String candidateModel : candidateModels()) {
            try {
                return transcribeWithModel(audioPath, candidateModel);
            } catch (VoiceRecognitionException exception) {
                if (firstFailure == null) {
                    firstFailure = exception;
                }
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
        throw new VoiceRecognitionException("Speech-to-Text request failed.");
    }

    private String transcribeWithModel(Path audioPath, String selectedModel) throws VoiceRecognitionException {
        try {
            String boundary = "----SmartVoiceBoundary" + System.currentTimeMillis();
            byte[] payload = buildMultipartPayload(boundary, audioPath, selectedModel);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new VoiceRecognitionException(extractErrorMessage(response.body(), response.statusCode()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.path("text");
            String text = textNode.isMissingNode() ? "" : textNode.asText("");
            if (text == null || text.isBlank()) {
                throw new VoiceRecognitionException("Speech-to-Text API returned empty text.");
            }
            return text.trim();
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new VoiceRecognitionException("Speech-to-Text request failed.", exception);
        }
    }

    private byte[] buildMultipartPayload(String boundary, Path audioPath, String selectedModel) throws IOException {
        byte[] audioBytes = Files.readAllBytes(audioPath);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writeField(output, boundary, "model", selectedModel);
        if (language != null && !language.isBlank()) {
            writeField(output, boundary, "language", language);
        }
        writeField(output, boundary, "response_format", "json");
        writeFile(output, boundary, "file", audioPath.getFileName().toString(), "audio/wav", audioBytes);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private List<String> candidateModels() {
        if ("whisper-1".equalsIgnoreCase(model)) {
            return List.of("whisper-1", "gpt-4o-mini-transcribe");
        }
        if ("gpt-4o-mini-transcribe".equalsIgnoreCase(model)) {
            return List.of("gpt-4o-mini-transcribe", "whisper-1");
        }
        return List.of(model, "whisper-1");
    }

    private String resolveApiKey() {
        List<String> candidates = List.of(
                ConfigLoader.get("OPENAI_API_KEY", ""),
                ConfigLoader.get("OPENAI_KEY", ""),
                ConfigLoader.get("SPEECH_TO_TEXT_API_KEY", ""),
                ConfigLoader.get("OPENAI_STT_API_KEY", "")
        );
        for (String candidate : candidates) {
            if (isUsableApiKey(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    private boolean isUsableApiKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim();
        return !normalized.equalsIgnoreCase("sk-your-real-key-here")
                && !normalized.startsWith("sk-your-");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void writeField(ByteArrayOutputStream output, String boundary, String fieldName, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFile(
            ByteArrayOutputStream output,
            String boundary,
            String fieldName,
            String filename,
            String contentType,
            byte[] bytes
    ) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String extractErrorMessage(String body, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messageNode = root.path("error").path("message");
            if (!messageNode.isMissingNode() && !messageNode.asText().isBlank()) {
                return "Speech-to-Text API error (" + statusCode + "): " + messageNode.asText();
            }
        } catch (IOException ignored) {
        }
        return "Speech-to-Text API error (" + statusCode + ").";
    }

    public static class VoiceRecognitionException extends Exception {
        public VoiceRecognitionException(String message) {
            super(message);
        }

        public VoiceRecognitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
