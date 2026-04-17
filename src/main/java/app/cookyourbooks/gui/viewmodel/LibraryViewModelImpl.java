package app.cookyourbooks.gui.viewmodel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.services.LibrarianService;

/** Implementation of {@link LibraryViewModel} for browsing and managing recipe collections. */
public final class LibraryViewModelImpl implements LibraryViewModel {

  // ── Services ──────────────────────────────────────────────────────────────
  private final LibrarianService librarianService;
  private final NavigationService navigationService;
  private final javafx.util.Duration undoTimeout;

  // ── Internal state (not observed by the View) ─────────────────────────────
  private List<CollectionSummary> allCollections = new ArrayList<>();
  private @Nullable CollectionSummary pendingDelete = null;
  private @Nullable PauseTransition undoTimer = null;

  // ── Observable properties ─────────────────────────────────────────────────
  private final ObservableList<CollectionSummary> collections = FXCollections.observableArrayList();
  private final StringProperty filterText = new SimpleStringProperty("");
  private final ObservableList<RecipeSummary> recipes = FXCollections.observableArrayList();
  private final BooleanProperty loading = new SimpleBooleanProperty(false);
  private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
  private final StringProperty undoMessage = new SimpleStringProperty("");
  private final StringProperty selectedCollectionId = new SimpleStringProperty();

  /**
   * Constructs a new LibraryViewModelImpl.
   *
   * @param librarianService the service used to load and manage collections
   * @param navigationService the shared navigation service used to open recipes in the editor
   * @param undoTimeout how long the undo window stays open after a delete
   */
  public LibraryViewModelImpl(
      LibrarianService librarianService,
      NavigationService navigationService,
      Duration undoTimeout) {
    this.librarianService = librarianService;
    this.navigationService = navigationService;
    this.undoTimeout = javafx.util.Duration.millis((double) undoTimeout.toMillis());

    // Re-derive the visible list whenever the filter text changes.
    filterText.addListener((obs, oldVal, newVal) -> applyFilter());

    // Pre-populate synchronously so the Library view shows data immediately on first open.
    var rawCollections = librarianService.listCollections();
    allCollections =
        rawCollections.stream()
            .map(
                c ->
                    new CollectionSummary(
                        c.getId(), c.getTitle(), c.getSourceType(), c.getRecipes().size()))
            .toList();
    applyFilter();
  }

  // ── Observable property accessors ─────────────────────────────────────────

  @Override
  public ObservableList<CollectionSummary> collectionsProperty() {
    return collections;
  }

  @Override
  public StringProperty filterTextProperty() {
    return filterText;
  }

  @Override
  public ObservableList<RecipeSummary> recipesProperty() {
    return recipes;
  }

  @Override
  public BooleanProperty loadingProperty() {
    return loading;
  }

  @Override
  public BooleanProperty undoAvailableProperty() {
    return undoAvailable;
  }

  @Override
  public StringProperty undoMessageProperty() {
    return undoMessage;
  }

  // ── Commands ──────────────────────────────────────────────────────────────

  @Override
  public void refresh() {
    loading.set(true);
    var unused =
        BackgroundTaskRunner.run(
            librarianService::listCollections,
            result -> {
              allCollections =
                  result.stream()
                      .map(
                          c ->
                              new CollectionSummary(
                                  c.getId(),
                                  c.getTitle(),
                                  c.getSourceType(),
                                  c.getRecipes().size()))
                      .toList();
              applyFilter();
              loading.set(false);
            },
            error -> loading.set(false));
  }

  @Override
  public void selectCollection(String collectionId) {
    // L10: look up in allCollections so nonexistent IDs are handled gracefully
    var found = allCollections.stream().filter(c -> c.id().equals(collectionId)).findFirst();
    if (found.isEmpty()) {
      return;
    }
    CollectionSummary summary = found.get();
    selectedCollectionId.set(collectionId);

    // listRecipes takes the collection title, not its ID
    List<RecipeSummary> summaries =
        librarianService.listRecipes(summary.title()).stream()
            .map(r -> new RecipeSummary(r.getId(), r.getTitle()))
            .toList();
    recipes.setAll(summaries);
  }

  @Override
  public void createCollection(String title) {
    librarianService.createCollection(title);
    refresh();
  }

  @Override
  public void deleteCollection(String collectionId) {
    var found = allCollections.stream().filter(c -> c.id().equals(collectionId)).findFirst();
    if (found.isEmpty()) {
      return;
    }

    // If there is already a pending delete, commit it before starting a new undo window
    if (pendingDelete != null && undoTimer != null) {
      undoTimer.stop();
      commitDelete();
    }

    pendingDelete = found.get();
    undoAvailable.set(true);
    undoMessage.set("Deleted: " + pendingDelete.title());
    applyFilter(); // hide from visible list immediately

    undoTimer = new PauseTransition(undoTimeout);
    undoTimer.setOnFinished(e -> commitDelete());
    undoTimer.play();
  }

  @Override
  public void undoDelete() {
    if (pendingDelete == null) {
      return;
    }
    if (undoTimer != null) {
      undoTimer.stop();
      undoTimer = null;
    }
    pendingDelete = null;
    undoAvailable.set(false);
    undoMessage.set("");
    applyFilter(); // restore in the visible list (filtered out only if text doesn't match — L13)
  }

  @Override
  public void selectRecipe(String recipeId) {
    navigationService.navigateToRecipe(recipeId);
  }

  // ── Non-JavaFX accessors (for grading tests) ──────────────────────────────

  @Override
  public List<String> getCollectionIds() {
    return collections.stream().map(CollectionSummary::id).toList();
  }

  @Override
  public @Nullable String getSelectedCollectionId() {
    return selectedCollectionId.get();
  }

  @Override
  public List<String> getRecipeIds() {
    return recipes.stream().map(RecipeSummary::id).toList();
  }

  @Override
  public boolean isLoading() {
    return loading.get();
  }

  @Override
  public boolean isUndoAvailable() {
    return undoAvailable.get();
  }

  @Override
  public String getFilterText() {
    return filterText.get();
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private void applyFilter() {
    String text = filterText.get().toLowerCase(Locale.ROOT);
    List<CollectionSummary> visible =
        allCollections.stream()
            .filter(c -> pendingDelete == null || !c.id().equals(pendingDelete.id()))
            .filter(c -> text.isEmpty() || c.title().toLowerCase(Locale.ROOT).contains(text))
            .toList();
    collections.setAll(visible);
  }

  /** Permanently deletes the pending collection and clears undo state. */
  private void commitDelete() {
    if (pendingDelete != null) {
      librarianService.deleteCollection(pendingDelete.id());
      pendingDelete = null;
    }
    undoTimer = null;
    undoAvailable.set(false);
    undoMessage.set("");
  }
}
