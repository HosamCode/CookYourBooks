package app.cookyourbooks.gui.viewmodel;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.RecipeService;

/** Implementation of {@link ShoppingListViewModel} for aggregating shopping lists from recipes. */
public final class ShoppingListViewModelImpl implements ShoppingListViewModel {

  // ── Services ──────────────────────────────────────────────────────────────
  private final LibrarianService librarianService;
  private final RecipeService recipeService;

  // ── Internal state (not observed by the View) ─────────────────────────────
  private List<String> selectedRecipeIds = new ArrayList<>();

  // ── Observable properties ─────────────────────────────────────────────────
  private final ObservableList<RecipeSummary> availableRecipes =
      FXCollections.observableArrayList();
  private final ObservableList<ShoppingItemSummary> shoppingItems =
      FXCollections.observableArrayList();
  private final ObservableList<String> uncountableItems = FXCollections.observableArrayList();
  private final BooleanProperty loading = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");

  /**
   * Constructs a new ShoppingListViewModelImpl.
   *
   * @param librarianService used to load all available recipes
   * @param recipeService used to generate the aggregated shopping list
   */
  public ShoppingListViewModelImpl(LibrarianService librarianService, RecipeService recipeService) {
    this.librarianService = librarianService;
    this.recipeService = recipeService;
  }

  // ── Observable property accessors ─────────────────────────────────────────

  @Override
  public ObservableList<RecipeSummary> availableRecipesProperty() {
    return availableRecipes;
  }

  @Override
  public ObservableList<ShoppingItemSummary> shoppingItemsProperty() {
    return shoppingItems;
  }

  @Override
  public ObservableList<String> uncountableItemsProperty() {
    return uncountableItems;
  }

  @Override
  public BooleanProperty loadingProperty() {
    return loading;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  // ── Commands ──────────────────────────────────────────────────────────────

  @Override
  public void loadRecipes() {
    loading.set(true);
    var unused =
        BackgroundTaskRunner.run(
            librarianService::listAllRecipes,
            result -> {
              List<RecipeSummary> summaries =
                  result.stream().map(r -> new RecipeSummary(r.getId(), r.getTitle())).toList();
              availableRecipes.setAll(summaries);
              loading.set(false);
            },
            error -> {
              statusMessage.set("Failed to load recipes: " + error.getMessage());
              loading.set(false);
            });
  }

  @Override
  public void setSelectedRecipes(List<String> recipeIds) {
    this.selectedRecipeIds = new ArrayList<>(recipeIds);
  }

  @Override
  public void generateShoppingList() {
    if (selectedRecipeIds.isEmpty()) {
      return;
    }
    // Snapshot the selection so changes mid-flight don't affect this run
    List<String> ids = new ArrayList<>(selectedRecipeIds);
    loading.set(true);
    var unused =
        BackgroundTaskRunner.run(
            () -> recipeService.generateShoppingList(ids),
            result -> {
              List<ShoppingItemSummary> items =
                  result.getItems().stream()
                      .map(i -> new ShoppingItemSummary(i.getName(), i.getQuantity().toString()))
                      .toList();
              shoppingItems.setAll(items);
              uncountableItems.setAll(result.getUncountableItems());
              int total = items.size() + result.getUncountableItems().size();
              statusMessage.set(total + " item" + (total == 1 ? "" : "s"));
              loading.set(false);
            },
            error -> {
              shoppingItems.clear();
              uncountableItems.clear();
              statusMessage.set("Error: " + error.getMessage());
              loading.set(false);
            });
  }

  @Override
  public void clearList() {
    shoppingItems.clear();
    uncountableItems.clear();
    statusMessage.set("");
  }

  // ── Non-JavaFX accessors (for grading tests) ──────────────────────────────

  @Override
  public List<String> getAvailableRecipeIds() {
    return availableRecipes.stream().map(RecipeSummary::id).toList();
  }

  @Override
  public List<String> getSelectedRecipeIds() {
    return new ArrayList<>(selectedRecipeIds);
  }

  @Override
  public List<String> getShoppingItemNames() {
    return shoppingItems.stream().map(ShoppingItemSummary::name).toList();
  }

  @Override
  public List<String> getUncountableItemNames() {
    return new ArrayList<>(uncountableItems);
  }

  @Override
  public boolean isLoading() {
    return loading.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }
}
