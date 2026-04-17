package app.cookyourbooks.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;
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

    // ── Seed sample recipes if the library is empty ──
    if (librarianService.listCollections().isEmpty()) {
      var col = librarianService.createCollection("My Recipes");
      librarianService.saveRecipe(makePancakes(), col.getId());
      librarianService.saveRecipe(makeTomatoPasta(), col.getId());
      librarianService.saveRecipe(makeChocolateChipCookies(), col.getId());
      LOG.info("Seeded sample recipes into 'My Recipes'");
    }
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
    libraryLoader.setController(new LibraryViewController(libraryVm));
    try {
      Parent libraryView = libraryLoader.load();
      mainController.setViewNode(NavigationService.View.LIBRARY, libraryView);
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

    Platform.runLater(() -> navigationService.navigateTo(NavigationService.View.LIBRARY));
  }

  // ── Sample recipe factories ────────────────────────────────────────────────

  private static Recipe makePancakes() {
    List<Ingredient> ingredients =
        List.of(
            new MeasuredIngredient("flour", new ExactQuantity(2.0, Unit.CUP), null, null),
            new MeasuredIngredient("milk", new ExactQuantity(1.0, Unit.CUP), null, null),
            new MeasuredIngredient("egg", new ExactQuantity(1.0, Unit.WHOLE), null, null),
            new VagueIngredient("salt", "to taste", null, null));
    List<Instruction> instructions =
        List.of(
            new Instruction(1, "Mix flour, milk, and egg until smooth.", List.of()),
            new Instruction(2, "Heat a lightly oiled griddle over medium heat.", List.of()),
            new Instruction(
                3, "Pour batter onto griddle and cook until bubbles form, then flip.", List.of()));
    return new Recipe(
        "Pancakes", new Servings(4, "pancakes"), ingredients, instructions, List.of());
  }

  private static Recipe makeTomatoPasta() {
    List<Ingredient> ingredients =
        List.of(
            new MeasuredIngredient("pasta", new ExactQuantity(200.0, Unit.GRAM), null, null),
            new MeasuredIngredient(
                "canned tomatoes", new ExactQuantity(400.0, Unit.GRAM), null, null),
            new VagueIngredient("garlic", null, "minced", null),
            new VagueIngredient("olive oil", "to taste", null, null));
    List<Instruction> instructions =
        List.of(
            new Instruction(1, "Cook pasta according to package instructions.", List.of()),
            new Instruction(
                2, "Sauté garlic in olive oil, add tomatoes and simmer 10 minutes.", List.of()),
            new Instruction(3, "Toss pasta with sauce and serve.", List.of()));
    return new Recipe(
        "Tomato Pasta", new Servings(2, "servings"), ingredients, instructions, List.of());
  }

  private static Recipe makeChocolateChipCookies() {
    List<Ingredient> ingredients =
        List.of(
            new MeasuredIngredient("butter", new ExactQuantity(1.0, Unit.CUP), "softened", null),
            new MeasuredIngredient("sugar", new ExactQuantity(0.75, Unit.CUP), null, null),
            new MeasuredIngredient("flour", new ExactQuantity(2.25, Unit.CUP), null, null),
            new MeasuredIngredient("chocolate chips", new ExactQuantity(2.0, Unit.CUP), null, null),
            new VagueIngredient("vanilla extract", "1 tsp", null, null));
    List<Instruction> instructions =
        List.of(
            new Instruction(1, "Cream butter and sugar together until fluffy.", List.of()),
            new Instruction(2, "Mix in flour gradually, then fold in chocolate chips.", List.of()),
            new Instruction(
                3,
                "Drop spoonfuls onto a baking sheet and bake at 375°F for 9-11 minutes.",
                List.of()));
    return new Recipe(
        "Chocolate Chip Cookies",
        new Servings(24, "cookies"),
        ingredients,
        instructions,
        List.of());
  }

  public static void main(String[] args) {
    launch(args);
  }
}
