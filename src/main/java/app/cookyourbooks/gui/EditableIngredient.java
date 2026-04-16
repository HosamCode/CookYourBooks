package app.cookyourbooks.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Quantity;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;

/**
 * A mutable, UI-friendly wrapper around an Ingredient.
 *
 * <p>Domain Ingredient objects are immutable and reject blank names, making them unusable for form
 * binding. This class holds in-progress edit state and converts back to a domain Ingredient when
 * done.
 *
 * <p>Used by: Recipe Editor (Rishika) and Import Interface (Angela).
 */
public class EditableIngredient {

  private final StringProperty name = new SimpleStringProperty("");
  private final StringProperty quantity = new SimpleStringProperty("");
  private final StringProperty unit = new SimpleStringProperty("");

  /**
   * The original domain ingredient this was created from. Null for brand-new ingredients added in
   * the UI.
   */
  private final @Nullable Ingredient original;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /** Create a blank ingredient (for "Add ingredient" button). */
  public EditableIngredient(String name) {
    this.name.set(name);
    this.original = null;
  }

  /** Internal constructor — preserves the original domain object. */
  private EditableIngredient(String name, String quantity, String unit, Ingredient original) {
    this.name.set(name);
    this.quantity.set(quantity != null ? quantity : "");
    this.unit.set(unit != null ? unit : "");
    this.original = original;
  }

  // -------------------------------------------------------------------------
  // Static factory — convert FROM a domain Ingredient
  // -------------------------------------------------------------------------

  public static EditableIngredient from(Ingredient ingredient) {
    if (ingredient instanceof MeasuredIngredient measured) {
      Quantity qty = measured.getQuantity();
      // Store just the number, not the full "10 cups" string
      String qtyStr = String.valueOf(qty.toDecimal()).replaceAll("\\.0+$", "");
      String unitStr = qty.getUnit().name(); // e.g. "CUP"
      return new EditableIngredient(measured.getName(), qtyStr, unitStr, measured);
    }
    return new EditableIngredient(ingredient.getName(), "", "", ingredient);
  }

  // -------------------------------------------------------------------------
  // Convert BACK to a domain Ingredient
  // -------------------------------------------------------------------------

  /**
   * Convert to a domain Ingredient for persistence. Returns null if name is blank — caller should
   * filter nulls before saving.
   */
  public @Nullable Ingredient toDomain() {
    String n = name.get() == null ? "" : name.get().trim();
    if (n.isBlank()) {
      return null;
    }

    String q = quantity.get() == null ? "" : quantity.get().trim();
    String u = unit.get() == null ? "" : unit.get().trim();

    if (!q.isBlank() && !u.isBlank()) {
      try {
        double amount = Double.parseDouble(q);
        // Try to match unit string to Unit enum (case-insensitive)
        Unit unitEnum = null;
        for (Unit unitVal : Unit.values()) {
          if (unitVal.name().equalsIgnoreCase(u) || unitVal.toString().equalsIgnoreCase(u)) {
            unitEnum = unitVal;
            break;
          }
        }
        if (unitEnum != null && amount > 0) {
          return new MeasuredIngredient(
              n,
              new ExactQuantity(amount, unitEnum),
              original instanceof MeasuredIngredient m ? m.getPreparation() : null,
              original instanceof MeasuredIngredient m2 ? m2.getNotes() : null);
        }
      } catch (NumberFormatException e) {
        // fall through to VagueIngredient
      }
    }

    // If original was MeasuredIngredient and qty/unit unchanged, preserve it
    if (original instanceof MeasuredIngredient measured) {
      return new MeasuredIngredient(
          n, measured.getQuantity(), measured.getPreparation(), measured.getNotes());
    }

    return new VagueIngredient(n, null, null, null);
  }

  // -------------------------------------------------------------------------
  // JavaFX property accessors (for View binding)
  // -------------------------------------------------------------------------

  public StringProperty nameProperty() {
    return name;
  }

  public StringProperty quantityProperty() {
    return quantity;
  }

  public StringProperty unitProperty() {
    return unit;
  }

  // -------------------------------------------------------------------------
  // Plain getters (for tests)
  // -------------------------------------------------------------------------

  public String getName() {
    return name.get();
  }

  public String getQuantity() {
    return quantity.get();
  }

  public String getUnit() {
    return unit.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public void setQuantity(String quantity) {
    this.quantity.set(quantity);
  }

  public void setUnit(String unit) {
    this.unit.set(unit);
  }

  @Override
  public String toString() {
    return "EditableIngredient{name='" + getName() + "'}";
  }
}
