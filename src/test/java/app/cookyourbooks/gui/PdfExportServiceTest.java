package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;

@SuppressWarnings("NullAway.Init")
class PdfExportServiceTest {

  @TempDir File tempDir;

  @Test
  void exportToPdf_createsFile() throws Exception {
    Recipe recipe =
        new Recipe(
            "Test Recipe",
            new Servings(4),
            List.of(new MeasuredIngredient("flour", new ExactQuantity(2, Unit.CUP), null, null)),
            List.of(new Instruction(1, "Mix ingredients", List.of())),
            List.of());

    File output = new File(tempDir, "test.pdf");
    PdfExportService service = new PdfExportService();
    service.exportToPdf(recipe, output);

    assertThat(output).exists();
    assertThat(output.length()).isGreaterThan(0);
  }

  @Test
  void exportToPdf_handlesEmptyIngredients() throws Exception {
    Recipe recipe = new Recipe("Empty Recipe", null, List.of(), List.of(), List.of());

    File output = new File(tempDir, "empty.pdf");
    PdfExportService service = new PdfExportService();
    service.exportToPdf(recipe, output);

    assertThat(output).exists();
    assertThat(output.length()).isGreaterThan(0);
  }

  @Test
  void exportToPdf_includesMultipleIngredients() throws Exception {
    Recipe recipe =
        new Recipe(
            "Pancakes",
            new Servings(2),
            List.of(
                new MeasuredIngredient("flour", new ExactQuantity(1, Unit.CUP), null, null),
                new MeasuredIngredient("milk", new ExactQuantity(0.5, Unit.CUP), null, null),
                new MeasuredIngredient("sugar", new ExactQuantity(2, Unit.TABLESPOON), null, null)),
            List.of(
                new Instruction(1, "Mix dry ingredients", List.of()),
                new Instruction(2, "Add wet ingredients", List.of()),
                new Instruction(3, "Cook on griddle", List.of())),
            List.of());

    File output = new File(tempDir, "pancakes.pdf");
    PdfExportService service = new PdfExportService();
    service.exportToPdf(recipe, output);

    assertThat(output).exists();
    assertThat(output.length()).isGreaterThan(0);
  }

  @Test
  void exportToPdf_handlesNullServings() throws Exception {
    Recipe recipe =
        new Recipe(
            "No Servings Recipe",
            null,
            List.of(
                new MeasuredIngredient(
                    "butter", new ExactQuantity(1, Unit.TABLESPOON), null, null)),
            List.of(new Instruction(1, "Melt butter", List.of())),
            List.of());

    File output = new File(tempDir, "noservings.pdf");
    PdfExportService service = new PdfExportService();
    service.exportToPdf(recipe, output);

    assertThat(output).exists();
    assertThat(output.length()).isGreaterThan(0);
  }

  @Test
  void exportToPdf_handlesLongRecipeTitle() throws Exception {
    Recipe recipe =
        new Recipe(
            "Grandma's Famous Triple Chocolate Chip Cookies With Extra Sprinkles",
            new Servings(24, "cookies"),
            List.of(
                new MeasuredIngredient(
                    "chocolate chips", new ExactQuantity(3, Unit.CUP), null, null)),
            List.of(new Instruction(1, "Mix everything together", List.of())),
            List.of());

    File output = new File(tempDir, "longname.pdf");
    PdfExportService service = new PdfExportService();
    service.exportToPdf(recipe, output);

    assertThat(output).exists();
    assertThat(output.length()).isGreaterThan(0);
  }
}
