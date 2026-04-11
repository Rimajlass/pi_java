package pi.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Afficher {

    @FXML
    private TextField fList;

    @FXML
    private TextField fnom;

    @FXML
    private TextField fprenom;

    public void setfList(String fList) {
        this.fList.setText(fList);
    }

    public void setFnom(String fnom) {
        this.fnom.setText(fnom);
    }

    public void setFprenom(String fprenom) {
        this.fprenom.setText(fprenom);
    }
}
