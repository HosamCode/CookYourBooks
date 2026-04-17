package app.cookyourbooks.gui.viewmodel;

/**
 * A lightweight snapshot of a {@link app.cookyourbooks.model.ShoppingItem} for display in the
 * shopping list view.
 *
 * <p>The View only needs the ingredient name and a pre-formatted quantity string for display.
 */
public record ShoppingItemSummary(String name, String quantity) {}
