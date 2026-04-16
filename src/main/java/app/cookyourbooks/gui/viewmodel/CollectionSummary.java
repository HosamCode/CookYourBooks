package app.cookyourbooks.gui.viewmodel;

import app.cookyourbooks.model.SourceType;

/**
 * A lightweight snapshot of a {@link app.cookyourbooks.model.RecipeCollection} for display in the
 * Library View.
 *
 * <p>The View only needs these four fields — passing the full {@code RecipeCollection} domain
 * object would expose unnecessary API surface and couple the View to the model.
 */
public record CollectionSummary(String id, String title, SourceType sourceType, int recipeCount) {}
