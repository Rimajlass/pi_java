package pi.controllers.CoursQuizController;

import javafx.fxml.FXML;

public class CoursQuizBackOfficeController {

    private CoursQuizDashboardController dashboardController;

    public void setDashboardController(CoursQuizDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void onShow() {
        // placeholder for future refresh hooks
    }

    @FXML
    private void onBackToFront() {
        if (dashboardController != null) {
            dashboardController.showFront();
        }
    }
}

