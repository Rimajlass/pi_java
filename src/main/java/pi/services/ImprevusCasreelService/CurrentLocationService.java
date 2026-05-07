package pi.services.ImprevusCasreelService;

import javafx.application.Platform;
import javafx.stage.Window;
import pi.tools.AppEnv;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.URI;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrentLocationService {

    public record CurrentLocation(String city, Double latitude, Double longitude, String source) {
        public boolean hasCoordinates() {
            return latitude != null && longitude != null;
        }
    }

    private static final Pattern CITY_PATTERN = Pattern.compile("\"city\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern TOWN_PATTERN = Pattern.compile("\"town\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern COUNTY_PATTERN = Pattern.compile("\"county\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern STATE_DISTRICT_PATTERN = Pattern.compile("\"state_district\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern SUBURB_PATTERN = Pattern.compile("\"suburb\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern BOROUGH_PATTERN = Pattern.compile("\"borough\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NEIGHBOURHOOD_PATTERN = Pattern.compile("\"neighbourhood\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern VILLAGE_PATTERN = Pattern.compile("\"village\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern LAT_PATTERN = Pattern.compile("\"latitude\"\\s*:\\s*([\\-\\d\\.]+)|\"lat\"\\s*:\\s*\"?([\\-\\d\\.]+)\"?");
    private static final Pattern LON_PATTERN = Pattern.compile("\"longitude\"\\s*:\\s*([\\-\\d\\.]+)|\"lon\"\\s*:\\s*\"?([\\-\\d\\.]+)\"?");
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public CompletableFuture<CurrentLocation> detectByIpAsync() {
        return CompletableFuture.supplyAsync(this::detectByIp);
    }

    public CompletableFuture<CurrentLocation> detectWindowsLiveLocationAsync() {
        return CompletableFuture.supplyAsync(this::detectWindowsLiveLocation);
    }

    public CompletableFuture<CurrentLocation> detectWithBrowserGpsOrIpAsync(Window owner) {
        CompletableFuture<CurrentLocation> result = new CompletableFuture<>();
        detectWindowsLiveLocationAsync().whenComplete((windowsLocation, windowsError) -> {
            if (windowsError == null && windowsLocation != null && windowsLocation.hasCoordinates()) {
                result.complete(windowsLocation);
                return;
            }
            openExternalBrowserGpsPrompt(result);
        });
        return result.exceptionally(ignored -> detectByIp());
    }

    private void openExternalBrowserGpsPrompt(CompletableFuture<CurrentLocation> result) {
        CompletableFuture.runAsync(() -> {
            HttpServer server = null;
            try {
                int port = findFreePort();
                server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", port), 0);
                server.setExecutor(Executors.newCachedThreadPool());
                HttpServer finalServer = server;
                server.createContext("/", exchange -> {
                    String html = """
                            <html>
                            <head><meta charset="utf-8"><title>Use current location</title></head>
                            <body style="font-family:Segoe UI,sans-serif;padding:24px;color:#213453;">
                              <h3>Allow location access</h3>
                              <p>This page requests your live location for nearby place suggestions.</p>
                              <p id="status">Waiting for permission...</p>
                              <script>
                                if(!navigator.geolocation){
                                  document.getElementById('status').textContent='Geolocation unavailable. Returning to app...';
                                  window.location.href='/error?reason=unsupported';
                                } else {
                                  navigator.geolocation.getCurrentPosition(function(pos){
                                    document.getElementById('status').textContent='Location captured. You can return to the app.';
                                    window.location.href='/location?lat=' + encodeURIComponent(pos.coords.latitude) + '&lon=' + encodeURIComponent(pos.coords.longitude);
                                  }, function(err){
                                    document.getElementById('status').textContent='Permission denied or unavailable. Returning to app...';
                                    window.location.href='/error?reason=' + encodeURIComponent(err && err.message ? err.message : 'denied');
                                  }, { enableHighAccuracy:true, timeout:12000, maximumAge:0 });
                                }
                              </script>
                            </body>
                            </html>
                            """;
                    byte[] payload = html.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, payload.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(payload);
                    }
                });
                server.createContext("/location", exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    double lat = parseQueryDouble(query, "lat");
                    double lon = parseQueryDouble(query, "lon");
                    String html = "<html><body style='font-family:Segoe UI,sans-serif;padding:24px;'>Location sent to the app. You can close this tab and return.</body></html>";
                    byte[] payload = html.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, payload.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(payload);
                    }
                    if (!result.isDone()) {
                        CurrentLocation resolved = reverseGeocode(lat, lon);
                        result.complete(new CurrentLocation(resolved.city(), lat, lon, "Browser GPS"));
                    }
                    finalServer.stop(0);
                });
                server.createContext("/error", exchange -> {
                    String html = "<html><body style='font-family:Segoe UI,sans-serif;padding:24px;'>Location could not be sent to the app. Falling back automatically.</body></html>";
                    byte[] payload = html.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, payload.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(payload);
                    }
                    if (!result.isDone()) {
                        result.complete(detectByIp());
                    }
                    finalServer.stop(0);
                });
                server.start();
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create("http://127.0.0.1:" + port + "/"));
                } else if (!result.isDone()) {
                    result.complete(detectByIp());
                    finalServer.stop(0);
                    return;
                }
                Thread.sleep(15000);
                if (!result.isDone()) {
                    result.complete(detectByIp());
                    finalServer.stop(0);
                }
            } catch (Exception ignored) {
                if (!result.isDone()) {
                    result.complete(detectByIp());
                }
                if (server != null) {
                    server.stop(0);
                }
            }
        });
    }

    private CurrentLocation detectByIp() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipwho.is/"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                String city = extractBestCity(body, resolveDefaultCity());
                Double lat = extractDouble(body, LAT_PATTERN);
                Double lon = extractDouble(body, LON_PATTERN);
                return new CurrentLocation(city, lat, lon, "IP location");
            }
        } catch (Exception ignored) {
        }
        return defaultLocation(resolveDefaultCity(), "Default city");
    }

    private CurrentLocation detectWindowsLiveLocation() {
        if (!isWindows()) {
            return defaultLocation(resolveDefaultCity(), "Windows live location unavailable");
        }
        String command = "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.Device; "
                + "$geo = New-Object System.Device.Location.GeoCoordinateWatcher; "
                + "$started = $geo.TryStart($false, [TimeSpan]::FromSeconds(10)); "
                + "if($started -and !$geo.Position.Location.IsUnknown){ "
                + "Write-Output ($geo.Position.Location.Latitude.ToString([System.Globalization.CultureInfo]::InvariantCulture) + ',' + $geo.Position.Location.Longitude.ToString([System.Globalization.CultureInfo]::InvariantCulture)) "
                + "} else { Write-Output 'UNAVAILABLE' }";
        try {
            Process process = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-Command", command
            ).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String output = reader.readLine();
                process.waitFor();
                if (output != null && output.contains(",")) {
                    String[] parts = output.trim().split(",");
                    if (parts.length == 2) {
                        double latitude = Double.parseDouble(parts[0]);
                        double longitude = Double.parseDouble(parts[1]);
                        CurrentLocation resolved = reverseGeocode(latitude, longitude);
                        return new CurrentLocation(resolved.city(), latitude, longitude, "Windows live location");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return defaultLocation(resolveDefaultCity(), "Windows live location unavailable");
    }

    public CurrentLocation reverseGeocode(double latitude, double longitude) {
        String apiKey = AppEnv.get("LOCATIONIQ_API_KEY");
        try {
            String url;
            if (apiKey != null && !apiKey.isBlank()) {
                url = "https://us1.locationiq.com/v1/reverse?key="
                        + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                        + "&lat=" + latitude
                        + "&lon=" + longitude
                        + "&format=json";
            } else {
                url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + latitude + "&lon=" + longitude;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "DecideFinance/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String city = extractBestCity(response.body(), resolveDefaultCity());
                return new CurrentLocation(city, latitude, longitude, "Browser GPS");
            }
        } catch (Exception ignored) {
        }
        return new CurrentLocation(resolveDefaultCity(), latitude, longitude, "Browser GPS");
    }

    private CurrentLocation defaultLocation(String city, String source) {
        return new CurrentLocation(city, null, null, source);
    }

    private String resolveDefaultCity() {
        String configured = AppEnv.get("DEFAULT_SUGGESTION_CITY");
        return configured == null || configured.isBlank() ? "Tunis" : configured.trim();
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }

    private String extract(String body, Pattern pattern, String fallback) {
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private String extractBestCity(String body, String fallback) {
        String normalizedFallback = normalizeDetectedCity(fallback);
        String suburb = extract(body, SUBURB_PATTERN, null);
        String neighbourhood = extract(body, NEIGHBOURHOOD_PATTERN, null);
        String borough = extract(body, BOROUGH_PATTERN, null);
        String city = extract(body, CITY_PATTERN, null);
        String town = extract(body, TOWN_PATTERN, null);
        String village = extract(body, VILLAGE_PATTERN, null);
        String county = extract(body, COUNTY_PATTERN, null);
        String stateDistrict = extract(body, STATE_DISTRICT_PATTERN, null);
        String displayName = extract(body, DISPLAY_NAME_PATTERN, null);

        String specialCase = normalizeGhazelaLocation(suburb, neighbourhood, borough, city, town, village, county, stateDistrict, displayName);
        if (specialCase != null) {
            return specialCase;
        }

        for (String candidate : new String[]{city, town, village, suburb, neighbourhood, borough, county, stateDistrict}) {
            String normalized = normalizeDetectedCity(candidate);
            if (normalized != null) {
                return normalized;
            }
        }

        String inferredFromDisplay = normalizeDetectedCity(extractFromDisplayName(displayName));
        return inferredFromDisplay != null ? inferredFromDisplay : normalizedFallback;
    }

    private String normalizeGhazelaLocation(String... values) {
        boolean hasGhazela = false;
        boolean hasAriana = false;
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeForComparison(value);
            if (normalized == null) {
                continue;
            }
            if (normalized.contains("ghazela") || normalized.contains("el ghazala")) {
                hasGhazela = true;
            }
            if (normalized.contains("ariana")) {
                hasAriana = true;
            }
            if (normalized.contains("technopole")) {
                hasGhazela = true;
                hasAriana = true;
            }
        }
        return hasGhazela && hasAriana ? "Ariana Technopole Ghazela" : null;
    }

    private String extractFromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        String[] parts = displayName.split(",");
        for (String part : parts) {
            String normalized = normalizeDetectedCity(part);
            if (normalized != null) {
                return normalized;
            }
        }
        return displayName.trim();
    }

    private String normalizeDetectedCity(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        String normalized = normalizeForComparison(trimmed);
        if (normalized == null) {
            return trimmed;
        }
        if (normalized.contains("ghazela") || normalized.contains("el ghazala")) {
            return "Ariana Technopole Ghazela";
        }
        if (normalized.contains("technopole") && normalized.contains("ariana")) {
            return "Ariana Technopole Ghazela";
        }
        return trimmed;
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return null;
        }
        return value
                .toLowerCase()
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replace('â', 'a')
                .replace('î', 'i')
                .replace('ï', 'i')
                .replace('ô', 'o')
                .replace('û', 'u')
                .replace('ù', 'u')
                .trim();
    }

    private Double extractDouble(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        if (matcher.find()) {
            String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (value != null && !value.isBlank()) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private double parseQueryDouble(String query, String key) {
        if (query == null || key == null) {
            return 0;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) {
                try {
                    return Double.parseDouble(java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
