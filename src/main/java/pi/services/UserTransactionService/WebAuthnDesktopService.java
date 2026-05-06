package pi.services.UserTransactionService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WebAuthnDesktopService {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(300);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile Map<String, String> dotenvCache;
    private static final String DEFAULT_RP_NAME = "Decide Finance";

    private final String publicBaseUrl;
    private final String rpId;
    private final String origin;
    private final String rpName;

    public WebAuthnDesktopService() {
        this.publicBaseUrl = normalizePublicBaseUrl(readConfig("WEBAUTHN_PUBLIC_BASE_URL", null));
        this.rpId = resolveRpId(readConfig("WEBAUTHN_RP_ID", null), publicBaseUrl);
        this.origin = resolveOrigin(readConfig("WEBAUTHN_ORIGIN", null), publicBaseUrl);
        this.rpName = readConfig("WEBAUTHN_RP_NAME", DEFAULT_RP_NAME);
    }

    public PendingPasskeyFlow startEnrollFlow(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email required for passkey enrollment.");
        }
        byte[] challenge = randomBytes(32);
        byte[] userId = email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return startFlow(
                "enroll",
                renderEnrollPage(base64Url(challenge), base64Url(userId), email.trim().toLowerCase(), rpId, origin, rpName)
        );
    }

    public PendingPasskeyFlow startLoginFlow(String email, String credentialId) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email required for biometric login.");
        }
        if (credentialId == null || credentialId.isBlank()) {
            throw new IllegalStateException("No passkey registered for this account.");
        }
        byte[] challenge = randomBytes(32);
        return startFlow(
                "login",
                renderLoginPage(base64Url(challenge), credentialId, origin)
        );
    }

    public String enrollCredential(String email) {
        PendingPasskeyFlow flow = startEnrollFlow(email);
        try {
            return flow.awaitCredential(WAIT_TIMEOUT);
        } finally {
            flow.close();
        }
    }

    public String authenticateCredential(String email, String credentialId) {
        PendingPasskeyFlow flow = startLoginFlow(email, credentialId);
        try {
            return flow.awaitCredential(WAIT_TIMEOUT);
        } finally {
            flow.close();
        }
    }

    private PendingPasskeyFlow startFlow(String mode, String htmlPage) {
        HttpServer server;
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            int bindPort = readConfigInt("WEBAUTHN_BIND_PORT", 0);
            server = HttpServer.create(new InetSocketAddress(bindPort), 0);
            HttpServer finalServer = server;
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if ("/complete".equals(path)) {
                    handleComplete(exchange, future, finalServer);
                    return;
                }
                respondHtml(exchange, 200, htmlPage);
            });
            server.start();

            int port = server.getAddress().getPort();
            String localUrl = "http://127.0.0.1:" + port + "/";
            String qrUrl = buildQrUrl(port);
            System.out.println("WebAuthn URL (" + mode + "): " + localUrl);
            System.out.println("WebAuthn QR URL (" + mode + "): " + qrUrl);
            return new PendingPasskeyFlow(server, future, localUrl, qrUrl);
        } catch (Exception e) {
            throw new IllegalStateException("WebAuthn " + mode + " failed: " + e.getMessage(), e);
        }
    }

    private String buildQrUrl(int port) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String value = publicBaseUrl;
            if (value.contains("{port}")) {
                return value.replace("{port}", String.valueOf(port));
            }
            if (value.endsWith("/")) {
                return value;
            }
            return value + "/";
        }
        String lanHost = resolveLanIpv4();
        if (lanHost == null || lanHost.isBlank()) {
            return "http://127.0.0.1:" + port + "/";
        }
        return "http://" + lanHost + ":" + port + "/";
    }

    private String resolveLanIpv4() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                var addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var addr = addresses.nextElement();
                    if (addr instanceof Inet4Address ipv4 && !ipv4.isLoopbackAddress()) {
                        return ipv4.getHostAddress();
                    }
                }
            }
            return null;
        } catch (SocketException e) {
            return null;
        }
    }

    private void handleComplete(HttpExchange exchange, CompletableFuture<String> future, HttpServer server) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String error = query.get("error");
        if (error != null && !error.isBlank()) {
            future.completeExceptionally(new IllegalStateException("Passkey cancelled: " + error));
            respondHtml(exchange, 400, "<html><body><h3>Passkey failed</h3><p>" + escape(error) + "</p></body></html>");
            server.stop(0);
            return;
        }
        String credentialId = query.get("credentialId");
        if (credentialId == null || credentialId.isBlank()) {
            future.completeExceptionally(new IllegalStateException("Passkey did not return a credential id."));
            respondHtml(exchange, 400, "<html><body><h3>Missing credential</h3></body></html>");
            server.stop(0);
            return;
        }

        future.complete(credentialId.trim());
        respondHtml(exchange, 200, "<html><body><h3>Passkey OK</h3><p>You can return to the app.</p></body></html>");
        server.stop(0);
    }

    private String renderEnrollPage(String challengeB64, String userIdB64, String email, String rpIdValue, String originValue, String rpNameValue) {
        return """
                <!doctype html>
                <html>
                <head><meta charset="utf-8"><title>Passkey Enrollment</title></head>
                <body style="font-family:Arial,sans-serif;padding:24px;">
                  <h2>Register passkey</h2>
                  <p>Complete your biometric enrollment in the system prompt.</p>
                  <script>
                    const b64urlToBytes = (v) => {
                      const p = v.replace(/-/g, '+').replace(/_/g, '/');
                      const pad = p + '==='.slice((p.length + 3) %% 4);
                      const raw = atob(pad);
                      return Uint8Array.from(raw, c => c.charCodeAt(0));
                    };
                    const bytesToB64url = (bytes) => {
                      let s = '';
                      bytes.forEach(b => s += String.fromCharCode(b));
                      return btoa(s).replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+$/g, '');
                    };
                    (async () => {
                      try {
                        const expectedOrigin = '%s';
                        if (!window.isSecureContext) {
                          throw new Error('WebAuthn requires HTTPS or localhost. Current context is not secure.');
                        }
                        if (window.location.origin !== expectedOrigin) {
                          throw new Error('Invalid origin for WebAuthn. Expected ' + expectedOrigin + ' but got ' + window.location.origin);
                        }
                        if (!window.PublicKeyCredential || !navigator.credentials || !navigator.credentials.create) {
                          throw new Error('WebAuthn is not available in this browser/context.');
                        }
                        const publicKey = {
                          challenge: b64urlToBytes('%s'),
                          rp: { name: '%s', id: '%s' },
                          user: { id: b64urlToBytes('%s'), name: '%s', displayName: '%s' },
                          pubKeyCredParams: [{ type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 }],
                          timeout: 60000,
                          hints: ['hybrid', 'client-device'],
                          authenticatorSelection: { userVerification: 'preferred' },
                          attestation: 'none'
                        };
                        const credential = await navigator.credentials.create({ publicKey });
                        const credentialId = bytesToB64url(new Uint8Array(credential.rawId));
                        window.location.href = '/complete?credentialId=' + encodeURIComponent(credentialId);
                      } catch (e) {
                        window.location.href = '/complete?error=' + encodeURIComponent(e.name + ': ' + e.message);
                      }
                    })();
                  </script>
                </body>
                </html>
                """.formatted(originValue, challengeB64, rpNameValue, rpIdValue, userIdB64, email, email);
    }

    private String renderLoginPage(String challengeB64, String credentialId, String originValue) {
        return """
                <!doctype html>
                <html>
                <head><meta charset="utf-8"><title>Passkey Login</title></head>
                <body style="font-family:Arial,sans-serif;padding:24px;">
                  <h2>Sign in with Face ID / Passkey</h2>
                  <p>Approve in the system prompt.</p>
                  <script>
                    const b64urlToBytes = (v) => {
                      const p = v.replace(/-/g, '+').replace(/_/g, '/');
                      const pad = p + '==='.slice((p.length + 3) %% 4);
                      const raw = atob(pad);
                      return Uint8Array.from(raw, c => c.charCodeAt(0));
                    };
                    const bytesToB64url = (bytes) => {
                      let s = '';
                      bytes.forEach(b => s += String.fromCharCode(b));
                      return btoa(s).replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+$/g, '');
                    };
                    (async () => {
                      try {
                        const expectedOrigin = '%s';
                        if (!window.isSecureContext) {
                          throw new Error('WebAuthn requires HTTPS or localhost. Current context is not secure.');
                        }
                        if (window.location.origin !== expectedOrigin) {
                          throw new Error('Invalid origin for WebAuthn. Expected ' + expectedOrigin + ' but got ' + window.location.origin);
                        }
                        if (!window.PublicKeyCredential || !navigator.credentials || !navigator.credentials.get) {
                          throw new Error('WebAuthn is not available in this browser/context.');
                        }
                        const publicKey = {
                          challenge: b64urlToBytes('%s'),
                          timeout: 60000,
                          hints: ['hybrid', 'client-device'],
                          userVerification: 'preferred',
                          allowCredentials: [{ type: 'public-key', id: b64urlToBytes('%s') }]
                        };
                        const assertion = await navigator.credentials.get({ publicKey });
                        const resolvedId = bytesToB64url(new Uint8Array(assertion.rawId));
                        window.location.href = '/complete?credentialId=' + encodeURIComponent(resolvedId);
                      } catch (e) {
                        window.location.href = '/complete?error=' + encodeURIComponent(e.name + ': ' + e.message);
                      }
                    })();
                  </script>
                </body>
                </html>
                """.formatted(originValue, challengeB64, credentialId);
    }

    private void openBrowser(String url) {
        Exception desktopError = null;
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalStateException("Desktop browser is not supported.");
            }
            Desktop.getDesktop().browse(URI.create(url));
            return;
        } catch (Exception e) {
            desktopError = e;
        }

        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
            return;
        } catch (Exception shellError) {
            throw new IllegalStateException(
                    "Cannot open browser for WebAuthn. Open this URL manually: " + url,
                    desktopError != null ? desktopError : shellError
            );
        }
    }

    public static final class PendingPasskeyFlow {
        private final HttpServer server;
        private final CompletableFuture<String> credentialFuture;
        private final String localUrl;
        private final String qrUrl;

        private PendingPasskeyFlow(HttpServer server,
                                   CompletableFuture<String> credentialFuture,
                                   String localUrl,
                                   String qrUrl) {
            this.server = server;
            this.credentialFuture = credentialFuture;
            this.localUrl = localUrl;
            this.qrUrl = qrUrl;
        }

        public String localUrl() {
            return localUrl;
        }

        public String qrUrl() {
            return qrUrl;
        }

        public void openLocalBrowser() {
            Exception desktopError = null;
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new IllegalStateException("Desktop browser is not supported.");
                }
                Desktop.getDesktop().browse(URI.create(localUrl));
                return;
            } catch (Exception e) {
                desktopError = e;
            }
            try {
                new ProcessBuilder("cmd", "/c", "start", "", localUrl).start();
            } catch (Exception shellError) {
                throw new IllegalStateException(
                        "Cannot open browser for WebAuthn. Open this URL manually: " + localUrl,
                        desktopError != null ? desktopError : shellError
                );
            }
        }

        public String awaitCredential(Duration timeout) {
            try {
                return credentialFuture.get(timeout.toSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("Passkey flow failed: " + e.getMessage(), e);
            }
        }

        public void close() {
            server.stop(0);
        }
    }

    private void respondHtml(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }
        for (String part : rawQuery.split("&")) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
                values.put(key, value);
            }
        }
        return values;
    }

    private byte[] randomBytes(int size) {
        byte[] out = new byte[size];
        RANDOM.nextBytes(out);
        return out;
    }

    private String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String normalizePublicBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.startsWith("https://")) {
            throw new IllegalStateException("WEBAUTHN_PUBLIC_BASE_URL must be a public HTTPS URL (ngrok/cloudflared). Example: https://abc123.ngrok-free.app");
        }
        try {
            URI uri = URI.create(normalized);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalStateException("WEBAUTHN_PUBLIC_BASE_URL host is invalid.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("WEBAUTHN_PUBLIC_BASE_URL is invalid: " + normalized, e);
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private String resolveRpId(String configuredRpId, String baseUrl) {
        String value = configuredRpId;
        if ((value == null || value.isBlank()) && baseUrl != null) {
            value = URI.create(baseUrl).getHost();
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("WEBAUTHN_RP_ID is required. Example: abc123.ngrok-free.app");
        }
        value = value.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.contains("/")) {
            throw new IllegalStateException("WEBAUTHN_RP_ID must be only the domain without protocol. Example: abc123.ngrok-free.app");
        }
        return value;
    }

    private String resolveOrigin(String configuredOrigin, String baseUrl) {
        String value = configuredOrigin;
        if ((value == null || value.isBlank()) && baseUrl != null) {
            URI uri = URI.create(baseUrl);
            value = uri.getScheme() + "://" + uri.getAuthority();
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("WEBAUTHN_ORIGIN is required. Example: https://abc123.ngrok-free.app");
        }
        value = value.trim();
        if (!value.startsWith("https://")) {
            throw new IllegalStateException("WEBAUTHN_ORIGIN must start with https://");
        }
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalStateException("WEBAUTHN_ORIGIN host is invalid.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("WEBAUTHN_ORIGIN is invalid: " + value, e);
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

    private int readConfigInt(String key, int defaultValue) {
        String value = readConfig(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> localCache = dotenvCache;
        if (localCache != null) {
            return localCache;
        }
        synchronized (WebAuthnDesktopService.class) {
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
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException ignored) {
            // ignore dotenv read errors
        }
    }
}
