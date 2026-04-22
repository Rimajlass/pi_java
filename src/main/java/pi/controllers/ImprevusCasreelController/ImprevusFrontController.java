package pi.controllers.ImprevusCasreelController;

import javafx.application.Platform;
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
import pi.services.ImprevusCasreelService.ImprevusService;
import pi.savings.ui.SavingsGoalsApp;

import java.io.File;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;

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
    @FXML private DatePicker dateEffetPicker;
    @FXML private Label statusLabel;
    @FXML private Label selectedCaseTitleLabel;
    @FXML private Label selectedCaseSourceLabel;
    @FXML private Label selectedCaseDescriptionLabel;
    @FXML private Label impactLabel;
    @FXML private Label selectedCaseEtatLabel;
    @FXML private Label selectedEventHintLabel;
    @FXML private Label totalImprevusValueLabel;
    @FXML private Label pendingCasesValueLabel;
    @FXML private Label acceptedCasesValueLabel;
    @FXML private ListView<Imprevus> imprevusListView;
    @FXML private ListView<CasRelles> casReelsListView;
    @FXML private Button saveCasButton;

    private final ImprevusService imprevusService = new ImprevusService();
    private final CasReelService casReelService = new CasReelService();
    private final ObservableList<Imprevus> imprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> casReelsList = FXCollections.observableArrayList();
    private final ObservableList<Imprevus> allImprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> allCasReelsList = FXCollections.observableArrayList();
    private Imprevus imprevuSelectionne;
    private CasRelles casReelSelectionne;
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void initialize() {
        casTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));
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
        refreshImprevus();
        refreshCasReels();
        resetDetails();
        updateSelectedEventHint();
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

        casReelsListView.setItems(casReelsList);
        casReelsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CasRelles item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label title = new Label(item.getTitre());
                title.getStyleClass().add("list-card-title");
                Label meta = new Label(getSourceLabel(item) + " - " + item.getType() + " - " + formatMontant(item.getMontant()));
                meta.getStyleClass().add("list-card-subtitle");
                Label state = new Label(item.getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : item.getResultat());
                state.getStyleClass().add("status-badge");
                Button editButton = new Button("Modifier");
                editButton.getStyleClass().add("mini-outline-button");
                editButton.setOnAction(event -> {
                    casReelSelectionne = item;
                    remplirFormulaireDepuisCasReel(item);
                    afficherDetails(item);
                    saveCasButton.setText("Save changes");
                    statusLabel.setText("Case loaded from My History. Update the form, then save.");
                    scrollToNode(caseFormCard);
                });
                Button deleteButton = new Button("Supprimer");
                deleteButton.getStyleClass().addAll("light-button", "danger-light");
                deleteButton.setOnAction(event -> supprimerDepuisListe(item));
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox actions = new HBox(10, spacer, editButton, deleteButton);
                VBox box = new VBox(8, title, meta, state, actions);
                box.getStyleClass().add("list-card-box");
                setGraphic(box);
            }
        });
        casReelsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                afficherDetails(selected);
            }
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
        filtrerCasReels(searchCasReelsField == null ? "" : searchCasReelsField.getText());
    }

    private void updateStats() {
        long pending = allCasReelsList.stream().filter(cas -> CasReelService.STATUT_EN_ATTENTE.equals(cas.getResultat())).count();
        long accepted = allCasReelsList.stream().filter(cas -> CasReelService.STATUT_ACCEPTE.equals(cas.getResultat())).count();
        pendingCasesValueLabel.setText(String.valueOf(pending));
        acceptedCasesValueLabel.setText(String.valueOf(accepted));
    }

    private void remplirFormulaireDepuisCasReel(CasRelles cas) {
        casTitreField.setText(cas.getTitre());
        casTypeComboBox.setValue(cas.getType());
        casDescriptionField.setText(cas.getDescription());
        casMontantField.setText(String.valueOf(cas.getMontant()));
        justificatifField.setText(cas.getJustificatifFileName());
        dateEffetPicker.setValue(cas.getDateEffet());
        imprevuSelectionne = cas.getImprevus();
        updateSelectedEventHint();
    }

    private CasRelles lireFormulaire() {
        String titre = safe(casTitreField);
        String type = casTypeComboBox.getValue();
        String montantText = safe(casMontantField);
        String description = safe(casDescriptionField);
        String justificatif = safe(justificatifField);
        LocalDate dateEffet = dateEffetPicker.getValue();
        if (titre.length() < 3 || type == null || montantText.isBlank() || dateEffet == null) {
            afficherErreur("Titre, type, montant et date sont obligatoires.");
            return null;
        }
        double montant = parsePositiveDouble(montantText, "montant du cas reel");
        if (montant <= 0) {
            return null;
        }
        return new CasRelles(imprevuSelectionne, titre, description, type,
                imprevuSelectionne == null ? "Manuel" : "Depuis imprevu",
                montant, casReelSelectionne == null ? null : casReelSelectionne.getSolution(),
                dateEffet, justificatif);
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

    private void appliquerSolution(String solution) {
        CasRelles selected = casReelsListView.getSelectionModel().getSelectedItem();
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
        dateEffetPicker.setValue(LocalDate.now());
        saveCasButton.setText("Add case");
        resetDetails();
        updateSelectedEventHint();
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
