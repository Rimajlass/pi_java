package pi.tools;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class TransactionDetailsDialogController {

    @FXML
    private StackPane overlayRoot;

    @FXML
    private VBox modalCard;

    @FXML
    private Label titleLabel;

    @FXML
    private Label typeBadge;

    @FXML
    private Label amountValueLabel;

    @FXML
    private Label dateValueLabel;

    @FXML
    private Label userValueLabel;

    @FXML
    private Label descriptionValueLabel;

    @FXML
    private Label sourceValueLabel;

    @FXML
    private Button closeButton;

    @FXML
    private Button okButton;

    public StackPane getOverlayRoot() {
        return overlayRoot;
    }

    public VBox getModalCard() {
        return modalCard;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    public Label getTypeBadge() {
        return typeBadge;
    }

    public Label getAmountValueLabel() {
        return amountValueLabel;
    }

    public Label getDateValueLabel() {
        return dateValueLabel;
    }

    public Label getUserValueLabel() {
        return userValueLabel;
    }

    public Label getDescriptionValueLabel() {
        return descriptionValueLabel;
    }

    public Label getSourceValueLabel() {
        return sourceValueLabel;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public Button getOkButton() {
        return okButton;
    }
}
