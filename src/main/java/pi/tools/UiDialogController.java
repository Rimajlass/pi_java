package pi.tools;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class UiDialogController {

    @FXML
    private StackPane overlayRoot;

    @FXML
    private VBox dialogCard;

    @FXML
    private Label iconLabel;

    @FXML
    private Label headingLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private VBox hintBox;

    @FXML
    private Label hintLabel;

    @FXML
    private HBox actionsBox;

    @FXML
    private Button cancelButton;

    @FXML
    private Button okButton;

    @FXML
    private Button closeButton;

    public StackPane getOverlayRoot() {
        return overlayRoot;
    }

    public VBox getDialogCard() {
        return dialogCard;
    }

    public Label getIconLabel() {
        return iconLabel;
    }

    public Label getHeadingLabel() {
        return headingLabel;
    }

    public Label getMessageLabel() {
        return messageLabel;
    }

    public HBox getActionsBox() {
        return actionsBox;
    }

    public VBox getHintBox() {
        return hintBox;
    }

    public Label getHintLabel() {
        return hintLabel;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public Button getOkButton() {
        return okButton;
    }

    public Button getCloseButton() {
        return closeButton;
    }
}
