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
        return suggestNearbyPlacesForNeeds(needs, city, null, null);
    }

    public List<String> suggestNearbyPlacesForNeeds(List<String> needs, String city, Double latitude, Double longitude) {
        if (city == null || city.isBlank() || needs == null || needs.isEmpty()) {
            if ((latitude == null || longitude == null) && (city == null || city.isBlank())) {
                return List.of();
            }
        }

        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String need : needs) {
            merged.addAll(searchBestEffort(need, city, latitude, longitude));
            if (merged.size() >= 5) {
                break;
            }
            merged.addAll(searchBestEffort(need.replace(" near", "").trim(), city, latitude, longitude));
            if (merged.size() >= 5) {
                break;
            }
        }
        if (merged.isEmpty()) {
            return buildFallbackPlaceQueries(needs, city);
        }
        return new ArrayList<>(merged).subList(0, Math.min(merged.size(), 5));
    }

    public String buildDirectionsUrl(String originCity, String destination) {
        return buildDirectionsUrl(null, null, originCity, destination);
    }

    public String buildDirectionsUrl(Double originLatitude, Double originLongitude, String originCity, String destination) {
        if ((originCity == null || originCity.isBlank()) && (originLatitude == null || originLongitude == null) || destination == null || destination.isBlank()) {
            return null;
        }
        String origin = originLatitude != null && originLongitude != null
                ? originLatitude + "," + originLongitude
                : originCity;
        return "https://www.google.com/maps/dir/?api=1"
                + "&origin=" + URLEncoder.encode(origin, StandardCharsets.UTF_8)
                + "&destination=" + URLEncoder.encode(destination, StandardCharsets.UTF_8)
                + "&travelmode=driving";
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

    private List<String> searchBestEffort(String need, String city, Double latitude, Double longitude) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        List<String> queries = city == null || city.isBlank()
                ? List.of(normalizeNeedForSearch(need))
                : List.of(
                need + " near " + city,
                need + " " + city,
                normalizeNeedForSearch(need) + " " + city
        );
        for (String query : queries) {
            if (merged.size() < 5 && latitude != null && longitude != null) {
                merged.addAll(searchWithNominatimNearby(normalizeNeedForSearch(need), latitude, longitude));
            }
            if (merged.size() < 5) {
                merged.addAll(searchWithNominatim(query));
            }
            if (merged.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(merged).subList(0, Math.min(merged.size(), 5));
    }

    private List<String> searchWithNominatim(String query) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&format=jsonv2&limit=5";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "DecideFinance/1.0")
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

    private List<String> searchWithNominatimNearby(String query, double latitude, double longitude) {
        try {
            double delta = 0.08;
            double left = longitude - delta;
            double right = longitude + delta;
            double top = latitude + delta;
            double bottom = latitude - delta;
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&format=jsonv2&limit=5"
                    + "&viewbox=" + left + "," + top + "," + right + "," + bottom
                    + "&bounded=1";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "DecideFinance/1.0")
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

    private String normalizeNeedForSearch(String need) {
        if (need == null) {
            return "service";
        }
        String value = need.toLowerCase();
        if (value.contains("garage")) return "garage";
        if (value.contains("diagnostic auto")) return "garage automobile";
        if (value.contains("centre vidange")) return "vidange voiture";
        if (value.contains("cabinet medical")) return "medecin";
        if (value.contains("laboratoire")) return "laboratoire analyse";
        if (value.contains("clinique")) return "clinique";
        if (value.contains("plombier")) return "plombier";
        if (value.contains("electricien")) return "electricien";
        if (value.contains("maintenance maison")) return "maintenance maison";
        return need;
    }

    private List<String> buildFallbackPlaceQueries(List<String> needs, String city) {
        LinkedHashSet<String> fallback = new LinkedHashSet<>();
        String safeCity = city == null || city.isBlank() ? "near me" : city;
        for (String need : needs) {
            String normalized = normalizeNeedForSearch(need).toLowerCase();
            if (normalized.contains("garage") || normalized.contains("voiture") || normalized.contains("vidange")) {
                fallback.add("Garage entretien auto " + safeCity);
                fallback.add("Centre diagnostic auto " + safeCity);
                fallback.add("Centre pneu " + safeCity);
                fallback.add("Vidange voiture " + safeCity);
                fallback.add("Mecanicien automobile " + safeCity);
            } else if (normalized.contains("medecin") || normalized.contains("laboratoire") || normalized.contains("clinique")) {
                fallback.add("Medecin generaliste " + safeCity);
                fallback.add("Laboratoire analyse " + safeCity);
                fallback.add("Clinique " + safeCity);
                fallback.add("Centre bilan sante " + safeCity);
                fallback.add("Cabinet medical " + safeCity);
            } else if (normalized.contains("plombier") || normalized.contains("electricien") || normalized.contains("maison")) {
                fallback.add("Plombier " + safeCity);
                fallback.add("Electricien " + safeCity);
                fallback.add("Maintenance maison " + safeCity);
                fallback.add("Reparation fuite eau " + safeCity);
                fallback.add("Diagnostic technique maison " + safeCity);
            } else {
                fallback.add(normalizeNeedForSearch(need) + " " + safeCity);
            }
            if (fallback.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(fallback).subList(0, Math.min(fallback.size(), 5));
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
