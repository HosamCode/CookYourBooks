package app.cookyourbooks.gui.view;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import app.cookyourbooks.gui.EditableIngredient;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModelImpl;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.TransformerService;

@SuppressWarnings("NullAway.Init")
public class RecipeEditorViewController {

  @FXML private TextField titleField;
  @FXML private ListView<EditableIngredient> ingredientList;
  @FXML private ListView<String> instructionList;
  @FXML private Button editButton;
  @FXML private Button saveButton;
  @FXML private Button discardButton;
  @FXML private Button addIngredientButton;
  @FXML private Button removeIngredientButton;
  @FXML private Button scaleButton;
  @FXML private TextField servingsField;
  @FXML private Label scaleStatusLabel;
  @FXML private Label statusLabel;

  private final RecipeEditorViewModelImpl viewModel;
  private final NavigationService navigationService;

  public RecipeEditorViewController(
      RecipeRepository recipeRepository,
      NavigationService navigationService,
      TransformerService transformerService) {
    this.viewModel = new RecipeEditorViewModelImpl(recipeRepository, transformerService);
    this.navigationService = navigationService;
  }

  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    titleField.textProperty().bindBidirectional(viewModel.titleProperty());

    ingredientList.setItems(viewModel.ingredientsProperty());
    ingredientList.setCellFactory(lv -> new IngredientCell());

    instructionList.setItems(viewModel.instructionsProperty());

    statusLabel.textProperty().bind(viewModel.statusMessageProperty());

    titleField
        .disableProperty()
        .bind(viewModel.editingProperty().not().or(viewModel.isSavingProperty()));
    addIngredientButton
        .disableProperty()
        .bind(viewModel.editingProperty().not().or(viewModel.isSavingProperty()));
    removeIngredientButton
        .disableProperty()
        .bind(viewModel.editingProperty().not().or(viewModel.isSavingProperty()));

    editButton.visibleProperty().bind(viewModel.editingProperty().not());
    editButton.managedProperty().bind(viewModel.editingProperty().not());

    saveButton.visibleProperty().bind(viewModel.editingProperty());
    saveButton.managedProperty().bind(viewModel.editingProperty());
    saveButton
        .disableProperty()
        .bind(viewModel.isValidProperty().not().or(viewModel.isSavingProperty()));
    saveButton
        .textProperty()
        .bind(
            javafx.beans.binding.Bindings.when(viewModel.isSavingProperty())
                .then("Saving...")
                .otherwise("Save"));

    discardButton.visibleProperty().bind(viewModel.editingProperty());
    discardButton.managedProperty().bind(viewModel.editingProperty());
    discardButton.disableProperty().bind(viewModel.isSavingProperty());

    // Scale button disabled when no recipe loaded
    scaleButton.disableProperty().bind(viewModel.recipeLoadedProperty().not());

