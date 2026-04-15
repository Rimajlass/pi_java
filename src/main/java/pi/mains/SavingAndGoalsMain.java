package pi.mains;

import javafx.application.Application;
import pi.savings.ui.SavingsGoalsApp;

public final class SavingAndGoalsMain {

    private SavingAndGoalsMain() {
    }

    public static void main(String[] args) {
        Application.launch(SavingsGoalsApp.class, args);
    }
}
