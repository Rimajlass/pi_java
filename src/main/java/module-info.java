module Pidev3A47 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires java.prefs;
    requires java.sql;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires vosk;
    requires jdk.jsobject;
    requires jdk.httpserver;
    requires mysql.connector.j;
    requires jakarta.mail;

    exports pi.mains;
    exports pi.savings.ui;
    exports pi.entities;
    exports pi.services.RevenueExpenseService;
    exports pi.tools;
    exports pi.assistant;

    opens pi.mains to javafx.fxml;
    opens pi.controllers.UserTransactionController to javafx.fxml;
    opens pi.controllers.ExpenseRevenueController.BACK to javafx.fxml;
    opens pi.controllers.ExpenseRevenueController.FRONT to javafx.fxml;
    opens pi.controllers.ExpenseRevenueController.UPDATE to javafx.fxml;
    opens pi.controllers.ImprevusCasreelController to javafx.fxml;
    opens pi.controllers.CoursQuizController to javafx.fxml;
    opens pi.savings.ui to javafx.fxml;
}
