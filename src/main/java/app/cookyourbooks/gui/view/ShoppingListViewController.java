package app.cookyourbooks.gui.view;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;

import app.cookyourbooks.gui.viewmodel.RecipeSummary;
import app.cookyourbooks.gui.viewmodel.ShoppingItemSummary;
import app.cookyourbooks.gui.viewmodel.ShoppingListViewModel;

/**
 * Controller for the Shopping List View ({@code ShoppingListView.fxml}).
 *
 * <p>Binds all UI controls to the {@link ShoppingListViewModel}. No business logic lives here.
 */
@SuppressWarnings({"NullAway.Init"})
public class ShoppingListViewController {

  @FXML private ListView<RecipeSummary> recipesListView;
  @FXML private ListView<ShoppingItemSummary> shoppingItemsListView;
  @FXML private ListView<String> uncountableItemsListView;
  @FXML private Button generateButton;
  @FXML private Button clearButton;
  @FXML private Label statusLabel;
  @FXML private ProgressIndicator loadingIndicator;

  private final ShoppingListViewModel viewModel;

  /**
   * Constructs the controller, injecting the ViewModel.
   *
   * @param viewModel the Shopping List ViewModel
   */
  public ShoppingListViewController(ShoppingListViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @SuppressWarnings({"UnusedMethod", "unchecked"})
  @FXML
  private void initialize() {
    // ── Available recipes (MULTIPLE selection) ───────────────────────────────
    recipesListView.setItems((ObservableList<RecipeSummary>) viewModel.availableRecipesProperty());
    recipesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    recipesListView.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(RecipeSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
              }
            });

    // Hand the full selected-ID snapshot to the ViewModel on every selection change.
    // We pass the whole list rather than toggling one at a time because the ListView
    // selection model is the authoritative source of truth — the ViewModel just stores it.
    recipesListView
        .getSelectionModel()
        .getSelectedItems()
        .addListener(
            (javafx.collections.ListChangeListener<RecipeSummary>)
                change -> {
                  List<String> ids =
                      recipesListView.getSelectionModel().getSelectedItems().stream()
                          .map(RecipeSummary::id)
                          .toList();
                  viewModel.setSelectedRecipes(ids);
                });

    // ── Shopping items (measured) ────────────────────────────────────────────
    shoppingItemsListView.setItems(
        (ObservableList<ShoppingItemSummary>) viewModel.shoppingItemsProperty());
    shoppingItemsListView.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(ShoppingItemSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.quantity() + "  " + item.name());
              }
            });

    // ── Uncountable items ────────────────────────────────────────────────────
    uncountableItemsListView.setItems(viewModel.uncountableItemsProperty());

    // ── Status label + loading indicator ────────────────────────────────────
    statusLabel.textProperty().bind(viewModel.statusMessageProperty());
    loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
    loadingIndicator.managedProperty().bind(viewModel.loadingProperty());

    // ── Generate: disabled when loading OR nothing selected ──────────────────
    generateButton
        .disableProperty()
        .bind(
            viewModel
                .loadingProperty()
                .or(Bindings.isEmpty(recipesListView.getSelectionModel().getSelectedItems())));
    generateButton.setOnAction(e -> viewModel.generateShoppingList());

    // ── Clear ────────────────────────────────────────────────────────────────
    clearButton.setOnAction(e -> viewModel.clearList());

    // Kick off the recipe load as soon as the view is ready
    viewModel.loadRecipes();
  }
}
