package pi.savings.ui;

import javafx.scene.Parent;

public final class AdminSavingsBackOfficeFactory {

    private AdminSavingsBackOfficeFactory() {
    }

    public static Parent buildSavingsWorkspace() {
        return buildWorkspace("Savings");
    }

    public static Parent buildGoalsWorkspace() {
        return buildWorkspace("Goals");
    }

    private static Parent buildWorkspace(String initialTab) {
        SavingsUiController controller = new SavingsUiController();
        GoalsBackOfficeView view = new GoalsBackOfficeView();
        return view.build(controller, initialTab, true);
    }
}
