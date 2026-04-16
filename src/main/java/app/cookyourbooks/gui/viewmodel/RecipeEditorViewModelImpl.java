package app.cookyourbooks.gui.viewmodel;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.EditableIngredient;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.TransformerService;

public class RecipeEditorViewModelImpl implements RecipeEditorViewModel {

  private final RecipeRepository recipeRepository;
  private final TransformerService transformerService;

  private @Nullable Recipe originalRecipe;
  private boolean suppressDirtyTracking = false;

  private final StringProperty title = new SimpleStringProperty("");
  private final BooleanProperty editing = new SimpleBooleanProperty(false);
  private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
  private final BooleanProperty isValid = new SimpleBooleanProperty(false);
  private final BooleanProperty isSaving = new SimpleBooleanProperty(false);
  private final BooleanProperty recipeLoaded = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");

  private final ObservableList<EditableIngredient> ingredients =
      FXCollections.observableArrayList(
          item ->
              new javafx.beans.Observable[] {
                item.nameProperty(), item.quantityProperty(), item.unitProperty()
              });

  private final ObservableList<String> instructions = FXCollections.observableArrayList();

  public RecipeEditorViewModelImpl(
      RecipeRepository recipeRepository, TransformerService transformerService) {
    this.recipeRepository = recipeRepository;
    this.transformerService = transformerService;
    wireListeners();
    isValid.set(false);
  }

  private void wireListeners() {
    title.addListener(
        (obs, oldVal, newVal) -> {
          isValid.set(newVal != null && !newVal.isBlank());
          if (!suppressDirtyTracking) {
            isDirty.set(true);
          }
        });
    ingredients.addListener(
        (javafx.collections.ListChangeListener<EditableIngredient>)
            change -> {
              if (!suppressDirtyTracking) {
                isDirty.set(true);
              }
            });
  }

  @Override
  public void loadRecipe(String recipeId) {
    Recipe recipe =
        recipeRepository
            .findById(recipeId)
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + recipeId));
    originalRecipe = recipe;
    loadRecipeData(recipe);
    recipeLoaded.set(true);
    statusMessage.set("");
  }

  private void loadRecipeData(Recipe recipe) {
    suppressDirtyTracking = true;
    try {
      title.set(recipe.getTitle());
      ingredients.setAll(
          recipe.getIngredients().stream()
              .map(EditableIngredient::from)
              .collect(Collectors.toList()));
      instructions.setAll(
          recipe.getInstructions().stream()
              .map(i -> i.getStepNumber() + ". " + i.getText())
              .collect(Collectors.toList()));
      isDirty.set(false);
      editing.set(false);
    } finally {
      suppressDirtyTracking = false;
    }
  }

  public void scaleRecipe(int targetServings) {
    if (originalRecipe == null) {
      return;
    }
    var result = transformerService.scale(originalRecipe, targetServings);
    suppressDirtyTracking = true;
    try {
      ingredients.setAll(
          result.scaled().getIngredients().stream()
              .map(EditableIngredient::from)
              .collect(Collectors.toList()));
    } finally {
      suppressDirtyTracking = false;
    }
  }

  public BooleanProperty recipeLoadedProperty() {
    return recipeLoaded;
  }

  @Override
  public void toggleEditMode() {
    editing.set(!editing.get());
  }

  @Override
  public void discardChanges() {
    if (originalRecipe == null) {
      return;
    }
    loadRecipeData(originalRecipe);
    statusMessage.set("");
  }

  @Override
  public void addIngredient() {
    ingredients.add(new EditableIngredient(""));
  }

  @Override
  public void removeIngredient(int index) {
    if (index >= 0 && index < ingredients.size()) {
      ingredients.remove(index);
    }
  }

  @Override
  public void save() {
    if (!editing.get() || !isDirty.get() || !isValid.get()) {
      return;
    }
    if (isSaving.get() || originalRecipe == null) {
      return;
    }
    Recipe updatedRecipe = buildRecipeFromCurrentState();
    isSaving.set(true);
    statusMessage.set("Saving...");
    var unused =
        BackgroundTaskRunner.run(
            () -> {
              recipeRepository.save(updatedRecipe);
              return updatedRecipe;
            },
            savedRecipe -> {
              originalRecipe = savedRecipe;
              isSaving.set(false);
              loadRecipeData(savedRecipe);
              statusMessage.set("Saved successfully.");
            },
            error -> {
              isSaving.set(false);
              statusMessage.set("Save failed: " + error.getMessage());
            });
  }

  @SuppressWarnings("NullAway")
  private Recipe buildRecipeFromCurrentState() {
    Recipe original = originalRecipe;
    List<Ingredient> domainIngredients =
        ingredients.stream()
            .map(EditableIngredient::toDomain)
            .filter(i -> i != null)
            .collect(Collectors.toList());
    return new Recipe(
        original.getId(),
        title.get(),
        original.getServings(),
        domainIngredients,
        original.getInstructions(),
        original.getConversionRules());
  }

  @Override
  public StringProperty titleProperty() {
    return title;
  }

  @Override
  public BooleanProperty editingProperty() {
    return editing;
  }

  @Override
  public BooleanProperty isDirtyProperty() {
    return isDirty;
  }

  @Override
  public BooleanProperty isValidProperty() {
    return isValid;
  }

  @Override
  public BooleanProperty isSavingProperty() {
    return isSaving;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  @Override
  public ObservableList<EditableIngredient> ingredientsProperty() {
    return ingredients;
  }

  public ObservableList<String> instructionsProperty() {
    return instructions;
  }

  @Override
  public @Nullable String getRecipeId() {
    return originalRecipe != null ? originalRecipe.getId() : null;
  }

  @Override
  public String getTitle() {
    return title.get();
  }

  @Override
  public boolean isEditing() {
    return editing.get();
  }

  @Override
  public boolean isDirty() {
    return isDirty.get();
  }

  @Override
  public boolean isValid() {
    return isValid.get();
  }

  @Override
  public boolean isSaving() {
    return isSaving.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  @Override
  public int getIngredientCount() {
    return ingredients.size();
  }

  @Override
  public List<String> getIngredientNames() {
    return ingredients.stream().map(EditableIngredient::getName).collect(Collectors.toList());
  }
}
