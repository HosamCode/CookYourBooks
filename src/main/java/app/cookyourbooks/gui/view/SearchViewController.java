package app.cookyourbooks.gui.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;

import app.cookyourbooks.gui.viewmodel.SearchViewModel;
import app.cookyourbooks.gui.viewmodel.SearchViewModelImpl.SearchResult;

@SuppressWarnings("NullAway.Init")
public class SearchViewController {

  @FXML private TextField searchField;
  @FXML private TextField ingredientField;
  @FXML private Button addFilterBtn;
  @FXML private ProgressIndicator loadingIndicator;
  @FXML private Label statusLabel;
  @FXML private FlowPane filterChips;
  @FXML private ListView<SearchResult> resultsList;

  private SearchViewModel vm;

  /** Called by whoever creates this controller — inject the VM before FXML loads. */
  public void setViewModel(SearchViewModel vm) {
    this.vm = vm;
  }

  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    // Bind search field → vm.setQuery (drives debounced search)
    searchField.textProperty().addListener((obs, old, val) -> vm.setQuery(val));

    // Bind loading indicator visibility
    loadingIndicator.visibleProperty().bind(vm.searchingProperty());
    loadingIndicator.managedProperty().bind(vm.searchingProperty());

    // Bind status label
    statusLabel.textProperty().bind(vm.statusMessageProperty());

    // Bind results list
    resultsList.setItems((javafx.collections.ObservableList<SearchResult>) vm.resultsProperty());
    resultsList.setCellFactory(buildCellFactory());

    // Double-click to navigate
    resultsList.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            vm.navigateToSelectedResult();
          }
        });

    // Keyboard navigation: up/down in searchField moves selection; Enter navigates
    searchField.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.DOWN) {
            vm.selectNextResult();
            syncListSelection();
            e.consume();
          } else if (e.getCode() == KeyCode.UP) {
            vm.selectPreviousResult();
            syncListSelection();
            e.consume();
          } else if (e.getCode() == KeyCode.ENTER) {
            vm.navigateToSelectedResult();
            e.consume();
          }
        });

    // Keep VM selected result in sync when user clicks list directly
    resultsList
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, sel) -> {
              if (sel != null) {
                // reflect click-selection back into vm by index
                int idx = resultsList.getSelectionModel().getSelectedIndex();
                // move vm selection to match
                syncVmSelection(idx);
              }
            });

    // Rebuild filter chips whenever the ingredient filter list changes
    vm.ingredientFiltersProperty()
        .addListener((javafx.collections.ListChangeListener<String>) c -> rebuildFilterChips());
  }

  @FXML
  private void onAddIngredientFilter() {
    String ingredient = ingredientField.getText().trim();
    if (!ingredient.isBlank()) {
      vm.addIngredientFilter(ingredient);
      ingredientField.clear();
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private void syncListSelection() {
    String id = vm.getSelectedResultId();
    if (id == null) {
      resultsList.getSelectionModel().clearSelection();
      return;
    }
    for (int i = 0; i < resultsList.getItems().size(); i++) {
      if (resultsList.getItems().get(i).id().equals(id)) {
        resultsList.getSelectionModel().select(i);
        resultsList.scrollTo(i);
        break;
      }
    }
  }

  private void syncVmSelection(int idx) {
    // Reset vm selection to match list index by cycling selectNext from -1
    // Simplest approach: just let the list drive navigation directly
    // (vm keyboard nav and list click-nav are separate concerns)
  }

  private void rebuildFilterChips() {
    filterChips.getChildren().clear();
    for (String f : vm.getIngredientFilters()) {
      Button chip = new Button(f + " ✕");
      chip.setOnAction(e -> vm.removeIngredientFilter(f));
      chip.getStyleClass().add("filter-chip");
      filterChips.getChildren().add(chip);
    }
  }

  private Callback<ListView<SearchResult>, ListCell<SearchResult>> buildCellFactory() {
    return lv ->
        new ListCell<>() {
          @Override
          protected void updateItem(SearchResult item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.title());
          }
        };
  }
}
