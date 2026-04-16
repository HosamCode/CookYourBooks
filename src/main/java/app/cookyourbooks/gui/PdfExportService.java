package app.cookyourbooks.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;

/** Service that exports a Recipe to a formatted PDF file. */
public class PdfExportService {

  /**
   * Exports the given recipe to a PDF file at the specified path.
   *
   * @param recipe the recipe to export
   * @param file the output file
   * @throws IOException if writing fails
   * @throws DocumentException if PDF generation fails
   */
  public void exportToPdf(Recipe recipe, File file) throws IOException, DocumentException {
    Document document = new Document();
    PdfWriter.getInstance(document, new FileOutputStream(file));
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
    Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC);

    // Header
    Paragraph header = new Paragraph("CookYourBooks", headerFont);
    header.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
    header.setSpacingAfter(12);
    document.add(header);

    // Title
    Paragraph title = new Paragraph(recipe.getTitle(), titleFont);
    title.setSpacingAfter(8);
    document.add(title);

    // Servings
    Servings servings = recipe.getServings();
    if (servings != null) {
      document.add(new Paragraph("Servings: " + servings, bodyFont));
    }

    document.add(new Paragraph(" "));

    // Ingredients
    Paragraph ingredientsHeading = new Paragraph("Ingredients", headingFont);
    ingredientsHeading.setSpacingAfter(6);
    document.add(ingredientsHeading);

    for (Ingredient ingredient : recipe.getIngredients()) {
      document.add(new Paragraph("\u2022 " + ingredient.toString(), bodyFont));
    }

    document.add(new Paragraph(" "));

    // Instructions
    Paragraph instructionsHeading = new Paragraph("Instructions", headingFont);
    instructionsHeading.setSpacingAfter(6);
    document.add(instructionsHeading);

    for (Instruction instruction : recipe.getInstructions()) {
      Paragraph step = new Paragraph(instruction.toString(), bodyFont);
      step.setSpacingAfter(4);
      document.add(step);
    }

    document.close();
  }
}
