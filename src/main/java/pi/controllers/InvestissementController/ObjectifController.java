package pi.controllers.InvestissementController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pi.entities.Objectif;
import pi.services.InvestissementService.ObjectifService;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

import java.util.List;

public class ObjectifController {

    @FXML
    private TableView<Objectif> objectifTable;

    @FXML
    private TableColumn<Objectif, String> colName;

    @FXML
    private TableColumn<Objectif, Number> colInitial;

    @FXML
    private TableColumn<Objectif, Number> colTarget;

    @FXML
    private TableColumn<Objectif, Number> colCurrent;

    @FXML
    private TableColumn<Objectif, String> colCompleted;

    @FXML
    private TableColumn<Objectif, Void> colModify;

    @FXML
    private TableColumn<Objectif, Void> colDelete;

    @FXML
    private TextField objectifSearch;

    private final ObjectifService objectifService = new ObjectifService();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        colInitial.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getInitialAmount()));

        colTarget.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getTargetAmount()));

        colCurrent.setCellValueFactory(data -> {
            try {
                double currentValue = objectifService.getCurrentValue(data.getValue().getId());
                return new SimpleDoubleProperty(currentValue);
            } catch (Exception e) {
                e.printStackTrace();
                return new SimpleDoubleProperty(0);
            }
        });

        colCompleted.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isCompleted() ? "✅ Complété" : "⏳ En cours"));

        colModify.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Modifier");
            {
                btn.setOnAction(e -> {
                    Objectif obj = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/modifyObjectif.fxml"));
                        Scene scene = new Scene(loader.load());
                        ModifyObjectifController controller = loader.getController();
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

        loadObjectifs();

        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.setOnAction(e -> {
                    Objectif obj = getTableView().getItems().get(getIndex());
                    try {
                        objectifService.delete(obj.getId());
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

    }

    private void loadObjectifs() {
        try {
            List<Objectif> list = objectifService.getAll();

            for (Objectif obj : list) {
                objectifService.checkAndMarkCompleted(obj.getId());
            }

            list = objectifService.getAll();
            ObservableList<Objectif> data = FXCollections.observableArrayList(list);
            FilteredList<Objectif> filtered = new FilteredList<>(data, p -> true);

            objectifSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                filtered.setPredicate(obj -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return obj.getName().toLowerCase().contains(newVal.toLowerCase());
                });
            });

            objectifTable.setItems(filtered);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/createObjectif.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setTitle("Créer un objectif");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // reload after creation
            loadObjectifs();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
