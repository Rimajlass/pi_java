module Pidev3A47 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires okhttp3;
    requires org.apache.pdfbox;
    requires com.fasterxml.jackson.databind;

    exports pi.mains;
    exports pi.savings.ui;
    exports pi.entities;
    exports pi.services.CurrencyService;
    exports pi.services.RevenueExpenseService;
    exports pi.tools;

    opens pi.mains to javafx.fxml;
    opens pi.controllers.UserTransactionController to javafx.fxml;
    opens pi.controllers.ExpenseRevenueController.BACK to javafx.fxml;
    opens pi.controllers.ExpenseRevenueController.FRONT to javafx.fxml;
    opens pi.controllers.ExpenseRevenueController.UPDATE to javafx.fxml;
    opens pi.controllers.ImprevusCasreelController to javafx.fxml;
    opens pi.controllers.CoursQuizController to javafx.fxml;
    opens pi.savings.ui to javafx.fxml;
}
