module Pidev3A47 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    exports pi.mains;

    opens pi.mains to javafx.fxml;
    opens pi.controllers.UserTransactionController to javafx.fxml;
}
