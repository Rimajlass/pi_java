package pi.controllers.ImprevusCasreelController;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.entities.CasRelles;
import pi.entities.Imprevus;
import pi.entities.User;
import pi.entities.UserNotification;
import pi.mains.MainFx;
import pi.services.ImprevusCasreelService.CasReelService;
import pi.services.ImprevusCasreelService.ImprevusService;
import pi.services.ImprevusCasreelService.UserNotificationService;
import pi.tools.AppEnv;

import java.io.IOException;
import java.util.Locale;

public class ImprevusBackController {
    @FXML private TextField imprevuTitreField;
    @FXML private ComboBox<String> imprevuTypeComboBox;
    @FXML private ListView<Imprevus> imprevusListView;
    @FXML private ListView<CasRelles> casListView;
    @FXML private Label casTitreLabel;
    @FXML private Label casMetaLabel;
    @FXML private Label casEtatLabel;
    @FXML private Label casPaymentLabel;
    @FXML private Label casAuditLabel;
    @FXML private Label casAiSuggestionLabel;
    @FXML private Label casNotificationLabel;
    @FXML private Label systemStatusLabel;
    @FXML private TextArea casDescriptionArea;
    @FXML private TextField raisonRefusField;
    @FXML private TextArea adminNoteArea;

    private final ImprevusService imprevusService = new ImprevusService();
    private final CasReelService casReelService = new CasReelService();
    private final ObservableList<Imprevus> imprevus = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> casReels = FXCollections.observableArrayList();
    private Imprevus selectedImprevu;
    private CasRelles selectedCas;
    private User currentAdmin;
    private final UserNotificationService adminNotificationService = new UserNotificationService();
    private Timeline adminNotificationPoller;
    private int lastAdminPopupNotificationId = -1;

