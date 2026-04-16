package app.cookyourbooks.gui.viewmodel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;

public class SearchViewModelImpl implements SearchViewModel {

  // ── Result entry record ───────────────────────────────────────────────
  public record SearchResult(String id, String title) {}

  // ── Services ──────────────────────────────────────────────────────────
  private final LibrarianService librarianService;
  private final NavigationService navigationService;
  private final javafx.util.Duration debounceDelay;

  // ── Observable state ──────────────────────────────────────────────────
  private final StringProperty query = new SimpleStringProperty("");
  private final ObservableList<SearchResult> results = FXCollections.observableArrayList();
  private final ObservableList<String> ingredientFilters = FXCollections.observableArrayList();
  private final BooleanProperty searching = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");

  // ── Internal state ────────────────────────────────────────────────────
  private @Nullable SearchResult selectedResult = null;
  private final AtomicInteger searchGeneration = new AtomicInteger(0);
  private @Nullable PauseTransition debounceTimer = null;

  // ── Constructors ──────────────────────────────────────────────────────

  public SearchViewModelImpl(
      LibrarianService librarianService,
      NavigationService navigationService,
      Duration debounceDelay) {
    this.librarianService = librarianService;
    this.navigationService = navigationService;
    this.debounceDelay = javafx.util.Duration.millis(debounceDelay.toMillis());
  }

  /** Production constructor — uses 300ms debounce. */
  public SearchViewModelImpl(
      LibrarianService librarianService, NavigationService navigationService) {
    this(librarianService, navigationService, Duration.ofMillis(300));
  }

  // ── SearchViewModel — Observable properties ───────────────────────────

  @Override
  public StringProperty queryProperty() {
    return query;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ObservableList<SearchResult> resultsProperty() {
    return results;
  }

  @Override
  public ObservableList<String> ingredientFiltersProperty() {
    return ingredientFilters;
  }

  @Override
  public BooleanProperty searchingProperty() {
    return searching;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  // ── SearchViewModel — Commands ────────────────────────────────────────

  @Override
  public void setQuery(String q) {
    query.set(q);
    scheduleDebounce();
  }

  @Override
  public void addIngredientFilter(String ingredient) {
    if (!ingredientFilters.contains(ingredient)) {
      ingredientFilters.add(ingredient);
    }
    runSearch(); // immediate — no debounce for filter changes
  }

  @Override
  public void removeIngredientFilter(String ingredient) {
    ingredientFilters.remove(ingredient);
    runSearch(); // immediate
  }

  @Override
  public void clearFilters() {
    query.set("");
    ingredientFilters.clear();
    runSearch(); // immediate
  }

  @Override
  public void selectNextResult() {
    if (results.isEmpty()) {
      return;
    }
    int idx = selectedResult == null ? 0 : results.indexOf(selectedResult) + 1;
    if (idx >= results.size()) {
      idx = 0;
    }
    selectedResult = results.get(idx);
  }

  @Override
  public void selectPreviousResult() {
    if (results.isEmpty()) {
      return;
    }
    int idx = selectedResult == null ? results.size() - 1 : results.indexOf(selectedResult) - 1;
    if (idx < 0) {
      idx = results.size() - 1;
    }
    selectedResult = results.get(idx);
  }

  @Override
  public void navigateToSelectedResult() {
    if (selectedResult != null) {
      navigationService.navigateToRecipe(selectedResult.id());
    }
  }

  // ── SearchViewModel — Non-JavaFX accessors ────────────────────────────

  @Override
  public String getQuery() {
    return query.get();
  }

  @Override
  public List<String> getResultIds() {
    return results.stream().map(SearchResult::id).toList();
  }

  @Override
  public List<String> getIngredientFilters() {
    return List.copyOf(ingredientFilters);
  }

  @Override
  public boolean isSearching() {
    return searching.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  @Override
  public @Nullable String getSelectedResultId() {
    return selectedResult == null ? null : selectedResult.id();
  }

  // ── Private helpers ───────────────────────────────────────────────────

  private void scheduleDebounce() {
    if (debounceTimer != null) {
      debounceTimer.stop();
    }
    debounceTimer = new PauseTransition(debounceDelay);
    debounceTimer.setOnFinished(e -> runSearch());
    debounceTimer.play();
  }

  private void runSearch() {
    String currentQuery = query.get();
    List<String> currentFilters = List.copyOf(ingredientFilters);

    // Increment generation — any in-flight search with a stale generation is discarded
    int gen = searchGeneration.incrementAndGet();

    searching.set(true);
    statusMessage.set("Searching...");

    BackgroundTaskRunner.run(
        () -> fetchResults(currentQuery, currentFilters),
        recipes -> {
          if (searchGeneration.get() != gen) {
            return; // stale result — discard
          }
          List<SearchResult> mapped =
              recipes.stream().map(r -> new SearchResult(r.getId(), r.getTitle())).toList();
          results.setAll(mapped);
          selectedResult = null;
          int count = mapped.size();
          statusMessage.set(
              count == 0 ? "No results found" : count + " result" + (count == 1 ? "" : "s"));
          searching.set(false);
        },
        err -> {
          if (searchGeneration.get() != gen) {
            return;
          }
          statusMessage.set("Search failed: " + err.getMessage());
          searching.set(false);
        });
  }

  /** Runs on background thread. Fetches and intersects results. */
  private List<Recipe> fetchResults(String q, List<String> filters) {
    boolean emptyQuery = q == null || q.isBlank();
    boolean noFilters = filters.isEmpty();

    List<Recipe> base;
    if (emptyQuery && noFilters) {
      // S11: return all recipes
      base = librarianService.listAllRecipes();
    } else if (emptyQuery) {
      // Only ingredient filters — start from all recipes
      base = new ArrayList<>(librarianService.listAllRecipes());
    } else {
      base = new ArrayList<>(librarianService.resolveRecipes(q));
    }

    // AND-intersect each ingredient filter
    for (String ingredient : filters) {
      List<Recipe> byIngredient = librarianService.searchByIngredient(ingredient);
      base.retainAll(byIngredient);
    }

    return base;
  }
}
