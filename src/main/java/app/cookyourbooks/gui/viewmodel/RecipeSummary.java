package app.cookyourbooks.gui.viewmodel;

/**
 * A lightweight snapshot of a {@link app.cookyourbooks.model.Recipe} for display in the recipe list
 * within the Library View.
 *
 * <p>The View only needs the ID (for navigation) and title (for display).
 */
public record RecipeSummary(String id, String title) {}
