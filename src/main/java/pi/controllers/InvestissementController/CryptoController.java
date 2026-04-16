package pi.controllers.InvestissementController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import pi.entities.Crypto;
import pi.entities.Investissement;
import pi.services.InvestissementService.CryptoApiService;
import pi.services.InvestissementService.CryptoService;
import pi.services.InvestissementService.InvestissementService;

import java.util.List;

public class CryptoController {

    @FXML
    private TableView<Crypto> table;

    @FXML
    private TableColumn<Crypto, String> colName;

    @FXML
    private TableColumn<Crypto, Number> colPrice;

    @FXML
    private TableView<Investissement> investTable;

    @FXML
    private TableColumn<Investissement, String> colCrypto;

    @FXML
    private TableColumn<Investissement, Number> colAmount;

    @FXML
    private TableColumn<Investissement, Number> colQuantity;

    @FXML
    private TableColumn<Investissement, Void> colDelete;

    @FXML
    private TableColumn<Investissement, Number> colProfitLoss;

    @FXML
    private TableColumn<Investissement, Void> colModify;

    @FXML
    private TextField investSearch;

    private final CryptoApiService apiService = new CryptoApiService();
    private final CryptoService cryptoService = new CryptoService();
    private final InvestissementService investissementService = new InvestissementService();

    @FXML
    public void initialize() {

        // 🔹 Table Crypto
        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        colPrice.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getCurrentprice()));

        // 🔹 Table Investissement
        colCrypto.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCrypto().getName()));

        colAmount.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getAmountInvested()));


        colProfitLoss.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getProfitLoss()));


        colQuantity.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getQuantity()));
        colDelete.setCellFactory(col -> new TableCell<>() {
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
        colModify.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Modifier");
            {
                btn.getStyleClass().add("table-edit-button");
                btn.setOnAction(e -> {
                    Investissement inv = getTableView().getItems().get(getIndex());
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(inv.getAmountInvested()));
                    dialog.setTitle("Modifier investissement");
                    dialog.setHeaderText(null);
                    dialog.setContentText("Nouveau montant (USD):");
                    dialog.showAndWait().ifPresent(input -> {
                        try {
                            double newAmount = Double.parseDouble(input);
                            double newQuantity = newAmount / inv.getBuyPrice();
                            investissementService.update(inv.getId(), newAmount, newQuantity);
                            loadInvestissements();
                        } catch (NumberFormatException ex) {
                            // invalid input, ignore
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });


        loadData();
        loadInvestissements();
    }

    @FXML
    public void refresh() {
        loadData();
        loadInvestissements();
    }

    private void loadData() {
        try {
            List<Crypto> cryptos = apiService.getCryptos();

            try {
                cryptoService.saveAllOrUpdate(cryptos);
            } catch (Exception e) {
                System.out.println("DB save failed: " + e.getMessage());
            }

            table.setItems(FXCollections.observableArrayList(cryptos));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInvestissements() {
        try {
            List<Investissement> list = investissementService.getAll();
            ObservableList<Investissement> data = FXCollections.observableArrayList(list);
            FilteredList<Investissement> filtered = new FilteredList<>(data, p -> true);

            investSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                filtered.setPredicate(inv -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return inv.getCrypto().getName().toLowerCase().contains(newVal.toLowerCase());
                });
            });

            investTable.setItems(filtered);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToInvest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/investissement.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setTitle("Investir");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void goToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/admin.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setTitle("Admin");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToObjectifs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/objectif.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setTitle("Objectifs");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}