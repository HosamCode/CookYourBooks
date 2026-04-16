package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;
import app.cookyourbooks.services.LibrarianService;

@SuppressWarnings("NullAway.Init")
class LibraryViewModelTest extends ViewModelTestBase {

  private LibrarianService librarianService;
  private NavigationService navigationService;
  private LibraryViewModelImpl vm;

  // Use a very short undo timeout so tests don't have to wait 5 seconds
  private static final Duration UNDO_TIMEOUT = Duration.ofMillis(50);

  @BeforeEach
  void setUp() throws InterruptedException {
    librarianService = mock(LibrarianService.class);
    navigationService = mock(NavigationService.class);
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          vm = new LibraryViewModelImpl(librarianService, navigationService, UNDO_TIMEOUT);
          latch.countDown();
        });
    latch.await();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /** Creates a mock RecipeCollection with the given ID, title, sourceType, and 0 recipes. */
  private static RecipeCollection mockCollection(String id, String title, SourceType type) {
    RecipeCollection col = mock(RecipeCollection.class);
    when(col.getId()).thenReturn(id);
    when(col.getTitle()).thenReturn(title);
    when(col.getSourceType()).thenReturn(type);
    when(col.getRecipes()).thenReturn(List.of());
    return col;
  }

  /** Creates a mock Recipe with the given ID and title. */
  private static Recipe mockRecipe(String id, String title) {
    Recipe r = mock(Recipe.class);
    when(r.getId()).thenReturn(id);
    when(r.getTitle()).thenReturn(title);
    return r;
  }

  /**
   * Runs refresh() on the FX thread and waits long enough for the background task and its success
   * callback to complete.
   */
  private void refreshAndWait() throws InterruptedException {
    Platform.runLater(() -> vm.refresh());
    Thread.sleep(200);
    waitForFxEvents();
  }

  // ── L1: refresh() populates collections ───────────────────────────────────

  @Test
  void l1_refreshPopulatesCollections() throws InterruptedException {
    RecipeCollection col = mockCollection("c1", "Italian", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(col));

    refreshAndWait();

    assertThat(vm.getCollectionIds()).containsExactly("c1");
  }

  // ── L2: collection entry exposes ID, title, source type, recipe count ─────

  @Test
  void l2_collectionSummaryExposesRequiredFields() throws InterruptedException {
    RecipeCollection col = mock(RecipeCollection.class);
    when(col.getId()).thenReturn("c1");
    when(col.getTitle()).thenReturn("Italian");
    when(col.getSourceType()).thenReturn(SourceType.PERSONAL);
    Recipe r = mockRecipe("r1", "Pasta");
    when(col.getRecipes()).thenReturn(List.of(r));
    when(librarianService.listCollections()).thenReturn(List.of(col));

    refreshAndWait();

    CollectionSummary summary = vm.collectionsProperty().get(0);
    assertThat(summary.id()).isEqualTo("c1");
    assertThat(summary.title()).isEqualTo("Italian");
    assertThat(summary.sourceType()).isEqualTo(SourceType.PERSONAL);
    assertThat(summary.recipeCount()).isEqualTo(1);
  }

  // ── L3: selectCollection updates selected collection and recipe list ───────

  @Test
  void l3_selectCollectionUpdatesRecipeList() throws InterruptedException {
    RecipeCollection col = mockCollection("c1", "Italian", SourceType.PERSONAL);
    Recipe r1 = mockRecipe("r1", "Pasta");
    Recipe r2 = mockRecipe("r2", "Pizza");
    when(librarianService.listCollections()).thenReturn(List.of(col));
    when(librarianService.listRecipes("Italian")).thenReturn(List.of(r1, r2));

    refreshAndWait();

    Platform.runLater(() -> vm.selectCollection("c1"));
    waitForFxEvents();

    assertThat(vm.getSelectedCollectionId()).isEqualTo("c1");
    assertThat(vm.getRecipeIds()).containsExactly("r1", "r2");
  }

  // ── L4: createCollection appears after refresh ─────────────────────────────

  @Test
  void l4_createCollectionAppearsAfterRefresh() throws InterruptedException {
    RecipeCollection existing = mockCollection("c1", "Italian", SourceType.PERSONAL);
    RecipeCollection created = mockCollection("c2", "Desserts", SourceType.PERSONAL);
    when(librarianService.listCollections())
        .thenReturn(List.of(existing))
        .thenReturn(List.of(existing, created));

    refreshAndWait();
    assertThat(vm.getCollectionIds()).containsExactly("c1");

    Platform.runLater(() -> vm.createCollection("Desserts"));
    Thread.sleep(200);
    waitForFxEvents();

    assertThat(vm.getCollectionIds()).containsExactly("c1", "c2");
  }

  // ── L5: deleteCollection removes collection after undo timeout ─────────────

  @Test
  void l5_deleteCollectionPermanentAfterTimeout() throws InterruptedException {
    RecipeCollection col = mockCollection("c1", "Italian", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(col));

    refreshAndWait();

    Platform.runLater(() -> vm.deleteCollection("c1"));
    waitForFxEvents();

    // Hidden immediately but not yet permanently deleted
    assertThat(vm.getCollectionIds()).doesNotContain("c1");
    verify(librarianService, never()).deleteCollection("c1");

    // Wait for timeout
    Thread.sleep(200);
    waitForFxEvents();

    verify(librarianService).deleteCollection("c1");
    assertThat(vm.isUndoAvailable()).isFalse();
  }

  // ── L6: undoDelete restores collection within the window ──────────────────

  @Test
  void l6_undoDeleteRestoresCollection() throws InterruptedException {
    RecipeCollection col = mockCollection("c1", "Italian", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(col));

    refreshAndWait();

    Platform.runLater(() -> vm.deleteCollection("c1"));
    waitForFxEvents();
    assertThat(vm.getCollectionIds()).doesNotContain("c1");

    // Undo within the window
    Platform.runLater(() -> vm.undoDelete());
    waitForFxEvents();

    assertThat(vm.getCollectionIds()).containsExactly("c1");
    assertThat(vm.isUndoAvailable()).isFalse();
    verify(librarianService, never()).deleteCollection("c1");
  }

  // ── L7: undo state clears after timeout ───────────────────────────────────

  @Test
  void l7_undoStateClearsAfterTimeout() throws InterruptedException {
    RecipeCollection col = mockCollection("c1", "Italian", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(col));

    refreshAndWait();

    Platform.runLater(() -> vm.deleteCollection("c1"));
    waitForFxEvents();
    assertThat(vm.isUndoAvailable()).isTrue();

    Thread.sleep(200);
    waitForFxEvents();

    assertThat(vm.isUndoAvailable()).isFalse();
    assertThat(vm.undoMessageProperty().get()).isEmpty();
  }

  // ── L8: refresh() runs on background thread; loading true while fetching ──

  @Test
  void l8_loadingIsTrueDuringRefresh() throws InterruptedException {
    CountDownLatch serviceStarted = new CountDownLatch(1);
    CountDownLatch proceedLatch = new CountDownLatch(1);

    when(librarianService.listCollections())
        .thenAnswer(
            inv -> {
              serviceStarted.countDown();
              proceedLatch.await();
              return List.of();
            });

    Platform.runLater(() -> vm.refresh());
    waitForFxEvents(); // let refresh() set loading=true and start thread

    serviceStarted.await(); // background thread is now blocked inside listCollections()
    assertThat(vm.isLoading()).isTrue();

    proceedLatch.countDown(); // unblock the background thread
    Thread.sleep(100);
    waitForFxEvents(); // let success callback run

    assertThat(vm.isLoading()).isFalse();
  }

  // ── L9: selecting a recipe navigates ──────────────────────────────────────

  @Test
  void l9_selectRecipeNavigatesToEditor() throws InterruptedException {
    Platform.runLater(() -> vm.selectRecipe("r1"));
    waitForFxEvents();

    verify(navigationService).navigateToRecipe("r1");
  }

  // ── L10: nonexistent collection ID is handled gracefully ──────────────────

  @Test
  void l10_selectNonexistentCollectionIsNoop() throws InterruptedException {
    when(librarianService.listCollections()).thenReturn(List.of());
    refreshAndWait();

    // Should not throw and should not change selected ID
    Platform.runLater(() -> vm.selectCollection("does-not-exist"));
    waitForFxEvents();

    assertThat(vm.getSelectedCollectionId()).isNull();
    assertThat(vm.getRecipeIds()).isEmpty();
  }

  // ── L11: filterTextProperty filters case-insensitively ────────────────────

  @Test
  void l11_filterIsCaseInsensitive() throws InterruptedException {
    RecipeCollection col1 = mockCollection("c1", "Italian Food", SourceType.PERSONAL);
    RecipeCollection col2 = mockCollection("c2", "Desserts", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(col1, col2));
    refreshAndWait();

    Platform.runLater(() -> vm.filterTextProperty().set("italian"));
    waitForFxEvents();

    assertThat(vm.getCollectionIds()).containsExactly("c1");
    assertThat(vm.getCollectionIds()).doesNotContain("c2");
  }

  // ── L12: filter updates immediately as text changes ───────────────────────

  @Test
  void l12_filterUpdatesImmediately() throws InterruptedException {
    RecipeCollection col1 = mockCollection("c1", "Italian Food", SourceType.PERSONAL);
    RecipeCollection col2 = mockCollection("c2", "Desserts", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(col1, col2));
    refreshAndWait();

    // Change filter and immediately check — no debounce
    Platform.runLater(
        () -> {
          vm.filterTextProperty().set("Des");
          // listener fires synchronously on FX thread — list is already filtered
          assertThat(vm.getCollectionIds()).containsExactly("c2");
        });
    waitForFxEvents();

    assertThat(vm.getCollectionIds()).containsExactly("c2");
  }

  // ── L13: undo with active filter only restores if collection matches ───────

  @Test
  void l13_undoWithFilterOnlyRestoresIfMatches() throws InterruptedException {
    RecipeCollection col1 = mockCollection("c1", "Italian Food", SourceType.PERSONAL);
    RecipeCollection col2 = mockCollection("c2", "Desserts", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(col1, col2));
    refreshAndWait();

    // Delete "Desserts"
    Platform.runLater(() -> vm.deleteCollection("c2"));
    waitForFxEvents();

    // Apply a filter that does NOT match "Desserts"
    Platform.runLater(() -> vm.filterTextProperty().set("Italian"));
    waitForFxEvents();

    // Undo — "Desserts" is restored in allCollections but still excluded by filter
    Platform.runLater(() -> vm.undoDelete());
    waitForFxEvents();

    assertThat(vm.isUndoAvailable()).isFalse();
    assertThat(vm.getCollectionIds()).containsExactly("c1");
    assertThat(vm.getCollectionIds()).doesNotContain("c2");

    // Clear filter — both collections should now appear
    Platform.runLater(() -> vm.filterTextProperty().set(""));
    waitForFxEvents();

    assertThat(vm.getCollectionIds()).containsExactlyInAnyOrder("c1", "c2");
    verify(librarianService, atLeastOnce()).listCollections();
  }
}
