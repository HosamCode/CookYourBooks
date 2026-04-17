package app.cookyourbooks.gui;

import java.io.IOException;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.cookyourbooks.CybLibrary;
import app.cookyourbooks.gui.view.LibraryViewController;
import app.cookyourbooks.gui.view.MainViewController;
import app.cookyourbooks.gui.view.RecipeEditorViewController;
import app.cookyourbooks.gui.view.SearchViewController;
import app.cookyourbooks.gui.view.ShoppingListViewController;
import app.cookyourbooks.gui.viewmodel.LibraryViewModelImpl;
import app.cookyourbooks.gui.viewmodel.SearchViewModelImpl;
import app.cookyourbooks.gui.viewmodel.ShoppingListViewModelImpl;
import app.cookyourbooks.services.LibrarianServiceImpl;
import app.cookyourbooks.services.RecipeServiceImpl;
import app.cookyourbooks.services.TransformerServiceImpl;

public class CookYourBooksGuiApp extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(CookYourBooksGuiApp.class);

  @Override
  public void start(Stage primaryStage) throws Exception {
    // ── 1. Load the recipe library ──
    CybLibrary library = CybLibrary.load(Path.of("cyb-library.json"));

    // ── 2. Create services ──
    var librarianService =
        new LibrarianServiceImpl(
            library.getRecipeRepository(), library.getCollectionRepository(), library);
    var transformerService = new TransformerServiceImpl(library::getConversionRegistry);
    var recipeService =
        new RecipeServiceImpl(
            library.getRecipeRepository(),
            library.getCollectionRepository(),
            library.getConversionRegistry());
    LOG.info("Loaded {} collections", librarianService.listCollections().size());

    // ── 3. Create shared navigation ──
    var navigationService = new NavigationService();

    // ── 4. Create the main layout controller ──
    var mainController = new MainViewController(navigationService);

    // ── 5. Wire Library View ──
    var libraryVm =
        new LibraryViewModelImpl(
            librarianService, navigationService, java.time.Duration.ofSeconds(5));
    FXMLLoader libraryLoader = new FXMLLoader(getClass().getResource("/fxml/LibraryView.fxml"));
    libraryLoader.setControllerFactory(clazz -> new LibraryViewController(libraryVm));
    try {
      Parent libraryView = libraryLoader.load();
      mainController.setViewNode(NavigationService.View.LIBRARY, libraryView);
      libraryVm.refresh();
    } catch (IOException e) {
      LOG.error("Failed to load LibraryView.fxml", e);
    }

    // ── Wire Recipe Editor ──
    FXMLLoader editorLoader = new FXMLLoader(getClass().getResource("/fxml/RecipeEditorView.fxml"));
    editorLoader.setControllerFactory(
        clazz ->
            new RecipeEditorViewController(
                library.getRecipeRepository(), navigationService, transformerService));
    try {
      Parent editorView = editorLoader.load();
      mainController.setViewNode(NavigationService.View.RECIPE_EDITOR, editorView);
      LOG.info("Recipe Editor wired successfully");
    } catch (IOException e) {
      LOG.error("Failed to load RecipeEditorView.fxml", e);
    }

    // ── Wire Search & Filter ──
    var searchVm = new SearchViewModelImpl(librarianService, navigationService);
    FXMLLoader searchLoader = new FXMLLoader(getClass().getResource("/fxml/SearchView.fxml"));
    searchLoader.setControllerFactory(
        clazz -> {
          var controller = new SearchViewController();
          controller.setViewModel(searchVm);
          return controller;
        });
    try {
      Parent searchView = searchLoader.load();
      mainController.setViewNode(NavigationService.View.SEARCH, searchView);
      LOG.info("Search view registered");
    } catch (IOException e) {
      LOG.error("Failed to load SearchView.fxml", e);
    }

    // ── Wire Shopping List ──
    var shoppingListVm = new ShoppingListViewModelImpl(librarianService, recipeService);
    FXMLLoader shoppingListLoader =
        new FXMLLoader(getClass().getResource("/fxml/ShoppingListView.fxml"));
    shoppingListLoader.setControllerFactory(
        clazz -> new ShoppingListViewController(shoppingListVm));
    try {
      Parent shoppingListView = shoppingListLoader.load();
      mainController.setViewNode(NavigationService.View.SHOPPING_LIST, shoppingListView);
      LOG.info("Shopping List view registered");
    } catch (IOException e) {
      LOG.error("Failed to load ShoppingListView.fxml", e);
    }

    // TODO: Wire Import Interface (Angela)

    // ── 6. Load main layout AFTER views are registered ──
    FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
    mainLoader.setController(mainController);
    Parent root = mainLoader.load();

    // ── 7. Show the window ──
    Scene scene = new Scene(root, 960, 640);
    primaryStage.setTitle("CookYourBooks");
    primaryStage.setScene(scene);
    primaryStage.show();

    // ── 8. Temporary: load test recipe for development ──
    Platform.runLater(
        () -> {
          navigationService.navigateTo(NavigationService.View.LIBRARY);
          navigationService.navigateToRecipe("test-1");
        });
  }

  public static void main(String[] args) {
    launch(args);
  }
}
