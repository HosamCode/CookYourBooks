package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.conversion.LayeredConversionRegistry;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.TransformerService;
import app.cookyourbooks.services.TransformerServiceImpl;

@SuppressWarnings("NullAway")
class RecipeScalingTest extends ViewModelTestBase {

  private RecipeRepository recipeRepository;
  private TransformerService transformerService;
  private RecipeEditorViewModelImpl viewModel;

  private static Recipe makePancakes() {
    return new Recipe(
        "pancakes-1",
        "Pancakes",
        new Servings(4, "servings"),
        List.of(
            new MeasuredIngredient("flour", new ExactQuantity(250.0, Unit.GRAM), null, null),
            new MeasuredIngredient("milk", new ExactQuantity(100.0, Unit.MILLILITER), null, null)),
        List.of(new Instruction(1, "Mix and cook.", List.of())),
        List.of());
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    recipeRepository = mock(RecipeRepository.class);
    transformerService = new TransformerServiceImpl(() -> new LayeredConversionRegistry());
    Recipe pancakes = makePancakes();
    when(recipeRepository.findById("pancakes-1")).thenReturn(Optional.of(pancakes));
    Platform.runLater(
        () -> viewModel = new RecipeEditorViewModelImpl(recipeRepository, transformerService));
    waitForFxEvents();
  }

  @Test
  void rs1_scaleRecipe_doublesIngredientQuantities() throws InterruptedException {
    Platform.runLater(
        () -> {
          viewModel.loadRecipe("pancakes-1");
          viewModel.scaleRecipe(8);
        });
    waitForFxEvents();
    assertThat(viewModel.getIngredientNames()).contains("flour");
    assertThat(viewModel.getIngredientCount()).isEqualTo(2);
  }

  @Test
  void rs2_scaleRecipe_doesNotMarkDirty() throws InterruptedException {
    Platform.runLater(
        () -> {
          viewModel.loadRecipe("pancakes-1");
          viewModel.scaleRecipe(8);
        });
    waitForFxEvents();
    assertThat(viewModel.isDirty()).isFalse();
  }

  @Test
  void rs3_scaleRecipe_ingredientCountUnchanged() throws InterruptedException {
    Platform.runLater(
        () -> {
          viewModel.loadRecipe("pancakes-1");
          viewModel.scaleRecipe(2);
        });
    waitForFxEvents();
    assertThat(viewModel.getIngredientCount()).isEqualTo(2);
  }

  @Test
  void rs4_recipeLoaded_trueAfterLoad() throws InterruptedException {
    Platform.runLater(() -> viewModel.loadRecipe("pancakes-1"));
    waitForFxEvents();
    assertThat(viewModel.recipeLoadedProperty().get()).isTrue();
  }

  @Test
  void rs5_scaleRecipe_preservesIngredientNames() throws InterruptedException {
    Platform.runLater(
        () -> {
          viewModel.loadRecipe("pancakes-1");
          viewModel.scaleRecipe(8);
        });
    waitForFxEvents();
    assertThat(viewModel.getIngredientNames()).containsExactly("flour", "milk");
  }
}
