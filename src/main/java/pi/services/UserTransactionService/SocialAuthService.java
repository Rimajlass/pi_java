package pi.services.UserTransactionService;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import pi.entities.User;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SocialAuthService {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);
    private static final long CALLBACK_WAIT_SECONDS = 150;
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String FACEBOOK_AUTH_URL = "https://www.facebook.com/dialog/oauth";
    private static final String FACEBOOK_TOKEN_URL = "https://graph.facebook.com/v20.0/oauth/access_token";
    private static final String FACEBOOK_USERINFO_URL = "https://graph.facebook.com/me?fields=id,name,email";
    private static final String GOOGLE_CLIENT_ID_KEY = "GOOGLE_OAUTH_CLIENT_ID";
    private static final String GOOGLE_CLIENT_SECRET_KEY = "GOOGLE_OAUTH_CLIENT_SECRET";
    private static final String GOOGLE_REDIRECT_URI_KEY = "GOOGLE_REDIRECT_URI";
    private static final String FACEBOOK_CLIENT_ID_KEY = "FACEBOOK_OAUTH_CLIENT_ID";
    private static final String FACEBOOK_CLIENT_SECRET_KEY = "FACEBOOK_OAUTH_CLIENT_SECRET";
    private static final String FACEBOOK_REDIRECT_URI_KEY = "FACEBOOK_REDIRECT_URI";
    private static final String SUCCESS_HTML = """
            <html><body style="font-family:Arial,sans-serif;padding:24px;">
            <h2>Connexion reussie</h2><p>Vous pouvez fermer cet onglet et revenir dans l'application.</p>
            </body></html>
            """;
    private static final String FAILURE_HTML = """
            <html><body style="font-family:Arial,sans-serif;padding:24px;">
            <h2>Connexion echouee</h2><p>Fermez cet onglet puis reessayez depuis l'application.</p>
            </body></html>
            """;

    private static volatile Map<String, String> dotenvCache;

    private final HttpClient httpClient;
    private final UserService userService;

    public SocialAuthService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        this.userService = new UserService();
    }

    public void validateStartupConfiguration() {
        List<String> missingKeys = new ArrayList<>();
        collectMissingConfig(GOOGLE_CLIENT_ID_KEY, missingKeys);
        collectMissingConfig(GOOGLE_CLIENT_SECRET_KEY, missingKeys);
        collectMissingConfig(GOOGLE_REDIRECT_URI_KEY, missingKeys);
        collectMissingConfig(FACEBOOK_CLIENT_ID_KEY, missingKeys);
        collectMissingConfig(FACEBOOK_CLIENT_SECRET_KEY, missingKeys);
        collectMissingConfig(FACEBOOK_REDIRECT_URI_KEY, missingKeys);

        if (!missingKeys.isEmpty()) {
            throw new IllegalStateException("Variables OAuth manquantes: " + String.join(", ", missingKeys));
        }

        validateRedirectUri(requireConfig(GOOGLE_REDIRECT_URI_KEY), GOOGLE_REDIRECT_URI_KEY);
        validateRedirectUri(requireConfig(FACEBOOK_REDIRECT_URI_KEY), FACEBOOK_REDIRECT_URI_KEY);
    }

    public boolean isGoogleConfigured() {
        return isProviderConfigured(GOOGLE_CLIENT_ID_KEY, GOOGLE_CLIENT_SECRET_KEY, GOOGLE_REDIRECT_URI_KEY);
    }

    public boolean isFacebookConfigured() {
        return isProviderConfigured(FACEBOOK_CLIENT_ID_KEY, FACEBOOK_CLIENT_SECRET_KEY, FACEBOOK_REDIRECT_URI_KEY);
    }

    public User authenticateWithGoogle() {
        String clientId = requireConfig(GOOGLE_CLIENT_ID_KEY);
        String clientSecret = requireConfig(GOOGLE_CLIENT_SECRET_KEY);
        String redirectUri = requireConfig(GOOGLE_REDIRECT_URI_KEY);
        String state = UUID.randomUUID().toString();

        String authUrl = GOOGLE_AUTH_URL
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc("openid email profile")
                + "&state=" + enc(state)
                + "&prompt=select_account";

        CallbackPayload callbackPayload = waitForAuthorizationCode(authUrl, state, redirectUri, "google");

        String tokenBody = "code=" + enc(callbackPayload.code())
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&redirect_uri=" + enc(redirectUri)
                + "&grant_type=authorization_code";

        JsonObject tokenJson = postForm(GOOGLE_TOKEN_URL, tokenBody);
        String accessToken = readRequiredString(tokenJson, "access_token", "Token Google introuvable.");

        HttpRequest userInfoRequest = HttpRequest.newBuilder(URI.create(GOOGLE_USERINFO_URL))
                .timeout(HTTP_TIMEOUT)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        JsonObject profile = sendJson(userInfoRequest);
        String email = readRequiredString(profile, "email", "Google n'a pas retourne d'email.");
        String name = readOptionalString(profile, "name");

        return userService.findOrCreateSocialUser(email, name, "google");
    }

    public User authenticateWithFacebook() {
        String clientId = requireConfig(FACEBOOK_CLIENT_ID_KEY);
        String clientSecret = requireConfig(FACEBOOK_CLIENT_SECRET_KEY);
        String redirectUri = requireConfig(FACEBOOK_REDIRECT_URI_KEY);
        String state = UUID.randomUUID().toString();

        String authUrl = FACEBOOK_AUTH_URL
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc("public_profile")
                + "&state=" + enc(state);

        CallbackPayload callbackPayload = waitForAuthorizationCode(authUrl, state, redirectUri, "facebook");

        String tokenUrl = FACEBOOK_TOKEN_URL
                + "?client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&redirect_uri=" + enc(redirectUri)
                + "&code=" + enc(callbackPayload.code());

        JsonObject tokenJson = sendJson(HttpRequest.newBuilder(URI.create(tokenUrl)).timeout(HTTP_TIMEOUT).GET().build());
        String accessToken = readRequiredString(tokenJson, "access_token", "Token Facebook introuvable.");

        String profileUrl = FACEBOOK_USERINFO_URL + "&access_token=" + enc(accessToken);
        JsonObject profile = sendJson(HttpRequest.newBuilder(URI.create(profileUrl)).timeout(HTTP_TIMEOUT).GET().build());
        String email = readOptionalString(profile, "email");
        if (email == null || email.isBlank()) {
            String facebookId = readRequiredString(profile, "id", "Facebook n'a retourne ni email ni id.");
            email = facebookId + "@facebook.local";
        }
        String name = readOptionalString(profile, "name");

        return userService.findOrCreateSocialUser(email, name, "facebook");
    }

    private CallbackPayload waitForAuthorizationCode(String authUrl, String expectedState, String redirectUri, String provider) {
        CompletableFuture<CallbackPayload> callbackFuture = new CompletableFuture<>();
        HttpServer server = null;
        URI callbackUri = validateRedirectUri(redirectUri, provider + "_REDIRECT_URI");

        try {
            server = HttpServer.create(new InetSocketAddress(callbackUri.getHost(), callbackUri.getPort()), 0);
            HttpServer finalServer = server;
            server.createContext(callbackUri.getPath(), exchange ->
                    handleCallback(exchange, expectedState, callbackFuture, finalServer));
            server.start();

            System.out.println("OAuth URL (" + provider + "): " + authUrl);
            openBrowser(authUrl);
            return callbackFuture.get(CALLBACK_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Connexion " + provider + " impossible: " + e.getMessage(), e);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    private void handleCallback(
            HttpExchange exchange,
            String expectedState,
            CompletableFuture<CallbackPayload> callbackFuture,
            HttpServer server
    ) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String code = query.get("code");
        String state = query.get("state");
        String error = query.get("error");
        boolean success = code != null && state != null && state.equals(expectedState) && error == null;

        if (success) {
            callbackFuture.complete(new CallbackPayload(code, state));
            respondHtml(exchange, 200, SUCCESS_HTML);
        } else {
            callbackFuture.completeExceptionally(new IllegalStateException(
                    error != null ? "Autorisation refusee: " + error : "Callback OAuth invalide."
            ));
            respondHtml(exchange, 400, FAILURE_HTML);
        }
        server.stop(0);
    }

    private void respondHtml(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private void openBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalStateException("Desktop browse non supporte.");
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'ouvrir le navigateur pour OAuth.", e);
        }
    }

    private JsonObject postForm(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return sendJson(request);
    }

    private JsonObject sendJson(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Erreur HTTP OAuth " + response.statusCode() + ": " + response.body());
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("Echec appel OAuth distant: " + e.getMessage(), e);
        }
    }

    private String requireConfig(String key) {
        String value = readConfig(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configuration manquante: " + key + " (env systeme ou .env/.env.local).");
        }
        return value;
    }

    private void collectMissingConfig(String key, List<String> missingKeys) {
        String value = readConfig(key);
        if (value == null || value.isBlank()) {
            missingKeys.add(key);
        }
    }

    private boolean isProviderConfigured(String clientIdKey, String clientSecretKey, String redirectUriKey) {
        String clientId = readConfig(clientIdKey);
        String clientSecret = readConfig(clientSecretKey);
        String redirectUri = readConfig(redirectUriKey);
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(redirectUri)) {
            return false;
        }
        try {
            validateRedirectUri(redirectUri, redirectUriKey);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private URI validateRedirectUri(String value, String key) {
        try {
            URI uri = URI.create(value);
            if (!"http".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalStateException("La variable " + key + " doit utiliser http://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalStateException("La variable " + key + " doit contenir un host valide.");
            }
            if (uri.getPort() <= 0) {
                throw new IllegalStateException("La variable " + key + " doit contenir un port explicite.");
            }
            if (uri.getPath() == null || uri.getPath().isBlank() || "/".equals(uri.getPath())) {
                throw new IllegalStateException("La variable " + key + " doit contenir un path de callback.");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("La variable " + key + " n'est pas une URI valide.", e);
        }
    }

    private String readConfig(String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProperty = System.getProperty(key);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        return loadDotenv().get(key);
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> localCache = dotenvCache;
        if (localCache != null) {
            return localCache;
        }
        synchronized (SocialAuthService.class) {
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
            throw new IllegalStateException("Lecture du fichier " + path + " impossible.", e);
        }
    }

    private String readRequiredString(JsonObject json, String key, String message) {
        String value = readOptionalString(json, key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String readOptionalString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }
        String[] params = rawQuery.split("&");
        for (String part : params) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
                values.put(key, value);
            }
        }
        return values;
    }

    private record CallbackPayload(String code, String state) {
    }
}
