package app.cookyourbooks.gui.viewmodel;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

/**
 * ViewModel interface for the Shopping List feature.
 *
 * <p>Generates an aggregated shopping list from recipes selected by the user. Measured ingredients
 * (e.g., "2 cups flour") are combined across recipes; vague ingredients (e.g., "salt to taste") are
 * listed separately with no quantity.
 *
 * <h2>Requirement mapping</h2>
 *
 * <ul>
 *   <li><b>SL1:</b> {@link #loadRecipes()} populates {@link #availableRecipesProperty()} from the
 *       library
 *   <li><b>SL2:</b> Each recipe entry exposes ID and title
 *   <li><b>SL3:</b> {@link #setSelectedRecipes(List)} updates the set of selected recipe IDs
 *   <li><b>SL4:</b> {@link #generateShoppingList()} aggregates ingredients from selected recipes
 *       via {@code RecipeService}
 *   <li><b>SL5:</b> Measured results appear in {@link #shoppingItemsProperty()} with name and
 *       formatted quantity
 *   <li><b>SL6:</b> Vague results appear in {@link #uncountableItemsProperty()} as plain strings
 *   <li><b>SL7:</b> {@link #generateShoppingList()} runs on a background thread; {@link
 *       #loadingProperty()} is {@code true} while running; {@link #clearList()} resets results
 * </ul>
 */
public interface ShoppingListViewModel {

  // ──────────────────────────────────────────────────────────────────────────
  // Observable properties (for JavaFX binding in the View)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * All recipes available for selection. You choose the entry type. Each entry should expose the
   * recipe's ID and title.
   */
  ObservableList<?> availableRecipesProperty();

  /**
   * The aggregated measured shopping items. Each entry exposes the ingredient name and a formatted
   * quantity string (e.g., "2 cups").
   */
  ObservableList<?> shoppingItemsProperty();

  /** Names of vague (uncountable) ingredients, such as "salt to taste". */
  ObservableList<String> uncountableItemsProperty();

  /** Whether a background operation (load or generate) is currently in progress. */
  BooleanProperty loadingProperty();

  /**
   * A status message reflecting the current state (e.g., "12 items", "Select at least one recipe",
   * or an error description).
   */
  StringProperty statusMessageProperty();

  // ──────────────────────────────────────────────────────────────────────────
  // Commands (user actions)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Loads all recipes from the library and populates {@link #availableRecipesProperty()}.
   *
   * <p>Must run on a background thread via {@code BackgroundTaskRunner}. While loading, {@link
   * #loadingProperty()} must be {@code true}.
   */
  void loadRecipes();

  /**
   * Replaces the current selection with the given recipe IDs.
   *
   * <p>Called by the View's selection listener whenever the ListView's selected items change. The
   * ViewModel stores the snapshot for use by {@link #generateShoppingList()}.
   *
   * @param recipeIds the IDs of all currently selected recipes (may be empty)
   */
  void setSelectedRecipes(List<String> recipeIds);

  /**
   * Generates the aggregated shopping list from all currently selected recipes.
   *
   * <p>Must run on a background thread via {@code BackgroundTaskRunner}. While generating, {@link
   * #loadingProperty()} must be {@code true}. On success, populates {@link
   * #shoppingItemsProperty()} and {@link #uncountableItemsProperty()} and updates {@link
   * #statusMessageProperty()}. On failure, clears the result lists and sets a descriptive error
   * message.
   *
   * <p>This is a no-op if no recipes are currently selected.
   */
  void generateShoppingList();

  /** Clears both result lists and resets the status message. */
  void clearList();

  // ──────────────────────────────────────────────────────────────────────────
  // Non-JavaFX accessors (for grading tests)
  // ──────────────────────────────────────────────────────────────────────────

  /** Returns the IDs of all available recipes. */
  List<String> getAvailableRecipeIds();

  /** Returns the IDs of all currently selected recipes. */
  List<String> getSelectedRecipeIds();

  /** Returns the names of all measured shopping items in the current result. */
  List<String> getShoppingItemNames();

  /** Returns the names of all uncountable items in the current result. */
  List<String> getUncountableItemNames();

  /** Returns whether a background operation is currently in progress. */
  boolean isLoading();

  /** Returns the current status message. */
  String getStatusMessage();
}