    @FXML
    public void initialize() {
        imprevuTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));
        imprevusListView.setItems(imprevus);
        casListView.setItems(casReels);

        imprevusListView.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Imprevus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });

        casListView.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(CasRelles item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label title = new Label(item.getTitre());
                title.getStyleClass().add("list-card-title");
                Label meta = new Label((item.getImprevus() == null ? "Manual case" : item.getImprevus().getTitre())
                        + " - " + item.getType()
                        + " - " + String.format(Locale.US, "%.2f DT", item.getMontant())
                        + " - " + (item.getPaymentMethod() == null ? "-" : item.getPaymentMethod()));
                meta.getStyleClass().add("list-card-meta");
                Label audit = new Label(buildAuditLine(item));
                audit.getStyleClass().add("list-card-meta");
                Label status = new Label(item.getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : item.getResultat());
                status.getStyleClass().add(resolveStatusStyle(item.getResultat()));
                VBox card = new VBox(6, title, meta, audit, status);
                card.getStyleClass().add("list-card-box");
                setText(null);
                setGraphic(card);
            }
        });

        Platform.runLater(this::resolveCurrentAdmin);
        updateSystemStatus();

        imprevusListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            selectedImprevu = selected;
            if (selected != null) {
                imprevuTitreField.setText(selected.getTitre());
                imprevuTypeComboBox.setValue(selected.getType());
            }
        });
        casListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            selectedCas = selected;
            showSelectedCase(selected);
        });

        refreshImprevus();
        refreshCas();
        Platform.runLater(this::startAdminNotificationPolling);
    }

    private void startAdminNotificationPolling() {
        if (adminNotificationPoller != null) {
            return;
        }
        adminNotificationPoller = new Timeline(new KeyFrame(Duration.seconds(5), e -> checkAdminPopupNotification()));
        adminNotificationPoller.setCycleCount(Timeline.INDEFINITE);
        adminNotificationPoller.play();
    }

    private void checkAdminPopupNotification() {
        if (currentAdmin == null || currentAdmin.getId() <= 0) {
            return;
        }
        adminNotificationService.findLatestByUserId(currentAdmin.getId()).ifPresent(this::maybePopupAdminNotification);
    }

    private void maybePopupAdminNotification(UserNotification notification) {
        if (notification.getId() <= 0 || notification.isRead()) {
            lastAdminPopupNotificationId = Math.max(lastAdminPopupNotificationId, notification.getId());
            return;
        }
        if (notification.getId() == lastAdminPopupNotificationId) {
            return;
        }
        lastAdminPopupNotificationId = notification.getId();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Back-office");
            alert.setHeaderText(notification.getTitle());
            alert.setContentText(notification.getMessage());
            alert.show();
        });
        try {
            adminNotificationService.markAsRead(notification.getId());
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void handleSaveImprevu() {
        String titre = safe(imprevuTitreField);
        String type = imprevuTypeComboBox.getValue();
        if (titre.length() < 3 || type == null) {
            showError("Titre et type de l'imprevu sont obligatoires.");
            return;
        }
        Imprevus imprevu = new Imprevus(titre, type, 0);
        if (selectedImprevu != null) {
            imprevu.setId(selectedImprevu.getId());
            imprevusService.modifier(imprevu);
        } else {
            imprevusService.ajouter(imprevu);
        }
        clearImprevuForm();
        refreshImprevus();
    }

    @FXML
    private void handleDeleteImprevu() {
        if (selectedImprevu == null) {
            showError("Selectionne un imprevu a supprimer.");
            return;
        }
        imprevusService.supprimer(selectedImprevu.getId());
        clearImprevuForm();
        refreshImprevus();
        refreshCas();
    }

    @FXML
    private void handleClearImprevu() {
        clearImprevuForm();
    }

    @FXML
    private void handleAcceptCas() {
        if (selectedCas == null) {
            showError("Selectionne un cas reel a valider.");
            return;
        }
        int caseId = selectedCas.getId();
        CasReelService.CaseWorkflowOutcome outcome = casReelService.changerStatutWithOutcome(
                caseId,
                CasReelService.STATUT_ACCEPTE,
                null,
                safe(adminNoteArea),
                currentAdmin == null ? null : currentAdmin.getId()
        );
        if (selectedCas.getUser() != null) {
            try {
                adminNotificationService.createNotification(
                        selectedCas.getUser().getId(),
                        "Case approved",
                        "Your gain case \"" + selectedCas.getTitre() + "\" has been approved and allocated to the emergency fund."
                );
            } catch (RuntimeException ignored) {
            }
        }
        refreshCas();
        reselectCase(caseId);
        showWorkflowOutcome(outcome);
    }

    @FXML
    private void handleRefuseCas() {
        if (selectedCas == null) {
            showError("Selectionne un cas reel a refuser.");
            return;
        }
        String raison = safe(raisonRefusField);
        if (raison.length() < 3) {
            showError("Ajoute une raison de refus de 3 caracteres minimum.");
            return;
        }
        int caseId = selectedCas.getId();
        CasReelService.CaseWorkflowOutcome outcome = casReelService.changerStatutWithOutcome(
                caseId,
                CasReelService.STATUT_REFUSE,
                raison,
                safe(adminNoteArea),
                currentAdmin == null ? null : currentAdmin.getId()
        );
        refreshCas();
        reselectCase(caseId);
        showWorkflowOutcome(outcome);
    }

    @FXML
    private void handleGoFront() {
        switchScene("/imprevus-view.fxml", "Unexpected Events & Real Cases");
    }

    private void refreshImprevus() {
        imprevus.setAll(imprevusService.afficher());
        imprevusListView.refresh();
    }

    private void refreshCas() {
        casReels.setAll(
                casReelService.afficher()
                        .stream()
                        .filter(cas -> "Gain".equalsIgnoreCase(cas.getType()))
                        .toList()
        );

        casListView.refresh();

        showSelectedCase(
                selectedCas == null
                        ? null
                        : casReels.stream()
                          .filter(c -> c.getId() == selectedCas.getId())
                          .findFirst()
                          .orElse(null)
        );
    }

    private void reselectCase(int id) {
        casReels.stream()
                .filter(cas -> cas.getId() == id)
                .findFirst()
                .ifPresent(cas -> casListView.getSelectionModel().select(cas));
    }

    private void showSelectedCase(CasRelles cas) {
        selectedCas = cas;
        if (cas == null) {
            casTitreLabel.setText("Select a case");
            casMetaLabel.setText("No case selected");
            casEtatLabel.setText("-");
            casPaymentLabel.setText("Payment method: -");
            casAuditLabel.setText("Processed by: -");
            casAiSuggestionLabel.setText("AI suggestion: -");
            casNotificationLabel.setText("Notification: -");
            casDescriptionArea.clear();
            raisonRefusField.clear();
            adminNoteArea.clear();
            return;
        }
        casTitreLabel.setText(cas.getTitre());
        casMetaLabel.setText((cas.getImprevus() == null ? "Manual case" : cas.getImprevus().getTitre()) + " - " + cas.getType() + " - " + String.format(Locale.US, "%.2f DT", cas.getMontant()));
        casEtatLabel.setText(cas.getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : cas.getResultat());
        casPaymentLabel.setText("Payment method: " + (cas.getPaymentMethod() == null ? "-" : cas.getPaymentMethod()));
        casAuditLabel.setText(buildAuditLine(cas));
        casAiSuggestionLabel.setText("AI suggestion: " + (cas.getAiRefusalSuggestion() == null || cas.getAiRefusalSuggestion().isBlank() ? "-" : cas.getAiRefusalSuggestion()));
        casNotificationLabel.setText("Notification: " + buildNotificationLine(cas));
        casDescriptionArea.setText(cas.getDescription());
        raisonRefusField.setText(cas.getRaisonRefus() == null || cas.getRaisonRefus().isBlank()
                ? (cas.getAiRefusalSuggestion() == null ? "" : cas.getAiRefusalSuggestion())
                : cas.getRaisonRefus());
        adminNoteArea.setText(cas.getAdminNote() == null ? "" : cas.getAdminNote());
    }

    private String resolveStatusStyle(String statut) {
        if (CasReelService.STATUT_ACCEPTE.equals(statut)) {
            return "status-accepted";
        }
        if (CasReelService.STATUT_REFUSE.equals(statut)) {
            return "status-refused";
        }
        if (CasReelService.STATUT_EN_ATTENTE_ALLOCATION.equals(statut)) {
            return "status-pending";
        }
        return "status-pending";
    }

    private void clearImprevuForm() {
        selectedImprevu = null;
        imprevusListView.getSelectionModel().clearSelection();
        imprevuTitreField.clear();
        imprevuTypeComboBox.getSelectionModel().clearSelection();
    }

    private String safe(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String safe(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private String buildAuditLine(CasRelles cas) {
        String processedBy = cas.getConfirmedBy() == null
                ? "-"
                : (cas.getConfirmedBy().getNom() == null || cas.getConfirmedBy().getNom().isBlank()
                ? cas.getConfirmedBy().getEmail()
                : cas.getConfirmedBy().getNom());
        String processedAt = cas.getConfirmedAt() == null ? "-" : cas.getConfirmedAt().toString().replace('T', ' ');
        String reason = cas.getRaisonRefus() == null || cas.getRaisonRefus().isBlank() ? "No refusal reason" : cas.getRaisonRefus();
        String proof = cas.getJustificatifFileName() == null || cas.getJustificatifFileName().isBlank() ? "-" : cas.getJustificatifFileName();
        String advice = cas.getAiRefusalSuggestion() == null || cas.getAiRefusalSuggestion().isBlank() ? "-" : cas.getAiRefusalSuggestion();
        return "Processed by: " + processedBy + " | at: " + processedAt + " | reason: " + reason + " | proof: " + proof + " | advice: " + advice;
    }

    private void resolveCurrentAdmin() {
        Stage stage = (Stage) imprevusListView.getScene().getWindow();
        Object userData = stage.getUserData();
        if (userData instanceof User user) {
            currentAdmin = user;
            startAdminNotificationPolling();
        }
    }

    private void updateSystemStatus() {
        if (systemStatusLabel == null) {
            return;
        }
        String mailStatus = AppEnv.has("MAILER_DSN") && AppEnv.has("MAILER_FROM_ADDRESS") ? "mail actif" : "mail a configurer (.env)";
        String mapsStatus = AppEnv.has("LOCATIONIQ_API_KEY") ? "maps actif" : "maps a configurer (.env)";
        systemStatusLabel.setText("System status: " + mailStatus + " | " + mapsStatus + " | notif in-app active");
    }

    private String buildNotificationLine(CasRelles cas) {
        if (cas.getNotificationSentAt() != null) {
            return "mail sent at " + cas.getNotificationSentAt().toString().replace('T', ' ');
        }
        if (!AppEnv.has("MAILER_DSN") || !AppEnv.has("MAILER_FROM_ADDRESS")) {
            return "mail not sent: MAILER_DSN / MAILER_FROM_* missing in .env";
        }
        return "in-app notification created, mail not confirmed yet";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWorkflowOutcome(CasReelService.CaseWorkflowOutcome outcome) {
        if (outcome == null) {
            return;
        }
        if (outcome.notificationError() != null && !outcome.notificationError().isBlank()) {
            showInfo("Notification", "Statut mis a jour, mais notification in-app: " + outcome.notificationError());
        }
        if (outcome.decisionEmailSkippedByPolicy()) {
            showInfo("Email", "Pas d'email de decision (politique allocation / validation auto — notifications in-app uniquement).");
            return;
        }
        if (!outcome.emailSent()) {
            String reason = outcome.emailError() == null || outcome.emailError().isBlank()
                    ? "email non envoye (raison inconnue)"
                    : outcome.emailError();
            showInfo("Email", "Statut mis a jour, mais email non envoye: " + reason);
        }
    }

    private void switchScene(String resource, String title) {
        try {
            Stage stage = (Stage) imprevusListView.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(MainFx.class.getResource(resource));
            stage.setUserData(currentAdmin);
            stage.setScene(new Scene(loader.load(), 1400, 900));
            stage.setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException("Erreur navigation : " + e.getMessage(), e);
        }
    }
}
