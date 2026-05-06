package pi.controllers.InvestissementController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pi.entities.Investissement;
import pi.entities.Objectif;
import pi.entities.User;
import pi.mains.Main;
import pi.services.InvestissementService.InvestissementService;
import pi.services.InvestissementService.ObjectifService;
import pi.tools.ThemeManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminController {

    @FXML private TableView<Investissement> investTable;
    @FXML private TableColumn<Investissement, String> colCrypto;
    @FXML private TableColumn<Investissement, Number> colAmount;
    @FXML private TableColumn<Investissement, Number> colQuantity;
    @FXML private TableColumn<Investissement, Number> colProfitLoss;
    @FXML private TableColumn<Investissement, Void> colDeleteInvest;
    @FXML private TextField investSearch;

    @FXML private TableView<Objectif> objectifTable;
    @FXML private TableColumn<Objectif, String> colName;
    @FXML private TableColumn<Objectif, Number> colInitial;
    @FXML private TableColumn<Objectif, Number> colTarget;
    @FXML private TableColumn<Objectif, Number> colCurrent;
    @FXML private TableColumn<Objectif, String> colCompleted;
    @FXML private TableColumn<Objectif, Void> colModifyObj;
    @FXML private TableColumn<Objectif, Void> colDeleteObj;
    @FXML private TextField objectifSearch;

    @FXML private Label totalInvestLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label totalObjLabel;
    @FXML private Label completedObjLabel;
    @FXML private PieChart objectifChart;
    @FXML private VBox menuList;
    @FXML private Label feedbackLabel;

    private final InvestissementService investissementService = new InvestissementService();
    private final ObjectifService objectifService = new ObjectifService();
    private User currentUser;

    private Map<Integer, Double> objectifCurrentValueCache = new HashMap<>();

    @FXML
    public void initialize() {
        // investments columns
        colCrypto.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCrypto().getName()));
        colAmount.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getAmountInvested()));
        colQuantity.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getQuantity()));
        colProfitLoss.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getProfitLoss()));

        colDeleteInvest.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.getStyleClass().add("table-delete-button");
                btn.setOnAction(e -> {
                    Investissement inv = getTableView().getItems().get(getIndex());
                    try {
                        investissementService.delete(inv.getId());
                        loadInvestissements();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // objectifs columns
        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        colInitial.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getInitialAmount()));
        colTarget.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getTargetAmount()));
        colCurrent.setCellValueFactory(data ->
                new SimpleDoubleProperty(objectifCurrentValueCache.getOrDefault(data.getValue().getId(), 0.0)));
        colCompleted.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isCompleted() ? "✅ Complété" : "⏳ En cours"));

        colModifyObj.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Modifier");
            {
                btn.getStyleClass().add("table-edit-button");
                btn.setOnAction(e -> {
                    Objectif obj = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/modifyObjectif.fxml"));
                        Scene scene = new Scene(loader.load());
                        ModifyObjectifController controller = loader.getController();
                        if (currentUser != null) {
                            controller.setUser(currentUser);
                        }
                        controller.setObjectif(obj);

                        Stage stage = new Stage();
                        stage.setTitle("Modifier l'objectif");
                        stage.setScene(scene);
                        stage.initModality(Modality.APPLICATION_MODAL);
                        stage.showAndWait();

                        loadObjectifs();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        colDeleteObj.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.getStyleClass().add("table-delete-button");
                btn.setOnAction(e -> {
                    Objectif obj = getTableView().getItems().get(getIndex());
                    try {
                        if (currentUser != null && currentUser.getId() > 0) {
                            objectifService.deleteForUser(obj.getId(), currentUser.getId());
                        } else {
                            objectifService.delete(obj.getId());
                        }
                        loadObjectifs();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        loadInvestissements();
        loadObjectifs();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadInvestissements();
        loadObjectifs();
    }

    @FXML
    private void handleSidebarSelection(MouseEvent event) {
        if (!(event.getSource() instanceof HBox selectedRow) || menuList == null) {
            return;
        }

        menuList.getChildren().stream()
                .filter(HBox.class::isInstance)
                .map(HBox.class::cast)
                .forEach(row -> row.getStyleClass().remove("menu-row-active"));

        if (!selectedRow.getStyleClass().contains("menu-row-active")) {
            selectedRow.getStyleClass().add("menu-row-active");
        }

        if (selectedRow.getChildren().size() < 2 || !(selectedRow.getChildren().get(1) instanceof Label menuLabel)) {
            return;
        }

        String key = menuLabel.getText() == null ? "" : menuLabel.getText().trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "users", "transactions", "course & quiz", "unexpected events", "real cases",
                 "revenues", "expenses", "reports", "objectives", "reclamations", "goals",
                 "statistics", "ai quiz generator" -> openWindow("/pi/mains/admin-backend-view.fxml", "Admin Backend");
            case "investments" -> {
                // Already on investments page.
            }
            default -> {
                // Placeholder entries not yet wired.
            }
        }
    }

    @FXML
    private void handleOpenFrontInterfaceFromSidebar(MouseEvent event) {
        openWindow("/Expense/Revenue/FRONT/salary-expense-view.fxml", "Income & Expense Front Office");
    }

    @FXML
    private void handleLogout() {
        openWindow("/pi/mains/login-view.fxml", "User Secure Login");
    }

    private void openWindow(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource(resource));
            Stage stage = new Stage();
            stage.setTitle(title);
            Scene scene = new Scene(root, 1460, 900);
            if (resource != null && resource.contains("/pi/mains/transactions-management-view.fxml")) {
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/admin-backend.css").toExternalForm());
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/user-show.css").toExternalForm());
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/edit-user.css").toExternalForm());
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/transactions-management.css").toExternalForm());
                ThemeManager.registerScene(scene);
            }
            stage.setScene(scene);
            if (feedbackLabel != null && feedbackLabel.getScene() != null) {
                stage.initOwner(feedbackLabel.getScene().getWindow());
            }
            stage.show();
        } catch (IOException exception) {
            if (feedbackLabel != null) {
                feedbackLabel.setText("Unable to open: " + title);
            }
        }
    }

    private void loadInvestissements() {
        try {
            List<Investissement> list = (currentUser != null && currentUser.getId() > 0)
                    ? investissementService.getAllByUser(currentUser.getId())
                    : investissementService.getAll();

            totalInvestLabel.setText(String.valueOf(list.size()));
            double totalAmount = list.stream().mapToDouble(Investissement::getAmountInvested).sum();
            totalAmountLabel.setText(String.format("%.2f USD", totalAmount));

            ObservableList<Investissement> data = FXCollections.observableArrayList(list);
            FilteredList<Investissement> filtered = new FilteredList<>(data, p -> true);

            investSearch.textProperty().addListener((obs, oldVal, newVal) ->
                filtered.setPredicate(inv -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return inv.getCrypto().getName().toLowerCase().contains(newVal.toLowerCase());
                })
            );

            investTable.setItems(filtered);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadObjectifs() {
        try {
            List<Objectif> list = (currentUser != null && currentUser.getId() > 0)
                    ? objectifService.getAllByUser(currentUser.getId())
                    : objectifService.getAll();
            for (Objectif obj : list) {
                if (currentUser != null && currentUser.getId() > 0) {
                    objectifService.checkAndMarkCompletedForUser(obj.getId(), currentUser.getId());
                } else {
                    objectifService.checkAndMarkCompleted(obj.getId());
                }
            }
            list = (currentUser != null && currentUser.getId() > 0)
                    ? objectifService.getAllByUser(currentUser.getId())
                    : objectifService.getAll();

            objectifCurrentValueCache = (currentUser != null && currentUser.getId() > 0)
                    ? objectifService.getCurrentValuesAllByUser(currentUser.getId())
                    : objectifService.getCurrentValuesAll();

            totalObjLabel.setText(String.valueOf(list.size()));
            long completed = list.stream().filter(Objectif::isCompleted).count();
            long incomplete = list.size() - completed;
            completedObjLabel.setText(String.valueOf(completed));

            PieChart.Data completedSlice = new PieChart.Data("Complétés (" + completed + ")", completed);
            PieChart.Data incompleteSlice = new PieChart.Data("En cours (" + incomplete + ")", incomplete);
            objectifChart.getData().setAll(completedSlice, incompleteSlice);
            objectifChart.setTitle("Objectifs");

            objectifChart.getData().get(0).getNode().setStyle("-fx-pie-color: #08bbe7;");
            objectifChart.getData().get(1).getNode().setStyle("-fx-pie-color: #cbd5e1;");

            ObservableList<Objectif> data = FXCollections.observableArrayList(list);
            FilteredList<Objectif> filtered = new FilteredList<>(data, p -> true);

            objectifSearch.textProperty().addListener((obs, oldVal, newVal) ->
                filtered.setPredicate(obj -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return obj.getName().toLowerCase().contains(newVal.toLowerCase());
                })
            );

            objectifTable.setItems(filtered);
            objectifTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
