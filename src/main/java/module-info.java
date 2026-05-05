module Pidev3A47 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;
    requires com.google.zxing;
    requires jakarta.mail;
    requires java.desktop;
    requires java.net.http;
    requires java.prefs;
    requires java.sql;
    requires jdk.httpserver;
    requires jdk.jsobject;
    requires mysql.connector.j;
    requires okhttp3;
    requires org.apache.pdfbox;
    requires vosk;
    requires webcam.capture;

    exports pi.mains;
    exports pi.savings.ui;
    exports pi.entities;
    exports pi.services.CurrencyService;
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
    opens pi.tools to javafx.fxml;
}
