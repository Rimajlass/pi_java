package pi.services.ImprevusCasreelService;

import pi.tools.AppEnv;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationSuggestionService {

    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"");
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public List<String> suggestNearbyPlaces(String category, String city) {
        String apiKey = AppEnv.get("LOCATIONIQ_API_KEY");
        if (apiKey == null || apiKey.isBlank() || city == null || city.isBlank()) {
            return List.of();
        }

        String query = switch (category) {
            case "Sante" -> "doctor clinic near " + city;
            case "Voiture" -> "car garage maintenance near " + city;
            case "Maison" -> "home repair plumber electrician near " + city;
            default -> "service near " + city;
        };

        try {
            String url = "https://us1.locationiq.com/v1/search?key="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&format=json&limit=5";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractDisplayNames(response.body());
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    public List<String> suggestNearbyPlacesForNeeds(List<String> needs, String city) {
        String apiKey = AppEnv.get("LOCATIONIQ_API_KEY");
        if (apiKey == null || apiKey.isBlank() || city == null || city.isBlank() || needs == null || needs.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String need : needs) {
            merged.addAll(search(need + " near " + city, apiKey));
            if (merged.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(merged).subList(0, Math.min(merged.size(), 5));
    }

    private List<String> search(String query, String apiKey) {
        try {
            String url = "https://us1.locationiq.com/v1/search?key="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&format=json&limit=5";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractDisplayNames(response.body());
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private List<String> extractDisplayNames(String body) {
        Matcher matcher = DISPLAY_NAME_PATTERN.matcher(body == null ? "" : body);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        while (matcher.find() && values.size() < 5) {
            values.add(matcher.group(1).replace("\\/", "/"));
        }
        return new ArrayList<>(values);
    }
}
