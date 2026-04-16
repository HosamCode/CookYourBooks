package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;

@SuppressWarnings("NullAway")
class SearchViewModelImplTest extends ViewModelTestBase {

  private LibrarianService libSvc;
  private NavigationService navSvc;
  private SearchViewModelImpl vm;

  private static final Duration FAST = Duration.ofMillis(50);

  private static Recipe recipe(String id, String title) {
    Recipe r = mock(Recipe.class);
    when(r.getId()).thenReturn(id);
    when(r.getTitle()).thenReturn(title);
    return r;
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    libSvc = mock(LibrarianService.class);
    navSvc = new NavigationService();

    Recipe cake = recipe("id-cake", "Chocolate Cake");
    Recipe pasta = recipe("id-pasta", "Pasta Bolognese");
    Recipe cookie = recipe("id-cookie", "Oatmeal Cookie");

    when(libSvc.listAllRecipes()).thenReturn(List.of(cake, pasta, cookie));
    when(libSvc.resolveRecipes("cake")).thenReturn(List.of(cake));
    when(libSvc.resolveRecipes("pasta")).thenReturn(List.of(pasta));
    when(libSvc.resolveRecipes("cookie")).thenReturn(List.of(cookie));
    when(libSvc.resolveRecipes("zzznomatch")).thenReturn(List.of());
    when(libSvc.searchByIngredient("flour")).thenReturn(List.of(cake, cookie));
    when(libSvc.searchByIngredient("oat")).thenReturn(List.of(cookie));

    // PauseTransition must be created on the FX thread
    Platform.runLater(() -> vm = new SearchViewModelImpl(libSvc, navSvc, FAST));
    waitForFxEvents();
  }

  // ── S1: Setting query triggers search and populates results ───────────

  @Test
  void s1_setQuery_triggersSearchAndPopulatesResults() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("cake"));
    waitForSearch();
    assertThat(vm.getResultIds()).containsExactly("id-cake");
  }

  // ── S2: Title search uses resolveRecipes ──────────────────────────────

  @Test
  void s2_titleSearch_usesResolveRecipes() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("pasta"));
    waitForSearch();
    verify(libSvc).resolveRecipes("pasta");
    assertThat(vm.getResultIds()).containsExactly("id-pasta");
  }

  // ── S3: Ingredient filter narrows results ─────────────────────────────

  @Test
  void s3_addIngredientFilter_narrowsResults() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("cake"));
    waitForSearch();
    Platform.runLater(() -> vm.addIngredientFilter("flour"));
    waitForSearch();
    assertThat(vm.getResultIds()).containsExactly("id-cake");
  }

  // ── S4: Multiple ingredient filters use AND logic ─────────────────────

  @Test
  void s4_multipleIngredientFilters_andLogic() throws InterruptedException {
    Platform.runLater(() -> vm.addIngredientFilter("flour"));
    waitForSearch();
    Platform.runLater(() -> vm.addIngredientFilter("oat"));
    waitForSearch();
    // flour → [cake, cookie]; oat → [cookie]; intersection → [cookie]
    assertThat(vm.getResultIds()).containsExactly("id-cookie");
  }

  // ── S5: Clearing filters resets results ───────────────────────────────

  @Test
  void s5_clearFilters_resetsResults() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("cake"));
    waitForSearch();
    Platform.runLater(() -> vm.clearFilters());
    waitForSearch();
    assertThat(vm.getQuery()).isEmpty();
    assertThat(vm.getIngredientFilters()).isEmpty();
    assertThat(vm.getResultIds()).containsExactlyInAnyOrder("id-cake", "id-pasta", "id-cookie");
  }

  // ── S6: isSearching is false after search completes ───────────────────

  @Test
  void s6_isSearching_falseAfterCompletion() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("cake"));
    waitForSearch();
    assertThat(vm.isSearching()).isFalse();
  }

  // ── S7: Debounce — rapid typing only fires one search ─────────────────

  @Test
  void s7_debounce_rapidTypingFiresOneSearch() throws InterruptedException {
    Platform.runLater(
        () -> {
          vm.setQuery("c");
          vm.setQuery("ca");
          vm.setQuery("cak");
          vm.setQuery("cake");
        });
    waitForSearch();
    verify(libSvc, times(1)).resolveRecipes("cake");
    verify(libSvc, never()).resolveRecipes("c");
    verify(libSvc, never()).resolveRecipes("ca");
    verify(libSvc, never()).resolveRecipes("cak");
  }

  // ── S8: selectNextResult / selectPreviousResult cycle through results ──

  @Test
  void s8_keyboardNavigation_cyclesThroughResults() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery(""));
    waitForSearch(); // loads all 3

    Platform.runLater(() -> vm.selectNextResult());
    waitForFxEvents();
    String first = vm.getSelectedResultId();
    assertThat(first).isNotNull();

    Platform.runLater(() -> vm.selectNextResult());
    waitForFxEvents();
    String second = vm.getSelectedResultId();
    assertThat(second).isNotEqualTo(first);

    Platform.runLater(() -> vm.selectPreviousResult());
    waitForFxEvents();
    assertThat(vm.getSelectedResultId()).isEqualTo(first);
  }

  // ── S9: navigateToSelectedResult uses NavigationService ───────────────

  @Test
  void s9_navigateToSelectedResult_usesNavigationService() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("cake"));
    waitForSearch();
    Platform.runLater(
        () -> {
          vm.selectNextResult();
          vm.navigateToSelectedResult();
        });
    waitForFxEvents();
    assertThat(navSvc.getSelectedRecipeId()).isEqualTo("id-cake");
    assertThat(navSvc.getCurrentView()).isEqualTo(NavigationService.View.RECIPE_EDITOR);
  }

  // ── S10: Status message reflects result count ─────────────────────────

  @Test
  void s10_statusMessage_oneResult() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("cake"));
    waitForSearch();
    assertThat(vm.getStatusMessage()).isEqualTo("1 result");
  }

  @Test
  void s10_statusMessage_noResults() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery("zzznomatch"));
    waitForSearch();
    assertThat(vm.getStatusMessage()).isEqualTo("No results found");
  }

  // ── S11: Empty query + no filters returns all recipes ─────────────────

  @Test
  void s11_emptyQueryNoFilters_returnsAllRecipes() throws InterruptedException {
    Platform.runLater(() -> vm.setQuery(""));
    waitForSearch();
    verify(libSvc).listAllRecipes();
    assertThat(vm.getResultIds()).containsExactlyInAnyOrder("id-cake", "id-pasta", "id-cookie");
  }

  // ── Helper ────────────────────────────────────────────────────────────

  /** Waits for debounce (50ms) + background task + FX callback to all complete. */
  private void waitForSearch() throws InterruptedException {
    Thread.sleep(200);
    waitForFxEvents();
  }
}
