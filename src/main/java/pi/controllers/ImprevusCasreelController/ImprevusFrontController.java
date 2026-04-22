package pi.controllers.ImprevusCasreelController;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.controllers.ExpenseRevenueController.FRONT.SalaryExpenseController;
import pi.controllers.UserTransactionController.AboutController;
import pi.controllers.UserTransactionController.ContactController;
import pi.controllers.UserTransactionController.SalaryHomeController;
import pi.controllers.UserTransactionController.ServiceController;
import pi.entities.CasRelles;
import pi.entities.Imprevus;
import pi.entities.User;
import pi.mains.Main;
import pi.services.ImprevusCasreelService.CasReelService;
import pi.services.ImprevusCasreelService.CaseFundingAdvice;
import pi.services.ImprevusCasreelService.ImprevusService;
import pi.services.ImprevusCasreelService.LocationSuggestionService;
import pi.services.ImprevusCasreelService.UserNotificationService;
import pi.savings.ui.SavingsGoalsApp;
import pi.tools.AppEnv;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ImprevusFrontController {
    @FXML private ScrollPane pageScrollPane;
    @FXML private VBox caseFormCard;
    @FXML private VBox historyCard;
    @FXML private TextField casTitreField;
    @FXML private ComboBox<String> casTypeComboBox;
    @FXML private TextField casMontantField;
    @FXML private TextField justificatifField;
    @FXML private TextArea casDescriptionField;
    @FXML private TextField searchImprevusField;
    @FXML private TextField searchCasReelsField;
    @FXML private ComboBox<String> triImprevusComboBox;
    @FXML private ComboBox<String> triCasReelsComboBox;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private DatePicker dateEffetPicker;
    @FXML private Label statusLabel;
    @FXML private Label fundingAdviceLabel;
    @FXML private Label selectedCaseTitleLabel;
    @FXML private Label selectedCaseSourceLabel;
    @FXML private Label selectedCaseDescriptionLabel;
    @FXML private Label impactLabel;
    @FXML private Label selectedCaseEtatLabel;
    @FXML private Label selectedEventHintLabel;
    @FXML private Label totalImprevusValueLabel;
    @FXML private Label pendingCasesValueLabel;
    @FXML private Label acceptedCasesValueLabel;
    @FXML private Label emergencyFundValueLabel;
    @FXML private Label latestNotificationLabel;
    @FXML private Label futureRiskLabel;
    @FXML private Label voitureRiskLabel;
    @FXML private Label santeRiskLabel;
    @FXML private Label maisonRiskLabel;
    @FXML private Label electroniqueRiskLabel;
    @FXML private Label educationRiskLabel;
    @FXML private Label facturesRiskLabel;
    @FXML private Label autresRiskLabel;
    @FXML private Label weeklyAdviceLabel;
    @FXML private Label appointmentSuggestionLabel;
    @FXML private ListView<Imprevus> imprevusListView;
    @FXML private TableView<CasRelles> casReelsTableView;
    @FXML private TableColumn<CasRelles, String> historyDateColumn;
    @FXML private TableColumn<CasRelles, String> historyTitleColumn;
    @FXML private TableColumn<CasRelles, String> historyTypeColumn;
    @FXML private TableColumn<CasRelles, String> historyCategoryColumn;
    @FXML private TableColumn<CasRelles, String> historyAmountColumn;
    @FXML private TableColumn<CasRelles, String> historySolutionColumn;
    @FXML private TableColumn<CasRelles, String> historyStatusColumn;
    @FXML private TableColumn<CasRelles, String> historyJustificatifColumn;
    @FXML private TableColumn<CasRelles, String> historyTreatedByColumn;
    @FXML private TableColumn<CasRelles, String> historyTreatedAtColumn;
    @FXML private TableColumn<CasRelles, String> historyRefusalReasonColumn;
    @FXML private Button saveCasButton;

    private final ImprevusService imprevusService = new ImprevusService();
    private final CasReelService casReelService = new CasReelService();
    private final UserNotificationService userNotificationService = new UserNotificationService();
    private final LocationSuggestionService locationSuggestionService = new LocationSuggestionService();
    private final ObservableList<Imprevus> imprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> casReelsList = FXCollections.observableArrayList();
    private final ObservableList<Imprevus> allImprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> allCasReelsList = FXCollections.observableArrayList();
    private Imprevus imprevuSelectionne;
    private CasRelles casReelSelectionne;
    private User currentUser;
    private Timeline notificationPoller;
    private int lastPoppedNotificationId = -1;

    public void setUser(User user) {
        this.currentUser = user;
        refreshFundingAdvicePreview();
        updateStats();
        updateRiskInsights();
        updateLatestNotification();
        startNotificationPolling();
    }

    @FXML
    public void initialize() {
        casTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(
                CasReelService.PAYMENT_EMERGENCY_FUND,
                CasReelService.PAYMENT_SAVING_ACCOUNT
        ));
        paymentMethodComboBox.setValue(CasReelService.PAYMENT_EMERGENCY_FUND);
        triImprevusComboBox.setItems(FXCollections.observableArrayList("Titre A-Z", "Titre Z-A"));
        triCasReelsComboBox.setItems(FXCollections.observableArrayList("Plus recent", "Plus ancien", "Titre A-Z", "Titre Z-A"));
        triImprevusComboBox.setValue("Titre A-Z");
        triCasReelsComboBox.setValue("Plus recent");
        dateEffetPicker.setValue(LocalDate.now());
        appliquerControleSaisie();
        configureLists();
        searchImprevusField.textProperty().addListener((obs, oldValue, newValue) -> filtrerImprevus(newValue));
        searchCasReelsField.textProperty().addListener((obs, oldValue, newValue) -> filtrerCasReels(newValue));
        triImprevusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> filtrerImprevus(searchImprevusField.getText()));
        triCasReelsComboBox.valueProperty().addListener((obs, oldValue, newValue) -> filtrerCasReels(searchCasReelsField.getText()));
        casTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateFundingControlsForType());
        paymentMethodComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshFundingAdvicePreview());
        casMontantField.textProperty().addListener((obs, oldValue, newValue) -> refreshFundingAdvicePreview());
        refreshImprevus();
        refreshCasReels();
        resetDetails();
        updateSelectedEventHint();
        updateFundingControlsForType();
        startNotificationPolling();
    }

    private void startNotificationPolling() {
        if (notificationPoller != null) {
            return;
        }
        notificationPoller = new Timeline(new KeyFrame(Duration.seconds(4), e -> checkAndPopupLatestNotification()));
        notificationPoller.setCycleCount(Timeline.INDEFINITE);
        notificationPoller.play();
    }

    private void checkAndPopupLatestNotification() {
        if (currentUser == null || currentUser.getId() <= 0) {
            return;
        }
        userNotificationService.findLatestByUserId(currentUser.getId()).ifPresent(notification -> {
            updateLatestNotification();
            if (notification.getId() <= 0) {
                return;
            }
            if (notification.isRead()) {
                lastPoppedNotificationId = Math.max(lastPoppedNotificationId, notification.getId());
                return;
            }
            if (notification.getId() == lastPoppedNotificationId) {
                return;
            }
            lastPoppedNotificationId = notification.getId();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Notification");
                alert.setHeaderText(notification.getTitle());
                alert.setContentText(notification.getMessage());
                alert.show();
            });
            try {
                userNotificationService.markAsRead(notification.getId());
            } catch (Exception ignored) {
            }
        });
    }

    private void configureLists() {
        imprevusListView.setItems(imprevusList);
        imprevusListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Imprevus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label title = new Label(item.getTitre());
                title.getStyleClass().add("list-card-title");
                VBox box = new VBox(6, title);
                box.getStyleClass().add("list-card-box");
                setGraphic(box);
            }
        });
        imprevusListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            imprevuSelectionne = selected;
            if (selected != null) {
                afficherDetailsDepuisImprevu(selected);
                statusLabel.setText("Unexpected event selected. The real-case form stays independent.");
            }
            updateSelectedEventHint();
        });

        casReelsTableView.setItems(casReelsList);
        casReelsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        casReelsTableView.setFixedCellSize(38);
        casReelsTableView.setPlaceholder(new Label("No case history yet."));
        historyDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateEffet() == null ? "-" : cell.getValue().getDateEffet().toString()));
        historyTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitre()));
        historyTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));
        historyCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategorie()));
        historyAmountColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatSignedMontant(cell.getValue())));
        historySolutionColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPaymentMethod() == null ? "-" : cell.getValue().getPaymentMethod()));
        historyStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : cell.getValue().getResultat()));
        historyJustificatifColumn.setCellValueFactory(cell -> new SimpleStringProperty(blankToDash(cell.getValue().getJustificatifFileName())));
        historyTreatedByColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveTreatedBy(cell.getValue())));
        historyTreatedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getConfirmedAt() == null ? "-" : cell.getValue().getConfirmedAt().toString().replace('T', ' ')));
        historyRefusalReasonColumn.setCellValueFactory(cell -> new SimpleStringProperty(blankToDash(cell.getValue().getRaisonRefus())));
        casReelsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                afficherDetails(selected);
            }
        });
        casReelsTableView.setRowFactory(table -> {
            TableRow<CasRelles> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    CasRelles item = row.getItem();
                    casReelSelectionne = item;
                    remplirFormulaireDepuisCasReel(item);
                    afficherDetails(item);
                    saveCasButton.setText("Save changes");
                    statusLabel.setText("Case loaded from History. Update the form, then save.");
                    scrollToNode(caseFormCard);
                }
            });
            return row;
        });
    }

    @FXML
    private void handleFocusAddCase() {
        scrollToNode(caseFormCard);
    }

    @FXML
    private void handleFocusHistory() {
        scrollToNode(historyCard);
    }

    @FXML
    private void handleChoisirFichier() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un justificatif");
        File file = fileChooser.showOpenDialog(justificatifField.getScene().getWindow());
        if (file != null) {
            justificatifField.setText(file.getName());
        }
    }

    @FXML
    private void handleSaveCase() {
        CasRelles cas = lireFormulaire();
        if (cas == null) {
            return;
        }
        if (isDuplicateCas(cas)) {
            afficherErreur("Ce cas reel existe deja.");
            return;
        }
        try {
            if (casReelSelectionne != null) {
                cas.setId(casReelSelectionne.getId());
                cas.setResultat(casReelSelectionne.getResultat());
                cas.setRaisonRefus(casReelSelectionne.getRaisonRefus());
                cas.setConfirmedAt(casReelSelectionne.getConfirmedAt());
                casReelService.modifier(cas);
                statusLabel.setText("Case updated and saved.");
            } else {
                cas.setResultat(CasReelService.STATUT_EN_ATTENTE);
                casReelService.ajouter(cas);
                statusLabel.setText("Case added. It now appears in My History.");
            }
            refreshCasReels();
            clearCaseEditor();
            scrollToNode(historyCard);
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void handleClearCase() {
        clearCaseEditor();
        statusLabel.setText("Case editor cleared.");
    }

    @FXML
    private void handleUseEmergencyFund() {
        appliquerSolution("Emergency Fund");
    }

    @FXML
    private void handleUseObjectif() {
        appliquerSolution("Objectif");
    }

    @FXML
    private void handleAskFamily() {
        appliquerSolution("Famille");
    }

    private void supprimerDepuisListe(CasRelles cas) {
        try {
            casReelService.supprimer(cas.getId());
            refreshCasReels();
            if (casReelSelectionne != null && casReelSelectionne.getId() == cas.getId()) {
                clearCaseEditor();
            }
            statusLabel.setText("Case deleted from My History.");
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void refreshImprevus() {
        allImprevusList.setAll(imprevusService.afficher());
        totalImprevusValueLabel.setText(String.valueOf(allImprevusList.size()));
        filtrerImprevus(searchImprevusField == null ? "" : searchImprevusField.getText());
    }

    private void refreshCasReels() {
        allCasReelsList.setAll(casReelService.afficher());
        updateStats();
        updateRiskInsights();
        updateLatestNotification();
        filtrerCasReels(searchCasReelsField == null ? "" : searchCasReelsField.getText());
    }

    private void updateStats() {
        long pending = allCasReelsList.stream().filter(cas -> CasReelService.STATUT_EN_ATTENTE.equals(cas.getResultat())).count();
        long accepted = allCasReelsList.stream().filter(cas -> CasReelService.STATUT_ACCEPTE.equals(cas.getResultat())).count();
        pendingCasesValueLabel.setText(String.valueOf(pending));
        acceptedCasesValueLabel.setText(String.valueOf(accepted));
        if (currentUser != null && emergencyFundValueLabel != null) {
            emergencyFundValueLabel.setText(formatMontant(casReelService.calculateEmergencyFundBalance(currentUser.getId())));
        } else if (emergencyFundValueLabel != null) {
            emergencyFundValueLabel.setText("0.00 DT");
        }
    }

    private void remplirFormulaireDepuisCasReel(CasRelles cas) {
        casTitreField.setText(cas.getTitre());
        casTypeComboBox.setValue(cas.getType());
        casDescriptionField.setText(cas.getDescription());
        casMontantField.setText(String.valueOf(cas.getMontant()));
        justificatifField.setText(cas.getJustificatifFileName());
        dateEffetPicker.setValue(cas.getDateEffet());
        paymentMethodComboBox.setValue(cas.getPaymentMethod() == null ? CasReelService.PAYMENT_EMERGENCY_FUND : cas.getPaymentMethod());
        imprevuSelectionne = cas.getImprevus();
        updateSelectedEventHint();
        updateFundingControlsForType();
    }

    private CasRelles lireFormulaire() {
        String titre = safe(casTitreField);
        String type = casTypeComboBox.getValue();
        String montantText = safe(casMontantField);
        String description = safe(casDescriptionField);
        String justificatif = safe(justificatifField);
        String paymentMethod = paymentMethodComboBox.getValue();
        LocalDate dateEffet = dateEffetPicker.getValue();
        if (titre.length() < 3 || type == null || montantText.isBlank() || dateEffet == null || paymentMethod == null) {
            afficherErreur("Titre, type, montant, date et methode de paiement sont obligatoires.");
            return null;
        }
        double montant = parsePositiveDouble(montantText, "montant du cas reel");
        if (montant <= 0) {
            return null;
        }
        CasRelles cas = new CasRelles(imprevuSelectionne, titre, description, type,
                imprevuSelectionne == null ? "Manuel" : "Depuis imprevu",
                montant, casReelSelectionne == null ? null : casReelSelectionne.getSolution(),
                dateEffet, justificatif);
        cas.setUser(currentUser);
        cas.setPaymentMethod(paymentMethod);
        if (currentUser != null) {
            CaseFundingAdvice advice = casReelService.analyzeFundingChoice(currentUser.getId(), paymentMethod, montant);
            cas.setAiRefusalSuggestion(advice.suggestion());
        }
        return cas;
    }

    private boolean isDuplicateCas(CasRelles cas) {
        return allCasReelsList.stream()
                .filter(existing -> casReelSelectionne == null || existing.getId() != casReelSelectionne.getId())
                .anyMatch(existing ->
                        normalize(existing.getTitre()).equals(normalize(cas.getTitre()))
                                && normalize(existing.getType()).equals(normalize(cas.getType()))
                                && Double.compare(existing.getMontant(), cas.getMontant()) == 0
                                && existing.getDateEffet() != null
                                && existing.getDateEffet().equals(cas.getDateEffet())
                                && sameImprevu(existing.getImprevus(), cas.getImprevus()));
    }

    private void filtrerImprevus(String query) {
        String value = normalize(query);
        if (value.isEmpty()) {
            imprevusList.setAll(allImprevusList);
        } else {
            imprevusList.setAll(allImprevusList.filtered(imprevu -> contains(imprevu.getTitre(), value)));
        }
        applyImprevuSort();
    }

    private void filtrerCasReels(String query) {
        String value = normalize(query);
        if (value.isEmpty()) {
            casReelsList.setAll(allCasReelsList);
        } else {
            casReelsList.setAll(allCasReelsList.filtered(cas ->
                    contains(cas.getTitre(), value) || contains(cas.getDescription(), value)
                            || contains(cas.getResultat(), value) || contains(getSourceLabel(cas), value)));
        }
        applyCasSort();
    }

    private void applyImprevuSort() {
        Comparator<Imprevus> comparator = "Titre Z-A".equals(triImprevusComboBox.getValue())
                ? Comparator.comparing(this::normalizeImprevuTitle).reversed()
                : Comparator.comparing(this::normalizeImprevuTitle);
        FXCollections.sort(imprevusList, comparator);
    }

    private void applyCasSort() {
        Comparator<CasRelles> comparator;
        if ("Plus ancien".equals(triCasReelsComboBox.getValue())) {
            comparator = Comparator.comparing(CasRelles::getDateEffet, Comparator.nullsLast(LocalDate::compareTo)).thenComparing(CasRelles::getId);
        } else if ("Titre A-Z".equals(triCasReelsComboBox.getValue())) {
            comparator = Comparator.comparing(this::normalizeCasTitle).thenComparing(CasRelles::getId);
        } else if ("Titre Z-A".equals(triCasReelsComboBox.getValue())) {
            comparator = Comparator.comparing(this::normalizeCasTitle, Comparator.reverseOrder()).thenComparing(CasRelles::getId);
        } else {
            comparator = Comparator.comparing(CasRelles::getDateEffet, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(CasRelles::getId, Comparator.reverseOrder());
        }
        FXCollections.sort(casReelsList, comparator);
    }

    private void appliquerControleSaisie() {
        casMontantField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,7}([\\.,]\\d{0,2})?") ? change : null));
        casTitreField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().length() <= 60 ? change : null));
        casDescriptionField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().length() <= 250 ? change : null));
    }

    private void updateFundingControlsForType() {
        boolean isGain = "Gain".equalsIgnoreCase(casTypeComboBox.getValue());
        if (isGain) {
            paymentMethodComboBox.setValue(CasReelService.PAYMENT_EMERGENCY_FUND);
        }
        paymentMethodComboBox.setDisable(isGain);
        refreshFundingAdvicePreview();
    }

    private void refreshFundingAdvicePreview() {
        if (fundingAdviceLabel == null) {
            return;
        }
        if (currentUser == null) {
            fundingAdviceLabel.setText("Connecte un utilisateur pour analyser la source de financement.");
            return;
        }
        String amountText = safe(casMontantField);
        if (amountText.isBlank()) {
            fundingAdviceLabel.setText("Choisis une methode de paiement pour voir le conseil intelligent.");
            return;
        }
        double amount = parsePreviewDouble(amountText);
        if (amount <= 0) {
            fundingAdviceLabel.setText("Le conseil apparaitra quand le montant sera valide.");
            return;
        }
        if ("Gain".equalsIgnoreCase(casTypeComboBox.getValue())) {
            double emergencyAfter = casReelService.calculateEmergencyFundBalance(currentUser.getId()) + amount;
            fundingAdviceLabel.setText(String.format(Locale.US,
                    "Cas positif: ce montant peut alimenter l'Emergency Fund. Solde projete: %.2f DT.",
                    emergencyAfter));
            return;
        }
        CaseFundingAdvice advice = casReelService.analyzeFundingChoice(currentUser.getId(), paymentMethodComboBox.getValue(), amount);
        fundingAdviceLabel.setText(advice.suggestion());
    }

    private void appliquerSolution(String solution) {
        CasRelles selected = casReelsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a case in My History first.");
            return;
        }
        selected.setSolution(solution);
        try {
            casReelService.modifier(selected);
            refreshCasReels();
            statusLabel.setText("Suggested handling saved: " + solution);
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void afficherDetails(CasRelles cas) {
        if (selectedCaseTitleLabel != null) selectedCaseTitleLabel.setText(cas.getTitre());
        if (selectedCaseSourceLabel != null) selectedCaseSourceLabel.setText(getSourceLabel(cas));
        if (selectedCaseDescriptionLabel != null) selectedCaseDescriptionLabel.setText(cas.getDescription() == null || cas.getDescription().isBlank() ? "No extra description." : cas.getDescription());
        if (selectedCaseEtatLabel != null) selectedCaseEtatLabel.setText(cas.getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : cas.getResultat());
        if (impactLabel != null) impactLabel.setText(formatMontant(cas.getMontant()));
    }

    private void afficherDetailsDepuisImprevu(Imprevus imprevu) {
        if (selectedCaseTitleLabel != null) selectedCaseTitleLabel.setText(imprevu.getTitre());
        if (selectedCaseSourceLabel != null) selectedCaseSourceLabel.setText("Selected event");
        if (selectedCaseDescriptionLabel != null) selectedCaseDescriptionLabel.setText("The budget of an event is the total amount of its linked real cases.");
        if (selectedCaseEtatLabel != null) selectedCaseEtatLabel.setText("-");
        if (impactLabel != null) impactLabel.setText(formatMontant(calculerBudgetImprevu(imprevu)));
    }

    private void resetDetails() {
        if (selectedCaseTitleLabel != null) selectedCaseTitleLabel.setText("Select an event or a case");
        if (selectedCaseSourceLabel != null) selectedCaseSourceLabel.setText("No current selection");
        if (selectedCaseDescriptionLabel != null) selectedCaseDescriptionLabel.setText("Choose an unexpected event for context, but create the real case independently.");
        if (selectedCaseEtatLabel != null) selectedCaseEtatLabel.setText("-");
        if (impactLabel != null) impactLabel.setText("0.00 DT");
    }

    private void clearCaseEditor() {
        casReelSelectionne = null;
        casTitreField.clear();
        casTypeComboBox.getSelectionModel().clearSelection();
        casDescriptionField.clear();
        casMontantField.clear();
        justificatifField.clear();
        paymentMethodComboBox.setValue(CasReelService.PAYMENT_EMERGENCY_FUND);
        fundingAdviceLabel.setText("Choisis une methode de paiement pour voir le conseil intelligent.");
        dateEffetPicker.setValue(LocalDate.now());
        saveCasButton.setText("Add case");
        resetDetails();
        updateSelectedEventHint();
        updateFundingControlsForType();
    }

    private void updateSelectedEventHint() {
        selectedEventHintLabel.setText(imprevuSelectionne == null ? "No unexpected event selected" : "Selected event: " + imprevuSelectionne.getTitre());
    }

    private double calculerBudgetImprevu(Imprevus imprevu) {
        if (imprevu == null) return 0;
        return allCasReelsList.stream()
                .filter(cas -> cas.getImprevus() != null && cas.getImprevus().getId() == imprevu.getId())
                .mapToDouble(CasRelles::getMontant)
                .sum();
    }

    private String getSourceLabel(CasRelles cas) {
        return cas.getImprevus() == null ? "Manual case" : cas.getImprevus().getTitre();
    }

    private String buildHistoryAuditLine(CasRelles cas) {
        String decision = cas.getConfirmedAt() == null ? "Pas encore traite" : "Traite le " + cas.getConfirmedAt().toLocalDate();
        String reason = cas.getRaisonRefus() == null || cas.getRaisonRefus().isBlank() ? "sans raison admin" : cas.getRaisonRefus();
        String proof = cas.getJustificatifFileName() == null || cas.getJustificatifFileName().isBlank() ? "preuve: -" : "preuve: " + cas.getJustificatifFileName();
        String advice = cas.getAiRefusalSuggestion() == null || cas.getAiRefusalSuggestion().isBlank() ? "" : " - conseil: " + cas.getAiRefusalSuggestion();
        return decision + " - " + reason + " - " + proof + advice;
    }

    private void updateRiskInsights() {
        if (futureRiskLabel == null) {
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        Arrays.asList("Voiture", "Sante", "Maison", "Electronique", "Education", "Factures", "Autres")
                .forEach(category -> counts.put(category, 0));

        LocalDate now = LocalDate.now();
        allCasReelsList.stream()
                .filter(cas -> cas.getDateEffet() != null && ChronoUnit.DAYS.between(cas.getDateEffet(), now) <= 30)
                .filter(cas -> "Depense".equalsIgnoreCase(cas.getType()))
                .forEach(cas -> counts.compute(inferRiskCategory(cas), (key, value) -> value == null ? 1 : value + 1));

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        int voiture = percent(counts.get("Voiture"), total);
        int sante = percent(counts.get("Sante"), total);
        int maison = percent(counts.get("Maison"), total);
        int electronique = percent(counts.get("Electronique"), total);
        int education = percent(counts.get("Education"), total);
        int factures = percent(counts.get("Factures"), total);
        int autres = percent(counts.get("Autres"), total);
        int overallScore = Math.min(100, total * 18);

        futureRiskLabel.setText((overallScore < 35 ? "Risque futur (30 jours): Faible" : overallScore < 70 ? "Risque futur (30 jours): Moyen" : "Risque futur (30 jours): Eleve")
                + " (" + overallScore + "/100)");
        voitureRiskLabel.setText("Voiture: " + voiture + "%");
        santeRiskLabel.setText("Sante: " + sante + "%");
        maisonRiskLabel.setText("Maison: " + maison + "%");
        electroniqueRiskLabel.setText("Electronique: " + electronique + "%");
        educationRiskLabel.setText("Education: " + education + "%");
        facturesRiskLabel.setText("Factures: " + factures + "%");
        autresRiskLabel.setText("Autres: " + autres + "%");

        String dominant = counts.entrySet().stream().max(Map.Entry.comparingByValue()).filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).orElse(null);
        if (dominant == null) {
            weeklyAdviceLabel.setText("Pas de risque fort detecte: continue le suivi hebdomadaire pour garder cette stabilite.");
            if (!hasMapsConfig()) {
                appointmentSuggestionLabel.setText("Rendez-vous suggere: aucun rendez-vous prioritaire pour le moment. Ajoute LOCATIONIQ_API_KEY dans .env pour activer les suggestions maps.");
            } else {
                appointmentSuggestionLabel.setText("Rendez-vous suggere: aucun rendez-vous prioritaire pour le moment.");
            }
            return;
        }

        weeklyAdviceLabel.setText(buildWeeklyAdvice(dominant, overallScore));
        appointmentSuggestionLabel.setText(buildAppointmentSuggestion(dominant));
        loadNearbySuggestions(dominant);
    }

    private void updateLatestNotification() {
        if (latestNotificationLabel == null) {
            return;
        }
        if (currentUser == null || currentUser.getId() <= 0) {
            latestNotificationLabel.setText("Notifications: connecte un utilisateur pour voir les alertes.");
            return;
        }
        userNotificationService.findLatestByUserId(currentUser.getId())
                .ifPresentOrElse(
                        notification -> latestNotificationLabel.setText("Notification recente: " + notification.getTitle() + " - " + notification.getMessage()),
                        () -> latestNotificationLabel.setText(hasMailConfig()
                                ? "Notifications: aucune notification pour le moment."
                                : "Notifications: actives dans l'app. Pour le mail, ajoute MAILER_DSN et MAILER_FROM_* dans .env.")
                );
    }

    private int percent(int count, int total) {
        return total <= 0 ? 0 : (int) Math.round((count * 100.0) / total);
    }

    private String inferRiskCategory(CasRelles cas) {
        return casReelService.inferRiskCategory(
                cas.getTitre(),
                cas.getDescription(),
                cas.getImprevus()
        );
    }

    private String buildWeeklyAdvice(String dominant, int score) {
        if ("Sante".equals(dominant)) {
            return score >= 35
                    ? "Conseils hebdomadaires: plusieurs signaux sante detectes. Garde un budget prevention et prevois un suivi medical regulier."
                    : "Conseils hebdomadaires: risque sante leger detecte. Continue la prevention et garde une reserve medicale.";
        }
        if ("Voiture".equals(dominant)) {
            return score >= 35
                    ? "Conseils hebdomadaires: plusieurs depenses voiture detectees. Planifie un entretien preventif et surveille les frais recurrents."
                    : "Conseils hebdomadaires: risque voiture leger detecte. Un controle mensuel peut eviter une panne plus lourde.";
        }
        return "Conseils hebdomadaires: le risque dominant est " + dominant + ". Continue le suivi hebdomadaire pour limiter les nouvelles occurrences.";
    }

    private String buildAppointmentSuggestion(String dominant) {
        String city = currentUser == null || currentUser.getGeoCityName() == null || currentUser.getGeoCityName().isBlank()
                ? "votre ville"
                : currentUser.getGeoCityName();
        if ("Sante".equals(dominant)) {
            return "Rendez-vous suggere: si plusieurs maladies reviennent, prevois un bilan mensuel et cherche un medecin proche de " + city + ".";
        }
        if ("Voiture".equals(dominant)) {
            return "Rendez-vous suggere: si plusieurs pannes voiture reviennent, planifie un entretien mensuel dans un garage proche de " + city + ".";
        }
        return "Rendez-vous suggere: pas de rendez-vous automatique fort, mais surveille les services proches de " + city + " si le risque augmente.";
    }

    private void loadNearbySuggestions(String dominant) {
        if (!hasMapsConfig()) {
            appointmentSuggestionLabel.setText(buildAppointmentSuggestion(dominant) + " Suggestions maps desactivees: ajoute LOCATIONIQ_API_KEY dans .env.");
            return;
        }
        if (currentUser == null || currentUser.getGeoCityName() == null || currentUser.getGeoCityName().isBlank()) {
            appointmentSuggestionLabel.setText(buildAppointmentSuggestion(dominant) + " Ajoute une ville utilisateur pour voir les lieux proches.");
            return;
        }
        String city = currentUser.getGeoCityName();
        appointmentSuggestionLabel.setText(buildAppointmentSuggestion(dominant) + " Chargement des lieux proches...");
        CompletableFuture
                .supplyAsync(() -> locationSuggestionService.suggestNearbyPlaces(dominant, city))
                .thenAccept(suggestions -> Platform.runLater(() -> applyNearbySuggestions(dominant, suggestions)));
    }

    private void applyNearbySuggestions(String dominant, List<String> suggestions) {
        String base = buildAppointmentSuggestion(dominant);
        if (suggestions == null || suggestions.isEmpty()) {
            appointmentSuggestionLabel.setText(base + " Aucun lieu suggere via maps pour le moment.");
            return;
        }
        appointmentSuggestionLabel.setText(base + " Lieux proches: " + String.join(" | ", suggestions));
    }

    private boolean hasMailConfig() {
        return AppEnv.has("MAILER_DSN") && AppEnv.has("MAILER_FROM_ADDRESS");
    }

    private boolean hasMapsConfig() {
        return AppEnv.has("LOCATIONIQ_API_KEY");
    }

    private boolean sameImprevu(Imprevus first, Imprevus second) {
        if (first == null || second == null) return first == null && second == null;
        return first.getId() == second.getId();
    }

    private double parsePositiveDouble(String raw, String fieldLabel) {
        try {
            double value = Double.parseDouble(raw.replace(',', '.'));
            if (value <= 0) {
                afficherErreur("Le " + fieldLabel + " doit etre strictement positif.");
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            afficherErreur("Le " + fieldLabel + " doit etre numerique.");
            return -1;
        }
    }

    private double parsePreviewDouble(String raw) {
        try {
            return Double.parseDouble(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String normalizeImprevuTitle(Imprevus imprevu) {
        return imprevu == null || imprevu.getTitre() == null ? "" : imprevu.getTitre().toLowerCase(Locale.ROOT);
    }

    private String normalizeCasTitle(CasRelles cas) {
        return cas == null || cas.getTitre() == null ? "" : cas.getTitre().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String safe(TextArea field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String formatMontant(double montant) {
        return String.format(Locale.US, "%.2f DT", montant);
    }

    private String formatSignedMontant(CasRelles cas) {
        double signed = "Depense".equalsIgnoreCase(cas.getType()) ? -Math.abs(cas.getMontant()) : Math.abs(cas.getMontant());
        return String.format(Locale.US, "%+.2f DT", signed).replace('+', ' ').trim();
    }

    private String resolveTreatedBy(CasRelles cas) {
        if (cas.getConfirmedBy() == null) {
            return "-";
        }
        if (cas.getConfirmedBy().getNom() != null && !cas.getConfirmedBy().getNom().isBlank()) {
            return cas.getConfirmedBy().getNom();
        }
        return blankToDash(cas.getConfirmedBy().getEmail());
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void scrollToNode(Node node) {
        if (pageScrollPane == null || node == null) return;
        Platform.runLater(() -> {
            Bounds contentBounds = pageScrollPane.getContent().getLayoutBounds();
            Bounds nodeBounds = node.getBoundsInParent();
            double viewportHeight = pageScrollPane.getViewportBounds().getHeight();
            double available = Math.max(contentBounds.getHeight() - viewportHeight, 1);
            pageScrollPane.setVvalue(Math.min(Math.max(nodeBounds.getMinY() / available, 0), 1));
        });
    }

    private void afficherErreur(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML private void handleOpenHome(ActionEvent event) { openPage(event, "/pi/mains/salary-home-view.fxml", "/pi/styles/salary-home.css", "Salary Home"); }
    @FXML private void handleOpenAbout(ActionEvent event) { openPage(event, "/pi/mains/about-view.fxml", "/pi/styles/about.css", "About Us"); }
    @FXML private void handleOpenService(ActionEvent event) { openPage(event, "/pi/mains/service-view.fxml", "/pi/styles/service.css", "Services"); }
    @FXML private void handleOpenIncomeExpenses(ActionEvent event) { openPage(event, "/Expense/Revenue/FRONT/salary-expense-view.fxml", null, "Income & Expense Management"); }
    @FXML private void handleOpenUnexpectedCases(ActionEvent event) { openPage(event, "/imprevus-view.fxml", "/styles/imprevus.css", "Unexpected Events & Real Cases"); }
    @FXML private void handleOpenContact(ActionEvent event) { openPage(event, "/pi/mains/contact-view.fxml", "/pi/styles/contact.css", "Contact"); }
    @FXML private void handleLogout(ActionEvent event) { openPage(event, "/pi/mains/login-view.fxml", "/pi/styles/login.css", "User Secure Login"); }

    @FXML
    private void handleOpenSavings(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setUserData(resolveUser(stage));
            new SavingsGoalsApp().start(stage);
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir Savings : " + e.getMessage());
        }
    }

    @FXML
    private void handleGoBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/back-office-view.fxml"));
            Scene scene = new Scene(loader.load(), 1500, 950);
            Stage stage = (Stage) imprevusListView.getScene().getWindow();
            stage.setUserData(currentUser);
            stage.setScene(scene);
            stage.setTitle("Back Office - Imprevus et Cas reels");
            stage.show();
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le back office : " + e.getMessage());
        }
    }

    private void openPage(ActionEvent event, String fxmlPath, String cssPath, String title) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            User user = resolveUser(stage);
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();
            injectUser(loader.getController(), user);
            Scene scene = new Scene(root, 1460, 900);
            if (cssPath != null) scene.getStylesheets().add(Main.class.getResource(cssPath).toExternalForm());
            stage.setUserData("/pi/mains/login-view.fxml".equals(fxmlPath) ? null : user);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir la page : " + e.getMessage());
        }
    }

    private User resolveUser(Stage stage) {
        Object userData = stage.getUserData();
        if (userData instanceof User user) currentUser = user;
        return currentUser;
    }

    private void injectUser(Object controller, User user) {
        if (user == null || controller == null) return;
        if (controller instanceof ImprevusFrontController c) c.setUser(user);
        else if (controller instanceof SalaryHomeController c) c.setUser(user);
        else if (controller instanceof AboutController c) c.setUser(user);
        else if (controller instanceof ServiceController c) c.setUser(user);
        else if (controller instanceof ContactController c) c.setUser(user);
        else if (controller instanceof SalaryExpenseController c) c.setUser(user);
    }
}
