package app.cookyourbooks.gui.view;

import java.util.EnumMap;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.NavigationService.View;

@SuppressWarnings("NullAway.Init")
public class MainViewController {

  @FXML private StackPane contentArea;
  @FXML private Button libraryButton;
  @FXML private Button editorButton;
  @FXML private Button importButton;
  @FXML private Button searchButton;
  @FXML private Button shoppingListButton;

  private final NavigationService navigationService;
  private final Map<View, Node> viewNodes = new EnumMap<>(View.class);

  public MainViewController(NavigationService navigationService) {
    this.navigationService = navigationService;
  }

  public void setViewNode(View view, Node node) {
    viewNodes.put(view, node);
  }

  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    System.out.println("initialize() called!");
    libraryButton.setOnAction(e -> navigationService.navigateTo(View.LIBRARY));
    editorButton.setOnAction(e -> navigationService.navigateTo(View.RECIPE_EDITOR));
    importButton.setOnAction(e -> navigationService.navigateTo(View.IMPORT));
    searchButton.setOnAction(e -> navigationService.navigateTo(View.SEARCH));
    shoppingListButton.setOnAction(e -> navigationService.navigateTo(View.SHOPPING_LIST));

    navigationService
        .currentViewProperty()
        .addListener(
            (obs, oldView, newView) -> {
              System.out.println("NAV LISTENER FIRED: " + newView);
              showView(newView);
            });

    showView(navigationService.getCurrentView());
  }

  private void showView(View view) {
    System.out.println("SHOW VIEW CALLED: " + view);
    if (contentArea == null) {
      System.out.println("contentArea is null — skipping");
      return;
    }

    Node node = viewNodes.get(view);
    System.out.println("NODE = " + node);

    // Ensure node fills the StackPane
    if (node instanceof javafx.scene.layout.Region region) {
      region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    contentArea
        .getChildren()
        .setAll(node != null ? node : new Label(view.name() + " — not yet implemented"));

    libraryButton.getStyleClass().remove("nav-active");
    editorButton.getStyleClass().remove("nav-active");
    importButton.getStyleClass().remove("nav-active");
    searchButton.getStyleClass().remove("nav-active");
    shoppingListButton.getStyleClass().remove("nav-active");

    switch (view) {
      case LIBRARY -> libraryButton.getStyleClass().add("nav-active");
      case RECIPE_EDITOR -> editorButton.getStyleClass().add("nav-active");
      case IMPORT -> importButton.getStyleClass().add("nav-active");
      case SEARCH -> searchButton.getStyleClass().add("nav-active");
      case SHOPPING_LIST -> shoppingListButton.getStyleClass().add("nav-active");
      default -> throw new IllegalStateException("Unknown view: " + view);
    }
  }
}
