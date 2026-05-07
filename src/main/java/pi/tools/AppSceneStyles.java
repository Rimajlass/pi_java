package pi.tools;

import javafx.scene.Scene;
import pi.mains.Main;

import java.io.IOException;

public final class AppSceneStyles {
    private static final String GLOBAL_CSS = "/pi/styles/global.css";

    private AppSceneStyles() {
    }

    public static void apply(Scene scene, String... pageStylesheets) throws IOException {
        if (scene == null) {
            return;
        }
        if (pageStylesheets != null) {
            for (String stylesheet : pageStylesheets) {
                if (stylesheet != null && !stylesheet.isBlank()) {
                    FxmlResources.addStylesheet(scene, Main.class, stylesheet);
                }
            }
        }
        FxmlResources.addStylesheet(scene, Main.class, GLOBAL_CSS);
        ThemeManager.registerScene(scene);
    }
}
