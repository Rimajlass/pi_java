package pi.savings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.tools.ConfigLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class PdfApiClientService {

    static final String DEFAULT_HTML_TO_PDF_URL = "https://api.html2pdf.app/v1/generate";
    static final String DEFAULT_PDFSHIFT_URL = "https://api.pdfshift.io/v3/convert/pdf";
    private static final String HTML2PDF_PLACEHOLDER = "your_html2pdf_api_key_here";
    private static final String PDFCO_PLACEHOLDER = "your_pdfco_api_key_here";
    private static final String PDFSHIFT_PLACEHOLDER = "your_pdfshift_api_key_here";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiProvider provider;
    private final String apiKey;
    private final String apiUrl;
    private final Duration timeout;
    private final String pdfShiftProcessorVersion;

    public PdfApiClientService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(resolveConnectTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
        this.provider = resolveProvider();
        this.apiKey = resolveApiKey();
        this.apiUrl = resolveApiUrl();
        this.timeout = Duration.ofSeconds(resolveReadTimeoutSeconds());
        this.pdfShiftProcessorVersion = ConfigLoader.get("PDFSHIFT_PROCESSOR_VERSION", "").trim();
    }

    public boolean hasConfiguredApiKey() {
        return hasConfiguredValue(apiKey);
    }

    public byte[] generatePdf(String html, String outputName) throws PdfApiException {
        if (!hasConfiguredApiKey()) {
            throw new PdfApiException(missingConfigurationMessage());
        }
        if (html == null || html.isBlank()) {
            throw new PdfApiException("Le contenu du rapport PDF est vide. Reessayez plus tard.");
        }

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    buildRequest(html, outputName),
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            return extractPdfBytes(response);
        } catch (PdfApiException exception) {
            throw exception;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new PdfApiException(
                    "La generation du PDF a echoue. Verifiez la configuration du service PDF ou reessayez plus tard.",
                    exception
            );
        }
    }

    private HttpRequest buildRequest(String html, String outputName) throws IOException, PdfApiException {
        return switch (provider) {
            case HTML2PDF -> buildHtml2PdfRequest(html, outputName);
            case PDFSHIFT -> buildPdfShiftRequest(html);
            case NONE -> throw new PdfApiException(missingConfigurationMessage());
        };
    }

    private HttpRequest buildHtml2PdfRequest(String html, String outputName) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("html", html);
        payload.put("apiKey", apiKey.trim());
        payload.put("format", "A4");
        payload.put("media", "print");
        payload.put("filename", outputName);
        payload.put("marginTop", 24);
        payload.put("marginBottom", 24);
        payload.put("marginLeft", 16);
        payload.put("marginRight", 16);
        payload.put("waitFor", 2);

        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
    }

    private HttpRequest buildPdfShiftRequest(String html) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", html);
        payload.put("format", "A4");
        payload.put("use_print", true);
        payload.put("wait_for_network", true);
        payload.put("delay", 2000);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("X-API-Key", apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

        if (!pdfShiftProcessorVersion.isBlank()) {
            builder.header("X-Processor-Version", pdfShiftProcessorVersion);
        }
        return builder.build();
    }

    private byte[] extractPdfBytes(HttpResponse<byte[]> response) throws IOException, PdfApiException {
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new PdfApiException(resolveApiErrorMessage(response, contentType));
        }
        if (contentType.contains("application/json")) {
            throw new PdfApiException(resolveApiErrorMessage(response, contentType));
        }
        return response.body();
    }

    private String resolveApiErrorMessage(HttpResponse<byte[]> response, String contentType) throws IOException {
        int statusCode = response.statusCode();
        if (statusCode == 401 || statusCode == 403) {
            return "L'authentification du service PDF a echoue. Verifiez la cle API configuree.";
        }
        if (statusCode == 408 || statusCode == 429) {
            return "Le service PDF est temporairement indisponible. Reessayez dans quelques instants.";
        }
        if (contentType.contains("application/json")) {
            JsonNode root = objectMapper.readTree(response.body());
            String message = root.path("message").asText("");
            if (message.isBlank()) {
                message = root.path("error").asText("");
            }
            if (!message.isBlank()) {
                return message;
            }
        }
        return "La generation du PDF a echoue. Verifiez la configuration du service PDF ou reessayez plus tard.";
    }

    private ApiProvider resolveProvider() {
        if (hasConfiguredValue(ConfigLoader.get("HTML2PDF_API_KEY", ""))) {
            return ApiProvider.HTML2PDF;
        }
        if (hasConfiguredValue(ConfigLoader.get("PDFSHIFT_API_KEY", ""))) {
            return ApiProvider.PDFSHIFT;
        }
        if (hasConfiguredValue(ConfigLoader.get("PDFCO_API_KEY", ""))) {
            return ApiProvider.HTML2PDF;
        }
        return ApiProvider.NONE;
    }

    private String resolveApiKey() {
        return switch (provider) {
            case HTML2PDF -> {
                String html2PdfKey = ConfigLoader.get("HTML2PDF_API_KEY", "");
                if (html2PdfKey != null && !html2PdfKey.isBlank()) {
                    yield html2PdfKey;
                }
                yield ConfigLoader.get("PDFCO_API_KEY", "");
            }
            case PDFSHIFT -> ConfigLoader.get("PDFSHIFT_API_KEY", "");
            case NONE -> "";
        };
    }

    private String resolveApiUrl() {
        return switch (provider) {
            case HTML2PDF -> {
                String configuredUrl = ConfigLoader.get("HTML2PDF_API_URL", "");
                if (configuredUrl != null && !configuredUrl.isBlank()) {
                    yield configuredUrl;
                }
                yield ConfigLoader.get("PDFCO_HTML_TO_PDF_URL", DEFAULT_HTML_TO_PDF_URL);
            }
            case PDFSHIFT -> {
                String configuredUrl = ConfigLoader.get("PDFSHIFT_API_URL", "");
                yield configuredUrl != null && !configuredUrl.isBlank() ? configuredUrl : DEFAULT_PDFSHIFT_URL;
            }
            case NONE -> DEFAULT_HTML_TO_PDF_URL;
        };
    }

    private int resolveConnectTimeoutSeconds() {
        String configured = ConfigLoader.get("HTML2PDF_CONNECT_TIMEOUT_SECONDS", "");
        if (configured != null && !configured.isBlank()) {
            return parsePositiveInt(configured, 12);
        }
        return ConfigLoader.getInt("PDFCO_CONNECT_TIMEOUT_SECONDS", 12);
    }

    private int resolveReadTimeoutSeconds() {
        String configured = ConfigLoader.get("HTML2PDF_READ_TIMEOUT_SECONDS", "");
        if (configured != null && !configured.isBlank()) {
            return parsePositiveInt(configured, 45);
        }
        return ConfigLoader.getInt("PDFCO_READ_TIMEOUT_SECONDS", 45);
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean hasConfiguredValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim();
        return !normalized.equalsIgnoreCase(HTML2PDF_PLACEHOLDER)
                && !normalized.equalsIgnoreCase(PDFCO_PLACEHOLDER)
                && !normalized.equalsIgnoreCase(PDFSHIFT_PLACEHOLDER)
                && !normalized.startsWith("your_");
    }

    private String missingConfigurationMessage() {
        return "Le service Smart PDF n'est pas configure. Ajoutez HTML2PDF_API_KEY ou PDFSHIFT_API_KEY dans config.properties, .env ou vos variables d'environnement.";
    }

    private enum ApiProvider {
        HTML2PDF,
        PDFSHIFT,
        NONE
    }

    public static class PdfApiException extends Exception {
        public PdfApiException(String message) {
            super(message);
        }

        public PdfApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
