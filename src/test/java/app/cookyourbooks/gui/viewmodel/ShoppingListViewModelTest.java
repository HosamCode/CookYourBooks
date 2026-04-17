package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.ShoppingItem;
import app.cookyourbooks.model.ShoppingList;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.RecipeService;

@SuppressWarnings("NullAway.Init")
class ShoppingListViewModelTest extends ViewModelTestBase {

  private LibrarianService librarianService;
  private RecipeService recipeService;
  private ShoppingListViewModelImpl vm;

  @BeforeEach
  void setUp() throws InterruptedException {
    librarianService = mock(LibrarianService.class);
    recipeService = mock(RecipeService.class);
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          vm = new ShoppingListViewModelImpl(librarianService, recipeService);
          latch.countDown();
        });
    latch.await();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static Recipe mockRecipe(String id, String title) {
    Recipe r = mock(Recipe.class);
    when(r.getId()).thenReturn(id);
    when(r.getTitle()).thenReturn(title);
    return r;
  }

  private static ShoppingItem mockItem(String name, String quantityStr) {
    ShoppingItem item = mock(ShoppingItem.class);
    app.cookyourbooks.model.Quantity qty = mock(app.cookyourbooks.model.Quantity.class);
    when(qty.toString()).thenReturn(quantityStr);
    when(item.getName()).thenReturn(name);
    when(item.getQuantity()).thenReturn(qty);
    return item;
  }

  private void loadAndWait() throws InterruptedException {
    Platform.runLater(() -> vm.loadRecipes());
    Thread.sleep(200);
    waitForFxEvents();
  }

  // ── SL1: loadRecipes() populates availableRecipesProperty() ───────────────

  @Test
  void sl1_loadRecipesPopulatesAvailableList() throws InterruptedException {
    Recipe r1 = mockRecipe("r1", "Pasta");
    Recipe r2 = mockRecipe("r2", "Soup");
    when(librarianService.listAllRecipes()).thenReturn(List.of(r1, r2));

    loadAndWait();

    assertThat(vm.getAvailableRecipeIds()).containsExactly("r1", "r2");
  }

  // ── SL1: loadingProperty() is true during loadRecipes() ───────────────────

  @Test
  void sl1_loadingIsTrueDuringLoad() throws InterruptedException {
    CountDownLatch serviceStarted = new CountDownLatch(1);
    CountDownLatch proceed = new CountDownLatch(1);
    when(librarianService.listAllRecipes())
        .thenAnswer(
            inv -> {
              serviceStarted.countDown();
              proceed.await();
              return List.of();
            });

    Platform.runLater(() -> vm.loadRecipes());
    waitForFxEvents();

    serviceStarted.await();
    assertThat(vm.isLoading()).isTrue();

    proceed.countDown();
    Thread.sleep(100);
    waitForFxEvents();

    assertThat(vm.isLoading()).isFalse();
  }

  // ── SL3: setSelectedRecipes() updates getSelectedRecipeIds() ──────────────

  @Test
  void sl3_setSelectedRecipesUpdatesAccessor() throws InterruptedException {
    Platform.runLater(() -> vm.setSelectedRecipes(List.of("r1", "r2")));
    waitForFxEvents();

    assertThat(vm.getSelectedRecipeIds()).containsExactly("r1", "r2");
  }

  // ── SL4/SL5: generateShoppingList() populates measured items ──────────────

  @Test
  void sl4_generatePopulatesMeasuredItems() throws InterruptedException {
    ShoppingItem flour = mockItem("flour", "2 cups");
    ShoppingItem eggs = mockItem("eggs", "3");
    ShoppingList result = mock(ShoppingList.class);
    when(result.getItems()).thenReturn(List.of(flour, eggs));
    when(result.getUncountableItems()).thenReturn(List.of());
    when(recipeService.generateShoppingList(List.of("r1"))).thenReturn(result);

    Platform.runLater(() -> vm.setSelectedRecipes(List.of("r1")));
    waitForFxEvents();

    Platform.runLater(() -> vm.generateShoppingList());
    Thread.sleep(200);
    waitForFxEvents();

    assertThat(vm.getShoppingItemNames()).containsExactly("flour", "eggs");
  }

  // ── SL6: generateShoppingList() populates uncountable items ───────────────

  @Test
  void sl6_generatePopulatesUncountableItems() throws InterruptedException {
    ShoppingList result = mock(ShoppingList.class);
    when(result.getItems()).thenReturn(List.of());
    when(result.getUncountableItems()).thenReturn(List.of("salt to taste", "fresh herbs"));
    when(recipeService.generateShoppingList(List.of("r1"))).thenReturn(result);

    Platform.runLater(() -> vm.setSelectedRecipes(List.of("r1")));
    waitForFxEvents();

    Platform.runLater(() -> vm.generateShoppingList());
    Thread.sleep(200);
    waitForFxEvents();

    assertThat(vm.getUncountableItemNames()).containsExactly("salt to taste", "fresh herbs");
  }

  // ── SL7: clearList() resets both result lists and status ──────────────────

  @Test
  void sl7_clearListResetsResults() throws InterruptedException {
    ShoppingItem flour = mockItem("flour", "2 cups");
    ShoppingList result = mock(ShoppingList.class);
    when(result.getItems()).thenReturn(List.of(flour));
    when(result.getUncountableItems()).thenReturn(List.of("salt to taste"));
    when(recipeService.generateShoppingList(List.of("r1"))).thenReturn(result);

    Platform.runLater(() -> vm.setSelectedRecipes(List.of("r1")));
    waitForFxEvents();
    Platform.runLater(() -> vm.generateShoppingList());
    Thread.sleep(200);
    waitForFxEvents();

    // Results should be populated before clear
    assertThat(vm.getShoppingItemNames()).isNotEmpty();

    Platform.runLater(() -> vm.clearList());
    waitForFxEvents();

    assertThat(vm.getShoppingItemNames()).isEmpty();
    assertThat(vm.getUncountableItemNames()).isEmpty();
    assertThat(vm.getStatusMessage()).isEmpty();
  }

  // ── SL7: generateShoppingList() is a no-op when nothing selected ──────────

  @Test
  void sl7_generateIsNoopWhenNothingSelected() throws InterruptedException {
    Platform.runLater(() -> vm.generateShoppingList());
    waitForFxEvents();

    verify(recipeService, never()).generateShoppingList(org.mockito.ArgumentMatchers.anyList());
    assertThat(vm.getShoppingItemNames()).isEmpty();
  }

  // ── SL7: status message reflects total item count ─────────────────────────

  @Test
  void sl7_statusMessageShowsItemCount() throws InterruptedException {
    ShoppingItem flour = mockItem("flour", "2 cups");
    ShoppingList result = mock(ShoppingList.class);
    when(result.getItems()).thenReturn(List.of(flour));
    when(result.getUncountableItems()).thenReturn(List.of("salt to taste"));
    when(recipeService.generateShoppingList(List.of("r1"))).thenReturn(result);

    Platform.runLater(() -> vm.setSelectedRecipes(List.of("r1")));
    waitForFxEvents();
    Platform.runLater(() -> vm.generateShoppingList());
    Thread.sleep(200);
    waitForFxEvents();

    assertThat(vm.getStatusMessage()).isEqualTo("2 items");
  }
}
