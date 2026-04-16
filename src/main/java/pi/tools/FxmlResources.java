package pi.tools;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Loads FXML from the classpath in a way that survives Windows paths with spaces
 * (URLs containing {@code %20}) which can break {@code new FXMLLoader(URL)} / parsers.
 */
public final class FxmlResources {

    private FxmlResources() {
    }

    public static FXMLLoader load(Class<?> anchor, String classpathFxml) throws IOException {
        try (InputStream stream = anchor.getResourceAsStream(classpathFxml)) {
            if (stream == null) {
                throw new IOException("Classpath resource not found: " + classpathFxml);
            }
            FXMLLoader loader = new FXMLLoader();
            URL base = anchor.getResource(directoryPrefix(classpathFxml));
            if (base != null) {
                loader.setLocation(safeFileUrl(base));
            }
            loader.load(stream);
            return loader;
        }
    }

    public static void addStylesheet(Scene scene, Class<?> anchor, String classpathCss) throws IOException {
        URL css = anchor.getResource(classpathCss);
        if (css == null) {
            return;
        }
        scene.getStylesheets().add(safeFileUrl(css).toExternalForm());
    }

    private static String directoryPrefix(String classpathFxml) {
        String path = classpathFxml.startsWith("/") ? classpathFxml : "/" + classpathFxml;
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash + 1) : "/";
    }

    private static URL safeFileUrl(URL url) throws IOException {
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return url;
        }
        try {
            return Paths.get(url.toURI()).toUri().toURL();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid file resource URI: " + url, e);
        }
    }
}