    viewModel
        .statusMessageProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              statusLabel.getStyleClass().removeAll("error-label", "success-label", "status-label");
              if (newVal != null && newVal.startsWith("Save failed")) {
                statusLabel.getStyleClass().add("error-label");
              } else if (newVal != null && newVal.startsWith("Saved")) {
                statusLabel.getStyleClass().add("success-label");
              } else {
                statusLabel.getStyleClass().add("status-label");
              }
            });

    navigationService
        .selectedRecipeIdProperty()
        .addListener(
            (obs, oldId, newId) -> {
              if (newId != null && !newId.isBlank()) {
                try {
                  viewModel.loadRecipe(newId);
                  scaleStatusLabel.setText("");
                } catch (Exception e) {
                  statusLabel.setText("Failed to load recipe: " + e.getMessage());
                }
              }
            });
  }

  @FXML
  private void onEdit() {
    viewModel.toggleEditMode();
  }

  @FXML
  private void onSave() {
    viewModel.save();
  }

  @FXML
  private void onDiscard() {
    if (viewModel.isDirty()) {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.setTitle("Unsaved Changes");
      alert.setHeaderText("You have unsaved changes.");
      alert.setContentText("Save before leaving?");

      ButtonType saveBtn = new ButtonType("Save");
      ButtonType discardBtn = new ButtonType("Discard");
      ButtonType cancelBtn =
          new ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

      alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

      alert
          .showAndWait()
          .ifPresent(
              result -> {
                if (result == saveBtn) {
                  viewModel.save();
                } else if (result == discardBtn) {
                  viewModel.discardChanges();
                }
              });
    } else {
      viewModel.discardChanges();
    }
  }

  @FXML
  private void onAddIngredient() {
    viewModel.addIngredient();
  }

  @FXML
  private void onRemoveIngredient() {
    int idx = ingredientList.getSelectionModel().getSelectedIndex();
    if (idx >= 0) {
      viewModel.removeIngredient(idx);
    }
  }

  @FXML
  private void onScale() {
    String text = servingsField.getText().trim();
    if (text.isBlank()) {
      scaleStatusLabel.setText("Enter a number of servings.");
      return;
    }
    int targetServings;
    try {
      targetServings = Integer.parseInt(text);
    } catch (NumberFormatException e) {
      scaleStatusLabel.setText("Invalid number.");
      return;
    }
    if (targetServings <= 0) {
      scaleStatusLabel.setText("Servings must be positive.");
      return;
    }
    try {
      viewModel.scaleRecipe(targetServings);
      scaleStatusLabel.setText("Scaled to " + targetServings + " servings.");
    } catch (Exception e) {
      scaleStatusLabel.setText("Scale failed: " + e.getMessage());
    }
  }

  // ── Ingredient cell: shows [Qty] [Unit dropdown] [Name] ──
  private class IngredientCell extends ListCell<EditableIngredient> {
    private final HBox row = new HBox(6);
    private final TextField qtyField = new TextField();
    private final ComboBox<String> unitBox = new ComboBox<>();
    private final TextField nameField = new TextField();
    private @org.jspecify.annotations.Nullable EditableIngredient currentItem = null;

    IngredientCell() {
      qtyField.setPrefWidth(60);
      qtyField.setPromptText("Qty");

      unitBox.setPrefWidth(90);
      unitBox.setPromptText("Unit");
      unitBox
          .getItems()
          .addAll(
              "CUP",
              "TABLESPOON",
              "TEASPOON",
              "FLUID_OUNCE",
              "GRAM",
              "KILOGRAM",
              "OUNCE",
              "POUND",
              "MILLILITER",
              "LITER",
              "PIECE",
              "PINCH",
              "DASH");
      unitBox.setEditable(true);

      nameField.setPromptText("Ingredient name");
      HBox.setHgrow(nameField, Priority.ALWAYS);

      var disabled = viewModel.editingProperty().not();
      qtyField.disableProperty().bind(disabled);
      unitBox.disableProperty().bind(disabled);
      nameField.disableProperty().bind(disabled);

      row.getChildren().addAll(qtyField, unitBox, nameField);
      row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(EditableIngredient item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setGraphic(null);
        if (currentItem != null) {
          qtyField.textProperty().unbindBidirectional(currentItem.quantityProperty());
          unitBox.valueProperty().unbindBidirectional(currentItem.unitProperty());
          nameField.textProperty().unbindBidirectional(currentItem.nameProperty());
        }
        currentItem = null;
      } else {
        if (currentItem != null) {
          qtyField.textProperty().unbindBidirectional(currentItem.quantityProperty());
          unitBox.valueProperty().unbindBidirectional(currentItem.unitProperty());
          nameField.textProperty().unbindBidirectional(currentItem.nameProperty());
        }
        currentItem = item;
        qtyField.textProperty().bindBidirectional(item.quantityProperty());
        unitBox.valueProperty().bindBidirectional(item.unitProperty());
        nameField.textProperty().bindBidirectional(item.nameProperty());
        setGraphic(row);
      }
    }
  }
}
