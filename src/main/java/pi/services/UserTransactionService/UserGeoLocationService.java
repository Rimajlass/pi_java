package pi.services.UserTransactionService;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class UserGeoLocationService {

    private static final String GEO_API_URL = "https://ipwho.is/";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;

    public UserGeoLocationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
    }

    public Optional<GeoLocationSnapshot> resolveCurrentLocation() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(GEO_API_URL))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!readBoolean(body, "success", false)) {
                return Optional.empty();
            }

            String ip = readString(body, "ip");
            String countryCode = readString(body, "country_code");
            String countryName = readString(body, "country");
            String regionName = readString(body, "region");
            String cityName = readString(body, "city");

            return Optional.of(new GeoLocationSnapshot(
                    toNullIfBlank(ip),
                    toNullIfBlank(countryCode),
                    toNullIfBlank(countryName),
                    toNullIfBlank(regionName),
                    toNullIfBlank(cityName),
                    false
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String readString(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) {
            return null;
        }
        return body.get(key).getAsString();
    }

    private boolean readBoolean(JsonObject body, String key, boolean fallback) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return body.get(key).getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }

    private String toNullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static final class GeoLocationSnapshot {
        private final String ip;
        private final String countryCode;
        private final String countryName;
        private final String regionName;
        private final String cityName;
        private final boolean vpnSuspected;

        public GeoLocationSnapshot(
                String ip,
                String countryCode,
                String countryName,
                String regionName,
                String cityName,
                boolean vpnSuspected
        ) {
            this.ip = ip;
            this.countryCode = countryCode;
            this.countryName = countryName;
            this.regionName = regionName;
            this.cityName = cityName;
            this.vpnSuspected = vpnSuspected;
        }

        public String getIp() {
            return ip;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getCountryName() {
            return countryName;
        }

        public String getRegionName() {
            return regionName;
        }

        public String getCityName() {
            return cityName;
        }

        public boolean isVpnSuspected() {
            return vpnSuspected;
        }
    }
}
