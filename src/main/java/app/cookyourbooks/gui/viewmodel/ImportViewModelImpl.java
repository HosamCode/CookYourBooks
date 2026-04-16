package app.cookyourbooks.gui.viewmodel;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nullable;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.RecipeOcrService;

/**
 * ViewModel implementation for the Import Interface feature.
 *
 * <p>Import recipes from images using OCR. This ViewModel manages a state machine for the import
 * workflow and supports pre-save editing of the extracted recipe.
 *
 * <h2>State machine</h2>
 *
 * <p>Your ViewModel must manage these state transitions:
 *
 * <pre>
 *   IDLE ──startImport()──▶ PROCESSING ──(success)──▶ REVIEW ──acceptImport()──▶ IDLE
 *                               │                       │
 *                               │                       └──rejectImport()──▶ IDLE
 *                               │
 *                               └──(failure)──▶ ERROR ──(reset)──▶ IDLE
 *                               │
 *                               └──cancelImport()──▶ IDLE
 * </pre>
 *
 * <p>You define your own state enum or representation. The grading accessor {@link #getState()}
 * returns a string so tests don't depend on your enum type. Return one of: {@code "idle"}, {@code
 * "processing"}, {@code "review"}, or {@code "error"}.
 *
 * <h2>Threading</h2>
 *
 * <p>Use {@code BackgroundTaskRunner} to run the OCR operation on a background thread. Inject
 * {@code FakeRecipeOcrService} (provided in the handout test fixtures) for development and testing.
 *
 * <h2>Requirement mapping</h2>
 *
 * <ul>
 *   <li><b>I1:</b> Initial state is idle; no imported recipe, no error
 *   <li><b>I2:</b> {@link #startImport(Path)} transitions to processing
 *   <li><b>I3:</b> Successful OCR transitions to review; imported recipe is populated
 *   <li><b>I4:</b> OCR failure transitions to error; error message is populated
 *   <li><b>I5:</b> {@link #cancelImport()} during processing transitions back to idle
 *   <li><b>I6:</b> {@link #acceptImport()} saves to selected collection and transitions to idle
 *   <li><b>I7:</b> {@link #rejectImport()} discards and transitions to idle
 *   <li><b>I8:</b> Available collections are loaded from the repository
 *   <li><b>I9:</b> Pre-save editing: title/ingredients can be modified before accept
 *   <li><b>I10:</b> {@link #acceptImport()} with no collection or no recipe is a no-op
 * </ul>
 */
public class ImportViewModelImpl implements ImportViewModel {

  private final RecipeOcrService ocrService;
  private final LibrarianService librarianService;

  private final StringProperty state = new SimpleStringProperty("idle");
  private final DoubleProperty progress = new SimpleDoubleProperty(0);
  private final StringProperty statusMessage = new SimpleStringProperty("");
  private final StringProperty errorMessage = new SimpleStringProperty();
  private final ObjectProperty<Recipe> importedRecipe = new SimpleObjectProperty<>();
  private final ObservableList<String> availableCollections = FXCollections.observableArrayList();
  private final StringProperty selectedCollectionId = new SimpleStringProperty();

  public ImportViewModelImpl(RecipeOcrService ocrService, LibrarianService librarianService) {
    this.ocrService = ocrService;
    this.librarianService = librarianService;
  }

  @Override
  public void startImport(Path imagePath) {
    state.set("processing");
    statusMessage.set("Extracting recipe...");
    BackgroundTaskRunner.run(
        () -> ocrService.extractRecipe(imagePath),
        recipe -> {
          importedRecipe.set(recipe);
          state.set("review");
          statusMessage.set("Review the imported recipe.");
        },
        error -> {
          errorMessage.set("Failed to extract recipe: " + error.getMessage());
          state.set("error");
        });
  }

  @Override
  public void cancelImport() {
    state.set("idle");
    statusMessage.set("Import canceled.");
  }

  @Override
  public void acceptImport() {
    if (state.get().equals("review")
        && importedRecipe.get() != null
        && selectedCollectionId.get() != null) {
      librarianService.saveRecipe(importedRecipe.get(), selectedCollectionId.get());
      state.set("idle");
      statusMessage.set("Recipe imported successfully.");
    }
  }

  @Override
  public void rejectImport() {
    state.set("idle");
    importedRecipe.set(null);
    statusMessage.set("Import rejected.");
  }

  @Override
  public void loadCollections() {
    availableCollections.setAll(
        librarianService.listCollections().stream().map(collection -> collection.getId()).toList());
  }

  @Override
  public StringProperty stateProperty() {
    return state;
  }

  @Override
  public DoubleProperty progressProperty() {
    return progress;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  @Override
  public StringProperty errorMessageProperty() {
    return errorMessage;
  }

  @Override
  public ObjectProperty<Recipe> importedRecipeProperty() {
    return importedRecipe;
  }

  @Override
  public ObservableList<String> availableCollectionsProperty() {
    return availableCollections;
  }

  @Override
  public StringProperty selectedCollectionIdProperty() {
    return selectedCollectionId;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Non-JavaFX accessors (for grading tests)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Returns the current state as a lowercase string: {@code "idle"}, {@code "processing"}, {@code
   * "review"}, or {@code "error"}.
   */
  public String getState() {
    return state.get();
  }

  /** Returns the current status message. */
  public String getStatusMessage() {
    return statusMessage.get();
  }

  /** Returns the current error message, or null if not in error state. */
  @Nullable
  public String getErrorMessage() {
    return errorMessage.get();
  }

  /** Returns the title of the imported recipe, or null if not in review state. */
  @Nullable
  public String getImportedRecipeTitle() {
    return importedRecipe.get() != null ? importedRecipe.get().getTitle() : null;
  }

  /** Returns the ingredient names of the imported recipe. Empty if not in review state. */
  public List<Ingredient> getImportedIngredientNames() {
    return importedRecipe.get() != null ? importedRecipe.get().getIngredients() : List.of();
  }

  /** Returns the IDs of the available collections. */
  public List<String> getAvailableCollectionIds() {
    return availableCollections;
  }

  /** Returns the ID of the selected target collection, or null if none. */
  @Nullable
  public String getSelectedCollectionId() {
    return selectedCollectionId.get();
  }
}
