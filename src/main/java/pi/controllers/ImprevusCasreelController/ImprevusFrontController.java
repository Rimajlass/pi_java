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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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
import pi.services.ImprevusCasreelService.CaseReportExporter;
import pi.services.ImprevusCasreelService.CaseFundingAdvice;
import pi.services.ImprevusCasreelService.CurrentLocationService;
import pi.services.ImprevusCasreelService.ImprevusService;
import pi.services.ImprevusCasreelService.LocationSuggestionService;
import pi.services.ImprevusCasreelService.AppointmentSuggestionService;
import pi.services.ImprevusCasreelService.PreventionPlanService;
import pi.services.ImprevusCasreelService.UserNotificationService;
import pi.savings.ui.SavingsGoalsApp;
import pi.tools.AppEnv;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Toolkit;
import java.awt.Image;
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
    @FXML private ComboBox<String> recommendedPlacesComboBox;
    @FXML private DatePicker dateEffetPicker;
    @FXML private Label statusLabel;
    @FXML private Label fundingAdviceLabel;
    @FXML private Label selectedCaseTitleLabel;
    @FXML private Label selectedCaseSourceLabel;
    @FXML private Label selectedCaseDescriptionLabel;
    @FXML private Label impactLabel;
    @FXML private Label selectedCaseEtatLabel;
    @FXML private Label selectedEventHintLabel;
    @FXML private Label eventListSummaryLabel;
    @FXML private Label historySummaryLabel;
    @FXML private Label editorModeLabel;
    @FXML private Label totalImprevusValueLabel;

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
    @FXML private VBox preventionPlanBox;
    @FXML private HBox appointmentActionRow;
    @FXML private Label preventionImmediateActionLabel;
    @FXML private Label preventionBudgetLabel;
    @FXML private Label preventionAdviceLabel;
    @FXML private Label appointmentSuggestionLabel;
    @FXML private Label nearbyPlacesLabel;
    @FXML private Label currentLocationLabel;
    @FXML private Button appointmentCalendarButton;
    @FXML private Hyperlink appointmentCalendarLink;
    @FXML private Hyperlink directionsLink;
    @FXML private ListView<Imprevus> imprevusListView;
    @FXML private HBox placesSelectionRow;
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
    private final AppointmentSuggestionService appointmentSuggestionService = new AppointmentSuggestionService();
    private final PreventionPlanService preventionPlanService = new PreventionPlanService();
    private final CurrentLocationService currentLocationService = new CurrentLocationService();
    private final ObservableList<Imprevus> imprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> casReelsList = FXCollections.observableArrayList();
    private final ObservableList<Imprevus> allImprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> allCasReelsList = FXCollections.observableArrayList();
    private Imprevus imprevuSelectionne;
    private CasRelles casReelSelectionne;
    private User currentUser;
    private Timeline notificationPoller;
    private int lastPoppedNotificationId = -1;
    private String currentAppointmentCalendarUrl;
    private String currentDirectionsUrl;
    private CurrentLocationService.CurrentLocation currentDetectedLocation;
    private boolean liveLocationRequestInProgress;

    public void setUser(User user) {
        this.currentUser = user;
        refreshFundingAdvicePreview();
        updateStats();
        updateRiskInsights();
        updateAppointmentInsights();
        updateLatestNotification();
        startNotificationPolling();
    }
    private void showLocalPcNotification(String title, String message) {
        try {
            if (!SystemTray.isSupported()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(title);
                    alert.setHeaderText(title);
                    alert.setContentText(message);
                    alert.show();
                });
                return;
            }

            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("");

            TrayIcon trayIcon = new TrayIcon(image, "Decide$");
            trayIcon.setImageAutoSize(true);

            tray.add(trayIcon);
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);

            new Thread(() -> {
                try {
                    Thread.sleep(6000);
                    tray.remove(trayIcon);
                } catch (Exception ignored) {}
            }).start();

        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(title);
                alert.setContentText(message);
                alert.show();
            });
        }
    }
    @FXML
    public void initialize() {
        casTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));
        casTypeComboBox.setVisibleRowCount(2);
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(
                CasReelService.PAYMENT_EMERGENCY_FUND,
                CasReelService.PAYMENT_SAVING_ACCOUNT
        ));
        paymentMethodComboBox.setVisibleRowCount(2);
        paymentMethodComboBox.setValue(CasReelService.PAYMENT_EMERGENCY_FUND);
        triImprevusComboBox.setItems(FXCollections.observableArrayList("Titre A-Z", "Titre Z-A"));
        triImprevusComboBox.setVisibleRowCount(2);
        triCasReelsComboBox.setItems(FXCollections.observableArrayList("Plus recent", "Plus ancien", "Titre A-Z", "Titre Z-A"));
        triCasReelsComboBox.setVisibleRowCount(4);
        triImprevusComboBox.setValue("Titre A-Z");
        triCasReelsComboBox.setValue("Plus recent");
        dateEffetPicker.setValue(LocalDate.now());
        if (recommendedPlacesComboBox != null) {
            recommendedPlacesComboBox.setVisibleRowCount(5);
            recommendedPlacesComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateDirectionsLink(newValue));
            recommendedPlacesComboBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : shortenPlaceLabel(item));
                }
            });
            recommendedPlacesComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : shortenPlaceLabel(item));
                }
            });
        }
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
        updateAppointmentLink(null);
        detectCurrentLocation(false);
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
        System.out.println("CHECK NOTIF...");

        if (currentUser != null) {
            System.out.println("USER ID = " + currentUser.getId());
        } else {
            System.out.println("USER IS NULL ❌");
        }

        if (currentUser == null || currentUser.getId() <= 0) {
            return;
        }
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
                showLocalPcNotification(
                        notification.getTitle(),
                        notification.getMessage()
                );
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
                Label subtitle = new Label(blankToDash(item.getType()));
                subtitle.getStyleClass().add("list-card-type");
                double signedBudget = calculerBudgetImprevu(item);
                Label budgetBadge = new Label(formatSignedAmount(signedBudget));
                String amountStyle = signedBudget >= 0 ? "amount-positive" : "amount-negative";
                budgetBadge.getStyleClass().add("list-card-budget");
                budgetBadge.getStyleClass().add(amountStyle);
                Label linkedCases = new Label(countLinkedCases(item) + " linked case" + (countLinkedCases(item) > 1 ? "s" : ""));
                linkedCases.getStyleClass().add("list-card-linked");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox topRow = new HBox(10, title, spacer, budgetBadge);
                topRow.getStyleClass().add("list-card-top-row");
                HBox bottomRow = new HBox(10, subtitle, linkedCases);
                bottomRow.getStyleClass().add("list-card-bottom-row");
                VBox box = new VBox(6, topRow, bottomRow);
                box.getStyleClass().add("list-card-box");
                box.getStyleClass().add(amountStyle);
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
        casReelsTableView.setFixedCellSize(54);
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
        configureHistoryColumns();
        casReelsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                casReelSelectionne = selected;
                afficherDetails(selected);
            } else {
                casReelSelectionne = null;
                updateAppointmentInsights();
            }
        });
        casReelsTableView.setRowFactory(table -> {
            TableRow<CasRelles> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    CasRelles item = row.getItem();
                    casReelSelectionne = item;
                    afficherDetails(item);
                    openEditPopup(item);
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
    private void handleShowStats(ActionEvent event) {
        openPage(event, "/stats-imprevus-view.fxml", "/styles/imprevus.css", "Statistiques");
    }
    @FXML
    private void handleExportPdfBilan() {
        try {
            Path exportDirectory = Paths.get("target", "exports", "imprevus");
            Path pdf = CaseReportExporter.writePdf(List.copyOf(allCasReelsList), buildCaseStats(), exportDirectory);
            statusLabel.setText("PDF bilan genere: " + pdf.toAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdf.toFile());
            }
        } catch (IOException e) {
            afficherErreur("Impossible de generer le PDF bilan : " + e.getMessage());
        }
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
        boolean creatingNewCase = casReelSelectionne == null;
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
                showStatus("Case updated successfully.", true);
            } else {
                CasReelService.CaseInsertOutcome outcome = casReelService.ajouter(cas);
                showStatus(outcome.userMessage(), true);
            }
            refreshCasReels();
            if (creatingNewCase) {
                suggestUrgentPlaceFlow(findMatchingRecentCase(cas));
            }
            clearCaseEditor();
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void handleClearCase() {
        clearCaseEditor();
        showStatus("Case editor cleared.", false);
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
        runPreservingScroll(() -> {
            allImprevusList.setAll(imprevusService.afficher());
            totalImprevusValueLabel.setText(String.valueOf(allImprevusList.size()));
            filtrerImprevus(searchImprevusField == null ? "" : searchImprevusField.getText());
        });
    }

    private void refreshCasReels() {
        runPreservingScroll(() -> {
            allCasReelsList.setAll(casReelService.afficher());
            updateStats();
            updateRiskInsights();
            updateLatestNotification();
            updateAppointmentInsights();
            filtrerCasReels(searchCasReelsField == null ? "" : searchCasReelsField.getText());
        });
    }

    private void updateStats() {
        long pending = allCasReelsList.stream().filter(cas -> {
            String r = cas.getResultat();
            return CasReelService.STATUT_EN_ATTENTE.equals(r) || CasReelService.STATUT_EN_ATTENTE_ALLOCATION.equals(r);
        }).count();
        long accepted = allCasReelsList.stream().filter(cas -> CasReelService.STATUT_ACCEPTE.equals(cas.getResultat())).count();

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
        saveCasButton.setText("Update case");
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
            if ("Gain".equalsIgnoreCase(type)) {
                CasReelService.GainAllocationDecision decision = casReelService.analyzeGainAllocation(currentUser.getId(), montant, paymentMethod);
                cas.setFinancialGoal(decision.targetGoal());
                cas.setAiRefusalSuggestion(decision.suggestion());
            } else {
                CaseFundingAdvice advice = casReelService.analyzeFundingChoice(currentUser.getId(), paymentMethod, montant);
                cas.setAiRefusalSuggestion(advice.suggestion());
            }
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
        if (eventListSummaryLabel != null) {
            eventListSummaryLabel.setText(imprevusList.size() + " events available");
        }
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
        if (historySummaryLabel != null) {
            historySummaryLabel.setText(casReelsList.size() + " cases shown");
        }
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
            CasReelService.GainAllocationDecision decision = casReelService.analyzeGainAllocation(currentUser.getId(), amount, paymentMethodComboBox.getValue());
            fundingAdviceLabel.setText(decision.suggestion());
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
        updateAppointmentInsights();
    }

    private void afficherDetailsDepuisImprevu(Imprevus imprevu) {
        if (selectedCaseTitleLabel != null) selectedCaseTitleLabel.setText(imprevu.getTitre());
        if (selectedCaseSourceLabel != null) selectedCaseSourceLabel.setText("Selected event");
        if (selectedCaseDescriptionLabel != null) selectedCaseDescriptionLabel.setText("The budget of an event is the total amount of its linked real cases.");
        if (selectedCaseEtatLabel != null) selectedCaseEtatLabel.setText("-");
        if (impactLabel != null) impactLabel.setText(formatMontant(calculerBudgetImprevu(imprevu)));
        updateAppointmentInsights();
    }

    private void resetDetails() {
        if (selectedCaseTitleLabel != null) selectedCaseTitleLabel.setText("Select an event or a case");
        if (selectedCaseSourceLabel != null) selectedCaseSourceLabel.setText("No current selection");
        if (selectedCaseDescriptionLabel != null) selectedCaseDescriptionLabel.setText("Choose an unexpected event for context, but create the real case independently.");
        if (selectedCaseEtatLabel != null) selectedCaseEtatLabel.setText("-");
        if (impactLabel != null) impactLabel.setText("0.00 DT");
        updateAppointmentInsights();
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
        setEditorMode(false);
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
                .mapToDouble(cas -> "Depense".equalsIgnoreCase(cas.getType()) ? -Math.abs(cas.getMontant()) : Math.abs(cas.getMontant()))
                .sum();
    }

    private int countLinkedCases(Imprevus imprevu) {
        if (imprevu == null) {
            return 0;
        }
        return (int) allCasReelsList.stream()
                .filter(cas -> cas.getImprevus() != null && cas.getImprevus().getId() == imprevu.getId())
                .count();
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
            updatePreventionPlan(null);
            return;
        }

        weeklyAdviceLabel.setText(buildWeeklyAdvice(dominant, overallScore));
        PreventionPlanService.PreventionPlan plan = preventionPlanService.buildPlan(
                List.copyOf(allCasReelsList),
                this::inferRiskCategory,
                resolveActiveCity()
        );
        updatePreventionPlan(plan);
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
                    ? "Conseils hebdomadaires: plusieurs signaux sante detectes. Cela signifie qu'un petit probleme risque de revenir sans suivi. Un rappel medical mensuel est donc conseille pour agir avant l'urgence."
                    : "Conseils hebdomadaires: risque sante leger detecte. Continue la prevention et garde une reserve medicale.";
        }
        if ("Voiture".equals(dominant)) {
            return score >= 35
                    ? "Conseils hebdomadaires: plusieurs depenses voiture detectees. Cela montre une repetition concrete, pas un hasard. Un rappel mensuel d'entretien est conseille pour prevenir la prochaine panne."
                    : "Conseils hebdomadaires: risque voiture leger detecte. Un controle mensuel peut eviter une panne plus lourde.";
        }
        if ("Maison".equals(dominant)) {
            return score >= 35
                    ? "Conseils hebdomadaires: plusieurs incidents maison detectes. Le systeme recommande un rappel mensuel de controle car la repetition montre un risque qui s'installe."
                    : "Conseils hebdomadaires: risque maison leger detecte. Un controle preventif regulier peut limiter les prochaines depenses.";
        }
        return "Conseils hebdomadaires: le risque dominant est " + dominant + ". Continue le suivi hebdomadaire pour limiter les nouvelles occurrences.";
    }

    private void updatePreventionPlan(PreventionPlanService.PreventionPlan plan) {
        if (preventionPlanBox == null) {
            return;
        }
        boolean visible = plan != null && plan.eligible();
        preventionPlanBox.setVisible(visible);
        preventionPlanBox.setManaged(visible);
        if (!visible) {
            return;
        }
        preventionImmediateActionLabel.setText("Action immediate\n" + plan.immediateAction());
        preventionBudgetLabel.setText("Budget conseille\n" + plan.suggestedBudget());
        preventionAdviceLabel.setText("Conseil\n" + plan.preventionAdvice());
    }

    private String buildAppointmentSuggestion(String dominant) {
        return appointmentSuggestionService.formatForUi(appointmentSuggestionService.suggest(dominant, currentUser));
    }

    private void updateAppointmentInsights() {
        if (appointmentSuggestionLabel == null && nearbyPlacesLabel == null) {
            return;
        }
        AppointmentContext context = resolveAppointmentContext();
        String recurringRisk = computeRecurringReminderRisk();
        if (context == null && recurringRisk == null) {
            if (appointmentSuggestionLabel != null) {
                appointmentSuggestionLabel.setText("Rendez-vous suggere: aucun rendez-vous prioritaire pour le moment.");
            }
            if (nearbyPlacesLabel != null) {
                nearbyPlacesLabel.setText("L'assistance lieux proches apparaitra seulement dans la popup d'urgence apres ajout d'un cas.");
            }
            updateAppointmentLink(null);
            return;
        }
        AppointmentSuggestionService.AppointmentSuggestion suggestion;
        if (recurringRisk != null) {
            suggestion = appointmentSuggestionService.suggest(
                    recurringRisk,
                    buildRecurringReminderCaseTitle(recurringRisk),
                    buildRecurringReminderReason(recurringRisk),
                    currentUser,
                    true,
                    resolveActiveCity()
            );
        } else {
            suggestion = appointmentSuggestionService.suggest(
                    context.riskCategory(),
                    context.caseTitle(),
                    context.caseDescription(),
                    currentUser,
                    false,
                    resolveActiveCity()
            );
        }
        if (appointmentSuggestionLabel != null) {
            appointmentSuggestionLabel.setText(appointmentSuggestionService.formatForUi(suggestion));
        }
        updateAppointmentLink(suggestion);
        loadNearbySuggestions(suggestion);
    }

    private void loadNearbySuggestions(AppointmentSuggestionService.AppointmentSuggestion suggestion) {
        if (nearbyPlacesLabel == null) {
            return;
        }
        String city = resolveActiveCity();
        Double latitude = currentDetectedLocation == null ? null : currentDetectedLocation.latitude();
        Double longitude = currentDetectedLocation == null ? null : currentDetectedLocation.longitude();
        if ((city == null || city.isBlank()) && (latitude == null || longitude == null)) {
            nearbyPlacesLabel.setText(appointmentSuggestionService.formatPlaceTypesForUi(suggestion)
                    + " Active la localisation ou ajoute une ville utilisateur pour voir les meilleures adresses.");
            updateRecommendedPlaces(List.of());
            return;
        }
        nearbyPlacesLabel.setText(appointmentSuggestionService.formatPlaceTypesForUi(suggestion)
                + " Chargement des meilleures adresses...");
        CompletableFuture
                .supplyAsync(() -> locationSuggestionService.suggestNearbyPlacesForNeeds(suggestion.placeTypesNeeded(), city, latitude, longitude))
                .orTimeout(20, TimeUnit.SECONDS)
                .exceptionally(error -> List.of())
                .thenAccept(suggestions -> Platform.runLater(() -> applyNearbySuggestions(suggestion, suggestions)));
    }

    private void applyNearbySuggestions(AppointmentSuggestionService.AppointmentSuggestion suggestion, List<String> suggestions) {
        if (nearbyPlacesLabel == null) {
            return;
        }
        if (suggestions == null || suggestions.isEmpty()) {
            List<String> fallbackPlaces = locationSuggestionService.suggestFallbackPlacesForNeeds(suggestion.placeTypesNeeded(), resolveActiveCity());
            nearbyPlacesLabel.setText(appointmentSuggestionService.formatPlaceTypesForUi(suggestion)
                    + " Resultats maps limites pour le moment, mais voici des recherches utiles a ouvrir.");
            updateRecommendedPlaces(fallbackPlaces);
            return;
        }
        nearbyPlacesLabel.setText(appointmentSuggestionService.formatPlaceTypesForUi(suggestion)
                + " 5 options utiles sont pretes. Choisis-en une pour ouvrir le trajet dans Google Maps.");
        updateRecommendedPlaces(suggestions);
    }

    private AppointmentContext resolveAppointmentContext() {
        if (casReelSelectionne != null) {
            return new AppointmentContext(
                    casReelSelectionne.getTitre(),
                    casReelSelectionne.getDescription(),
                    inferRiskCategory(casReelSelectionne)
            );
        }

        CasRelles latestExpense = allCasReelsList.stream()
                .filter(cas -> "Depense".equalsIgnoreCase(cas.getType()))
                .max(Comparator.comparing(CasRelles::getDateEffet, Comparator.nullsLast(LocalDate::compareTo)))
                .orElse(null);
        if (latestExpense != null) {
            return new AppointmentContext(
                    latestExpense.getTitre(),
                    latestExpense.getDescription(),
                    inferRiskCategory(latestExpense)
            );
        }

        String dominant = computeDominantRisk();
        if (dominant == null) {
            return null;
        }
        return new AppointmentContext("risque " + dominant, weeklyAdviceLabel == null ? "" : weeklyAdviceLabel.getText(), dominant);
    }

    private String computeDominantRisk() {
        return allCasReelsList.stream()
                .filter(cas -> "Depense".equalsIgnoreCase(cas.getType()))
                .collect(java.util.stream.Collectors.groupingBy(this::inferRiskCategory, LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private CaseReportExporter.CaseStats buildCaseStats() {
        int total = allCasReelsList.size();
        int gains = (int) allCasReelsList.stream().filter(cas -> "Gain".equalsIgnoreCase(cas.getType())).count();
        int depenses = (int) allCasReelsList.stream().filter(cas -> "Depense".equalsIgnoreCase(cas.getType())).count();
        int pending = (int) allCasReelsList.stream().filter(cas -> {
            String status = cas.getResultat();
            return CasReelService.STATUT_EN_ATTENTE.equals(status) || CasReelService.STATUT_EN_ATTENTE_ALLOCATION.equals(status);
        }).count();
        int accepted = (int) allCasReelsList.stream().filter(cas -> CasReelService.STATUT_ACCEPTE.equals(cas.getResultat())).count();
        double totalGain = allCasReelsList.stream()
                .filter(cas -> "Gain".equalsIgnoreCase(cas.getType()))
                .mapToDouble(CasRelles::getMontant)
                .sum();
        double totalExpense = allCasReelsList.stream()
                .filter(cas -> "Depense".equalsIgnoreCase(cas.getType()))
                .mapToDouble(CasRelles::getMontant)
                .sum();
        String dominantRisk = computeDominantRisk();
        return new CaseReportExporter.CaseStats(
                total,
                gains,
                depenses,
                pending,
                accepted,
                totalGain,
                totalExpense,
                dominantRisk == null ? "-" : dominantRisk
        );
    }

    private void updateAppointmentLink(AppointmentSuggestionService.AppointmentSuggestion suggestion) {
        currentAppointmentCalendarUrl = suggestion == null ? null : suggestion.calendarUrl();
        if (appointmentCalendarLink == null && appointmentCalendarButton == null) {
            return;
        }
        boolean active = suggestion != null
                && suggestion.recurringMonthly()
                && currentAppointmentCalendarUrl != null
                && !currentAppointmentCalendarUrl.isBlank();
        String actionText = suggestion != null && suggestion.recurringMonthly()
                ? "Programmer un controle mensuel"
                : "Ajouter ce rendez-vous au calendrier";
        if (appointmentActionRow != null) {
            appointmentActionRow.setVisible(active);
            appointmentActionRow.setManaged(active);
        }
        if (appointmentCalendarLink != null) {
            appointmentCalendarLink.setVisible(active);
            appointmentCalendarLink.setManaged(active);
            appointmentCalendarLink.setDisable(!active);
            appointmentCalendarLink.setText(actionText);
        }
        if (appointmentCalendarButton != null) {
            appointmentCalendarButton.setVisible(active);
            appointmentCalendarButton.setManaged(active);
            appointmentCalendarButton.setDisable(!active);
            appointmentCalendarButton.setText(actionText);
        }
    }

    @FXML
    private void handleOpenAppointmentCalendar() {
        if (currentAppointmentCalendarUrl == null || currentAppointmentCalendarUrl.isBlank()) {
            statusLabel.setText("Aucun rendez-vous calendrier disponible pour le moment.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(currentAppointmentCalendarUrl));
            }
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le calendrier : " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenDirections() {
        if (currentDirectionsUrl == null || currentDirectionsUrl.isBlank()) {
            statusLabel.setText("Choisis un lieu recommande pour voir le chemin.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(currentDirectionsUrl));
            }
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le trajet : " + e.getMessage());
        }
    }

    @FXML
    private void handleUseCurrentLocation() {
        detectCurrentLocation(true);
    }

    private boolean hasMailConfig() {
        return AppEnv.has("MAILER_DSN") && AppEnv.has("MAILER_FROM_ADDRESS");
    }

    private boolean hasMapsConfig() {
        return AppEnv.has("LOCATIONIQ_API_KEY");
    }

    private void updateRecommendedPlaces(List<String> places) {
        if (recommendedPlacesComboBox == null || placesSelectionRow == null) {
            return;
        }
        recommendedPlacesComboBox.getItems().setAll(places);
        boolean hasPlaces = places != null && !places.isEmpty();
        placesSelectionRow.setManaged(hasPlaces);
        placesSelectionRow.setVisible(hasPlaces);
        if (hasPlaces) {
            recommendedPlacesComboBox.getSelectionModel().selectFirst();
            updateDirectionsLink(recommendedPlacesComboBox.getValue());
        } else {
            recommendedPlacesComboBox.getSelectionModel().clearSelection();
            updateDirectionsLink(null);
        }
    }

    private void updateDirectionsLink(String place) {
        currentDirectionsUrl = null;
        if (directionsLink == null) {
            return;
        }
        currentDirectionsUrl = locationSuggestionService.buildDirectionsUrl(
                currentDetectedLocation == null ? null : currentDetectedLocation.latitude(),
                currentDetectedLocation == null ? null : currentDetectedLocation.longitude(),
                resolveActiveCity(),
                place
        );
        boolean active = currentDirectionsUrl != null && !currentDirectionsUrl.isBlank();
        directionsLink.setManaged(active);
        directionsLink.setVisible(active);
        directionsLink.setDisable(!active);
    }

    private boolean isMonthlyRecurringRisk(String riskCategory) {
        if (riskCategory == null || (!"Sante".equals(riskCategory) && !"Voiture".equals(riskCategory) && !"Maison".equals(riskCategory))) {
            return false;
        }
        long count = allCasReelsList.stream()
                .filter(cas -> "Depense".equalsIgnoreCase(cas.getType()))
                .filter(cas -> cas.getDateEffet() != null && !cas.getDateEffet().isBefore(LocalDate.now().minusDays(180)))
                .filter(cas -> riskCategory.equals(inferRiskCategory(cas)))
                .count();
        return count >= 2;
    }

    private void detectCurrentLocation(boolean preferBrowserGps) {
        liveLocationRequestInProgress = preferBrowserGps;
        if (currentLocationLabel != null) {
            currentLocationLabel.setText(preferBrowserGps
                    ? "Current location: requesting live location..."
                    : "Current location: detecting automatically...");
        }
        CompletableFuture<CurrentLocationService.CurrentLocation> future = preferBrowserGps
                ? currentLocationService.detectWithBrowserGpsOrIpAsync(pageScrollPane == null ? null : pageScrollPane.getScene().getWindow())
                : currentLocationService.detectByIpAsync();
        future.thenAccept(location -> Platform.runLater(() -> applyDetectedLocation(location)));
    }

    private void applyDetectedLocation(CurrentLocationService.CurrentLocation location) {
        currentDetectedLocation = location;
        if (currentLocationLabel != null) {
            if (location == null) {
                currentLocationLabel.setText("Current location: unavailable");
            } else {
                String coordinates = location.hasCoordinates()
                        ? String.format(Locale.US, " [%.5f, %.5f]", location.latitude(), location.longitude())
                        : "";
                currentLocationLabel.setText("Current location: " + location.city() + " (" + location.source() + ")" + coordinates);
            }
        }
        if (location != null && liveLocationRequestInProgress && location.hasCoordinates()) {
            showStatus("Live location received: " + location.city() + ".", true);
            showInfoPopup("Location updated", "Live location received for " + location.city() + ".");
        }
        if (liveLocationRequestInProgress && preferLiveLocationRequestedButUnavailable(location)) {
            showStatus("Windows/browser live location was unavailable. The app used fallback detection instead.", false);
        }
        liveLocationRequestInProgress = false;
        updateAppointmentInsights();
    }

    private String resolveActiveCity() {
        if (currentDetectedLocation != null && currentDetectedLocation.city() != null && !currentDetectedLocation.city().isBlank()) {
            return currentDetectedLocation.city();
        }
        if (currentUser != null && currentUser.getGeoCityName() != null && !currentUser.getGeoCityName().isBlank()) {
            return currentUser.getGeoCityName();
        }
        return null;
    }

    private boolean preferLiveLocationRequestedButUnavailable(CurrentLocationService.CurrentLocation location) {
        if (location == null || location.source() == null) {
            return false;
        }
        return "IP location".equals(location.source()) || "Default city".equals(location.source()) || "Windows live location unavailable".equals(location.source());
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

    private String formatSignedAmount(double montant) {
        return String.format(Locale.US, "%+.2f DT", montant);
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

    private record UrgentAssistanceData(AppointmentSuggestionService.AppointmentSuggestion suggestion, List<String> places) {
    }

    private void configureHistoryColumns() {
        historyDateColumn.setPrefWidth(92);
        historyTitleColumn.setPrefWidth(210);
        historyTypeColumn.setPrefWidth(82);
        historyAmountColumn.setPrefWidth(108);
        historySolutionColumn.setPrefWidth(126);
        historyStatusColumn.setPrefWidth(118);
        historyJustificatifColumn.setPrefWidth(90);

        historyCategoryColumn.setVisible(false);
        historyTreatedByColumn.setVisible(false);
        historyTreatedAtColumn.setVisible(false);
        historyRefusalReasonColumn.setVisible(false);

        historyTitleColumn.setCellFactory(column -> new TableCell<>() {
            private final Label title = new Label();
            private final Label subtitle = new Label();
            private final VBox box = new VBox(2, title, subtitle);
            {
                box.getStyleClass().add("history-title-cell");
                title.getStyleClass().add("history-main-title");
                subtitle.getStyleClass().add("history-subtitle");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                CasRelles cas = getTableRow().getItem();
                title.setText(blankToDash(item));
                subtitle.setText(getSourceLabel(cas));
                setGraphic(box);
            }
        });

        historyStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(formatStatus(item));
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add(statusStyleClass(item));
                setGraphic(badge);
            }
        });

        historyTypeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : capitalize(item));
            }
        });

        historyJustificatifColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : ("-".equals(item) ? "No file" : "Attached"));
            }
        });
    }

    private void replaceCaseInMemory(CasRelles updated) {
        replaceCaseInList(allCasReelsList, updated);
        replaceCaseInList(casReelsList, updated);
    }

    private void replaceCaseInList(ObservableList<CasRelles> list, CasRelles updated) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == updated.getId()) {
                list.set(i, updated);
                break;
            }
        }
    }

    private void selectCaseInTable(int caseId) {
        if (casReelsTableView == null) {
            return;
        }
        for (CasRelles cas : casReelsTableView.getItems()) {
            if (cas.getId() == caseId) {
                casReelsTableView.getSelectionModel().select(cas);
                break;
            }
        }
    }

    private void setEditorMode(boolean editing) {
        if (saveCasButton != null) {
            saveCasButton.setText(editing ? "Update case" : "Add case");
        }
        if (editorModeLabel != null) {
            editorModeLabel.setText(editing ? "Edit mode" : "Create mode");
            editorModeLabel.getStyleClass().remove("editing");
            if (editing) {
                editorModeLabel.getStyleClass().add("editing");
            }
        }
    }

    private void showStatus(String message, boolean success) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("selected-event-pill", "advisor-pill");
        statusLabel.getStyleClass().add(success ? "selected-event-pill" : "advisor-pill");
    }

    private void showInfoPopup(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void suggestUrgentPlaceFlow(CasRelles cas) {
        if (cas == null || !"Depense".equalsIgnoreCase(cas.getType())) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Urgent assistance");
        alert.setHeaderText("Do you want urgent help for this case?");
        alert.setContentText("""
                The case was saved.
                
                If this situation is urgent, the app can now:
                1. suggest the most relevant nearby places,
                2. show the monthly reminder if the issue repeats,
                3. open the route in Google Maps.
                
                Choose OK to open this help now, or Cancel to ignore it.
                """);
        alert.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                showStatus("Preparation de l'assistance urgente...", false);
                openUrgentAssistanceDialog(cas);
            }
        });
    }

    private CasRelles findMatchingRecentCase(CasRelles template) {
        if (template == null) {
            return null;
        }
        return allCasReelsList.stream()
                .filter(existing -> normalize(existing.getTitre()).equals(normalize(template.getTitre())))
                .filter(existing -> normalize(existing.getType()).equals(normalize(template.getType())))
                .filter(existing -> existing.getDateEffet() != null && existing.getDateEffet().equals(template.getDateEffet()))
                .filter(existing -> Double.compare(existing.getMontant(), template.getMontant()) == 0)
                .max(Comparator.comparing(CasRelles::getId))
                .orElse(null);
    }

    private String shortenPlaceLabel(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace(" | ", " - ");
        return cleaned.length() <= 72 ? cleaned : cleaned.substring(0, 69) + "...";
    }

    private void openUrgentAssistanceDialog(CasRelles cas) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assistance urgente");
        dialog.setHeaderText("Agir maintenant");
        final UrgentAssistanceData[] loadedData = new UrgentAssistanceData[1];

        ButtonType routeButton = new ButtonType("Voir le chemin", ButtonBar.ButtonData.APPLY);
        ButtonType reminderButton = new ButtonType("Ajouter rappel mensuel", ButtonBar.ButtonData.OTHER);
        ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().setAll(closeButton, routeButton, reminderButton);

        Label intro = new Label("Analyse du cas en cours...");
        intro.setWrapText(true);

        Label placesLabel = new Label("Chargement des meilleures adresses proches...");
        placesLabel.setWrapText(true);

        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(42, 42);

        ComboBox<String> placesCombo = new ComboBox<>();
        placesCombo.setMaxWidth(Double.MAX_VALUE);
        placesCombo.setVisible(false);
        placesCombo.setManaged(false);
        placesCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : shortenPlaceLabel(item));
            }
        });
        placesCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : shortenPlaceLabel(item));
            }
        });

        VBox content = new VBox(12, intro, placesLabel, loadingIndicator, placesCombo);
        content.setFillWidth(true);
        dialog.getDialogPane().setContent(content);

        Node routeNode = dialog.getDialogPane().lookupButton(routeButton);
        Node reminderNode = dialog.getDialogPane().lookupButton(reminderButton);
        if (routeNode != null) {
            routeNode.setDisable(true);
        }
        if (reminderNode != null) {
            reminderNode.setDisable(true);
        }

        String risk = inferRiskCategory(cas);
        String city = resolveActiveCity();
        Double latitude = currentDetectedLocation == null ? null : currentDetectedLocation.latitude();
        Double longitude = currentDetectedLocation == null ? null : currentDetectedLocation.longitude();
        boolean recurringMonthly = isMonthlyRecurringRisk(risk);
        AppointmentSuggestionService.AppointmentSuggestion initialSuggestion = appointmentSuggestionService.suggest(
                risk,
                recurringMonthly ? buildRecurringReminderCaseTitle(risk) : cas.getTitre(),
                recurringMonthly ? buildRecurringReminderReason(risk) : cas.getDescription(),
                currentUser,
                recurringMonthly,
                city
        );
        List<String> fallbackPlaces = locationSuggestionService.suggestFallbackPlacesForNeeds(initialSuggestion.placeTypesNeeded(), city);
        loadedData[0] = new UrgentAssistanceData(initialSuggestion, fallbackPlaces);
        applyUrgentAssistanceData(dialog, routeNode, reminderNode, intro, placesLabel, placesCombo, loadedData[0]);
        placesLabel.setText(fallbackPlaces.isEmpty()
                ? "Recherche des adresses proches en cours..."
                : "Des choix utiles sont deja prets. Recherche des lieux reels proches en cours...");
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);

        CompletableFuture
                .supplyAsync(() -> {
                    AppointmentSuggestionService.AppointmentSuggestion suggestion = initialSuggestion;
                    List<String> places = locationSuggestionService.suggestNearbyPlacesForNeeds(
                            suggestion.placeTypesNeeded(), city, latitude, longitude
                    );
                    if (places == null || places.isEmpty()) {
                        places = fallbackPlaces;
                    }
                    return new UrgentAssistanceData(suggestion, places);
                })
                .orTimeout(12, TimeUnit.SECONDS)
                .exceptionally(error -> loadedData[0])
                .thenAccept(data -> Platform.runLater(() -> {
                    loadedData[0] = data;
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                    applyUrgentAssistanceData(dialog, routeNode, reminderNode, intro, placesLabel, placesCombo, data);
                }));

        dialog.showAndWait().ifPresent(button -> {
            if (button == routeButton) {
                openDirectionsForPlace(placesCombo.getValue());
            } else if (button == reminderButton && loadedData[0] != null && loadedData[0].suggestion() != null) {
                currentAppointmentCalendarUrl = loadedData[0].suggestion().calendarUrl();
                handleOpenAppointmentCalendar();
            }
        });
    }

    private void applyUrgentAssistanceData(Dialog<ButtonType> dialog,
                                           Node routeNode,
                                           Node reminderNode,
                                           Label intro,
                                           Label placesLabel,
                                           ComboBox<String> placesCombo,
                                           UrgentAssistanceData data) {
        if (data == null || data.suggestion() == null) {
            intro.setText("L'assistance urgente n'a pas pu preparer les informations pour ce cas.");
            placesLabel.setText("Reessaie dans un instant apres avoir active la localisation.");
            if (routeNode != null) {
                routeNode.setDisable(true);
            }
            if (reminderNode != null) {
                reminderNode.setDisable(true);
            }
            return;
        }

        intro.setText(data.suggestion().description() + "\nPourquoi: " + data.suggestion().reason());

        boolean hasPlaces = data.places() != null && !data.places().isEmpty();
        placesCombo.getItems().setAll(hasPlaces ? data.places() : List.of());
        if (hasPlaces) {
            placesCombo.getSelectionModel().selectFirst();
        }
        placesCombo.setVisible(hasPlaces);
        placesCombo.setManaged(hasPlaces);
        placesLabel.setText(hasPlaces
                ? "Choisis un lieu proche utile pour agir maintenant."
                : "Aucun lieu proche n'a ete trouve pour le moment.");

        if (routeNode != null) {
            routeNode.setDisable(!hasPlaces);
            routeNode.setVisible(hasPlaces);
            routeNode.setManaged(hasPlaces);
        }

        boolean showReminder = data.suggestion().recurringMonthly();
        if (reminderNode != null) {
            reminderNode.setDisable(!showReminder);
            reminderNode.setVisible(showReminder);
            reminderNode.setManaged(showReminder);
        }

        dialog.getDialogPane().requestLayout();
        showStatus("Assistance urgente prete.", true);
    }

    private void openDirectionsForPlace(String place) {
        if (place == null || place.isBlank()) {
            showStatus("Aucun lieu selectionne pour le trajet.", false);
            return;
        }
        String url = locationSuggestionService.buildDirectionsUrl(
                currentDetectedLocation == null ? null : currentDetectedLocation.latitude(),
                currentDetectedLocation == null ? null : currentDetectedLocation.longitude(),
                resolveActiveCity(),
                place
        );
        if (url == null || url.isBlank()) {
            showStatus("Impossible de construire le trajet pour ce lieu.", false);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le trajet : " + e.getMessage());
        }
    }

    private String computeRecurringReminderRisk() {
        return List.of("Sante", "Voiture", "Maison").stream()
                .filter(this::isMonthlyRecurringRisk)
                .max(Comparator.comparingLong(this::countRecentRiskCases))
                .orElse(null);
    }

    private long countRecentRiskCases(String riskCategory) {
        return allCasReelsList.stream()
                .filter(cas -> "Depense".equalsIgnoreCase(cas.getType()))
                .filter(cas -> cas.getDateEffet() != null && !cas.getDateEffet().isBefore(LocalDate.now().minusDays(180)))
                .filter(cas -> riskCategory.equals(inferRiskCategory(cas)))
                .count();
    }

    private String buildRecurringReminderCaseTitle(String riskCategory) {
        return "Cas repetitif: " + riskCategory;
    }

    private String buildRecurringReminderReason(String riskCategory) {
        long count = countRecentRiskCases(riskCategory);
        return "Le conseil hebdomadaire a detecte " + count + " cas repetitifs de type " + riskCategory
                + " sur les derniers mois. Ce rappel mensuel est ajoute pour aider l'utilisateur a prevenir le prochain incident avant qu'il ne devienne urgent.";
    }

    private void openEditPopup(CasRelles cas) {
        if (cas == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Case");
        dialog.setHeaderText("Update the selected real case");

        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.LEFT);

        dialog.getDialogPane().getButtonTypes().addAll(
                deleteButtonType,
                ButtonType.CANCEL,
                ButtonType.OK
        );

        TextField titleField = new TextField(cas.getTitre());
        titleField.setPromptText("Case title");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Depense", "Gain"));
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setValue(cas.getType());

        TextField amountField = new TextField(String.valueOf(cas.getMontant()));
        amountField.setPromptText("Amount (DT)");
        amountField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,7}([\\.,]\\d{0,2})?") ? change : null
        ));

        ComboBox<String> paymentCombo = new ComboBox<>(FXCollections.observableArrayList(
                CasReelService.PAYMENT_EMERGENCY_FUND,
                CasReelService.PAYMENT_SAVING_ACCOUNT
        ));
        paymentCombo.setMaxWidth(Double.MAX_VALUE);
        paymentCombo.setValue(cas.getPaymentMethod() == null
                ? CasReelService.PAYMENT_EMERGENCY_FUND
                : cas.getPaymentMethod());

        DatePicker datePicker = new DatePicker(cas.getDateEffet());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextArea descriptionArea = new TextArea(cas.getDescription());
        descriptionArea.setPromptText("Update the case description");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(5);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);
        grid.add(titleField, 0, 0, 2, 1);
        grid.add(typeCombo, 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(paymentCombo, 0, 2, 2, 1);
        grid.add(datePicker, 0, 3, 2, 1);
        grid.add(descriptionArea, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(560);

        Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
        deleteButton.getStyleClass().add("danger-action-button");

        deleteButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer ce cas ?");
            confirm.setContentText("Cette action est irréversible.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        casReelService.supprimer(cas.getId());

                        casReelsList.removeIf(c -> c.getId() == cas.getId());
                        allCasReelsList.removeIf(c -> c.getId() == cas.getId());

                        casReelSelectionne = null;

                        refreshCasReels();
                        clearCaseEditor();

                        updateStats();
                        updateRiskInsights();
                        updateLatestNotification();
                        updateAppointmentInsights();

                        showStatus("Cas supprimé avec succès.", true);
                        dialog.close();

                    } catch (RuntimeException e) {
                        afficherErreur("Impossible de supprimer le cas : " + e.getMessage());
                    }
                }
            });
        });

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            CasRelles updated = buildUpdatedCaseFromPopup(
                    cas,
                    titleField,
                    typeCombo,
                    amountField,
                    paymentCombo,
                    datePicker,
                    descriptionArea
            );

            if (updated == null) {
                event.consume();
                return;
            }

            try {
                casReelService.modifier(updated);
                replaceCaseInMemory(updated);

                casReelSelectionne = updated;

                filtrerCasReels(searchCasReelsField == null ? "" : searchCasReelsField.getText());
                selectCaseInTable(updated.getId());

                updateStats();
                updateRiskInsights();
                updateLatestNotification();
                updateAppointmentInsights();

                showStatus("Case updated successfully.", true);
                showInfoPopup("Update completed", "The case was updated successfully.");

            } catch (RuntimeException e) {
                afficherErreur(e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private CasRelles buildUpdatedCaseFromPopup(
            CasRelles original,
            TextField titleField,
            ComboBox<String> typeCombo,
            TextField amountField,
            ComboBox<String> paymentCombo,
            DatePicker datePicker,
            TextArea descriptionArea
    ) {
        String titre = titleField.getText() == null ? "" : titleField.getText().trim();
        String type = typeCombo.getValue();
        String montantText = amountField.getText() == null ? "" : amountField.getText().trim();
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        String paymentMethod = paymentCombo.getValue();
        LocalDate dateEffet = datePicker.getValue();

        if (titre.length() < 3 || type == null || montantText.isBlank() || dateEffet == null || paymentMethod == null) {
            afficherErreur("Titre, type, montant, date et methode de paiement sont obligatoires.");
            return null;
        }

        double montant = parsePositiveDouble(montantText, "montant du cas reel");
        if (montant <= 0) {
            return null;
        }

        CasRelles updated = new CasRelles(
                original.getImprevus(),
                titre,
                description,
                type,
                original.getCategorie(),
                montant,
                original.getSolution(),
                dateEffet,
                original.getJustificatifFileName()
        );

        updated.setId(original.getId());
        updated.setImprevus(original.getImprevus());
        updated.setUser(original.getUser());
        updated.setResultat(original.getResultat());
        updated.setRaisonRefus(original.getRaisonRefus());
        updated.setConfirmedAt(original.getConfirmedAt());
        updated.setConfirmedBy(original.getConfirmedBy());
        updated.setFinancialGoal(original.getFinancialGoal());
        updated.setJustificatifFileName(original.getJustificatifFileName());
        updated.setAdminNote(original.getAdminNote());
        updated.setNotificationSentAt(original.getNotificationSentAt());
        updated.setSuppressDecisionEmail(original.isSuppressDecisionEmail());
        updated.setPaymentMethod(paymentMethod);
        updated.setAiRefusalSuggestion(original.getAiRefusalSuggestion());

        return updated;
    }

    private void runPreservingScroll(Runnable action) {
        double currentScroll = pageScrollPane == null ? 0 : pageScrollPane.getVvalue();
        action.run();
        if (pageScrollPane != null) {
            Platform.runLater(() -> pageScrollPane.setVvalue(currentScroll));
        }
    }

    private String formatStatus(String status) {
        return switch (status) {
            case CasReelService.STATUT_ACCEPTE -> "Accepted";
            case CasReelService.STATUT_REFUSE -> "Rejected";
            case CasReelService.STATUT_EN_ATTENTE_ALLOCATION -> "To review";
            default -> "Pending";
        };
    }

    private String statusStyleClass(String status) {
        return switch (status) {
            case CasReelService.STATUT_ACCEPTE -> "accepted";
            case CasReelService.STATUT_REFUSE -> "rejected";
            case CasReelService.STATUT_EN_ATTENTE_ALLOCATION -> "allocation";
            default -> "pending";
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private record AppointmentContext(String caseTitle, String caseDescription, String riskCategory) {
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
