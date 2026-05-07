package pi.tools;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;
import java.util.logging.Logger;

public final class ThemeManager {

    public static final String DARK_THEME = "/pi/styles/dark-theme.css";
    public static final String LIGHT_THEME = "/pi/styles/light-theme.css";

    private static final String PREF_NODE = "pi_java_theme";
    private static final String PREF_KEY = "selected_theme";
    private static final String DARK = "dark";
    private static final String LIGHT = "light";
    private static final Logger LOG = Logger.getLogger(ThemeManager.class.getName());
    private static final boolean THEME_DEBUG = Boolean.getBoolean("pi.theme.debug");
    private static final Set<Scene> REGISTERED_SCENES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ThemeManager() {
    }

    public static void applySavedTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        String selected = getSelectedTheme();
        debug("applySavedTheme selected=" + selected);
        applyTheme(scene, LIGHT.equals(selected) ? LIGHT_THEME : DARK_THEME);
    }

    public static void registerScene(Scene scene) {
        if (scene == null) {
            return;
        }
        REGISTERED_SCENES.add(scene);
        applySavedTheme(scene);
        debugSceneStylesheets(scene, "registerScene");
    }

    public static void registerStage(Stage stage) {
        if (stage == null) {
            return;
        }
        registerScene(stage.getScene());
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> registerScene(newScene));
    }

    public static boolean toggleTheme(Scene scene) {
        boolean darkMode = scene == null ? isDarkSelected() : isDarkMode(scene);
        setDarkMode(!darkMode);
        debug("toggleTheme -> " + (darkMode ? LIGHT : DARK));
        return !darkMode;
    }

    public static void setDarkMode(boolean enabled) {
        String nextTheme = enabled ? DARK_THEME : LIGHT_THEME;
        Preferences.userRoot()
                .node(PREF_NODE)
                .put(PREF_KEY, enabled ? DARK : LIGHT);
        applyThemeToRegisteredScenes(nextTheme);
        debug("setDarkMode -> " + (enabled ? DARK : LIGHT));
    }

    public static void resetToLightMode() {
        setDarkMode(false);
    }

    public static boolean isDarkMode(Scene scene) {
        if (scene == null) {
            return isDarkSelected();
        }
        String darkUrl = toExternal(DARK_THEME);
        return scene.getStylesheets().stream().anyMatch(css -> css.equals(darkUrl));
    }

    public static boolean isDarkSelected() {
        String selected = getSelectedTheme();
        return DARK.equals(selected);
    }

    private static String getSelectedTheme() {
        return Preferences.userRoot().node(PREF_NODE).get(PREF_KEY, LIGHT);
    }

    private static void applyTheme(Scene scene, String themePath) {
        if (scene == null) {
            return;
        }
        String darkUrl = toExternal(DARK_THEME);
        String lightUrl = toExternal(LIGHT_THEME);
        scene.getStylesheets().remove(darkUrl);
        scene.getStylesheets().remove(lightUrl);
        String themeUrl = toExternal(themePath);
        if (!scene.getStylesheets().contains(themeUrl)) {
            scene.getStylesheets().add(themeUrl);
        }
        debug("applyTheme added=" + themeUrl);
        debugSceneStylesheets(scene, "after applyTheme");
    }

    private static void applyThemeToRegisteredScenes(String themePath) {
        for (Scene registered : new ArrayList<>(REGISTERED_SCENES)) {
            applyTheme(registered, themePath);
        }
    }

    private static String toExternal(String resourcePath) {
        return ThemeManager.class.getResource(resourcePath).toExternalForm();
    }

    public static void debugSceneStylesheets(Scene scene, String context) {
        if (!THEME_DEBUG || scene == null) {
            return;
        }
        String prefix = context == null ? "" : context + " ";
        LOG.info(prefix + "stylesheets count=" + scene.getStylesheets().size());
        for (String css : scene.getStylesheets()) {
            LOG.info("  css=" + css);
        }
    }

    private static void debug(String message) {
        if (THEME_DEBUG) {
            LOG.info(message);
        }
    }
}
