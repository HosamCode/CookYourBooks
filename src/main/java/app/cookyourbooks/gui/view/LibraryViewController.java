package app.cookyourbooks.gui.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import app.cookyourbooks.gui.viewmodel.CollectionSummary;
import app.cookyourbooks.gui.viewmodel.LibraryViewModel;
import app.cookyourbooks.gui.viewmodel.RecipeSummary;

/**
 * Controller for the Library View ({@code LibraryView.fxml}).
 *
 * <p>Binds all UI controls to the {@link LibraryViewModel}. No business logic lives here — all
 * state management happens in the ViewModel.
 */
@SuppressWarnings({"NullAway.Init"})
public class LibraryViewController {

  @FXML private TextField filterTextField;
  @FXML private Button refreshButton;
  @FXML private Button createButton;
  @FXML private Button deleteButton;
  @FXML private ProgressIndicator loadingIndicator;
  @FXML private ListView<CollectionSummary> collectionsListView;
  @FXML private ListView<RecipeSummary> recipesListView;
  @FXML private HBox undoBar;
  @FXML private Label undoMessageLabel;
  @FXML private Button undoButton;

  private final LibraryViewModel viewModel;

  /**
   * Constructs the controller, injecting the ViewModel.
   *
   * @param viewModel the Library ViewModel
   */
  public LibraryViewController(LibraryViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @SuppressWarnings({"UnusedMethod", "unchecked"})
  @FXML
  private void initialize() {
    // ── Filter text field ────────────────────────────────────────────────────
    filterTextField.textProperty().bindBidirectional(viewModel.filterTextProperty());

    // ── Collections list ─────────────────────────────────────────────────────
    // Safe cast: our LibraryViewModelImpl always returns ObservableList<CollectionSummary>
    collectionsListView.setItems(
        (ObservableList<CollectionSummary>) viewModel.collectionsProperty());
    collectionsListView.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(CollectionSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
              }
            });
    collectionsListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                viewModel.selectCollection(newVal.id());
              }
            });

    // ── Recipes list ─────────────────────────────────────────────────────────
    // Safe cast: our LibraryViewModelImpl always returns ObservableList<RecipeSummary>
    recipesListView.setItems((ObservableList<RecipeSummary>) viewModel.recipesProperty());
    recipesListView.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(RecipeSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
              }
            });
    recipesListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                viewModel.selectRecipe(newVal.id());
              }
            });

    // ── Loading indicator: bind both visible and managed ─────────────────────
    loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
    loadingIndicator.managedProperty().bind(viewModel.loadingProperty());

    // ── Undo bar: bind both visible and managed ───────────────────────────────
    undoBar.visibleProperty().bind(viewModel.undoAvailableProperty());
    undoBar.managedProperty().bind(viewModel.undoAvailableProperty());
    undoMessageLabel.textProperty().bind(viewModel.undoMessageProperty());

    // ── Buttons ───────────────────────────────────────────────────────────────
    refreshButton.setOnAction(e -> viewModel.refresh());
    createButton.setOnAction(
        e -> {
          String title = filterTextField.getText().trim();
          if (!title.isBlank()) {
            viewModel.createCollection(title);
          }
        });
    deleteButton.setOnAction(
        e -> {
          CollectionSummary selected = collectionsListView.getSelectionModel().getSelectedItem();
          if (selected != null) {
            viewModel.deleteCollection(selected.id());
          }
        });
    undoButton.setOnAction(e -> viewModel.undoDelete());
  }
}
