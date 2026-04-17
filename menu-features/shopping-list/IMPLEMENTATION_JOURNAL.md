# Implementation Journal

## Pull Request

[Shopping List Feature Implementation](https://github.com/neu-cs3100/sp26-hw-cyb12-group-4619/pull/2)

This PR implements the full Shopping List feature including the ViewModel interface and implementation, ShoppingItemSummary record, FXML layout, controller, and navigation wiring.

## Commit History

Development progressed incrementally across the following stages:

1. Added `ShoppingItemSummary` record with `name` and `quantity` fields
2. Wrote `ShoppingListViewModel` interface with SL1-SL7 observable properties and commands
3. Implemented `ShoppingListViewModelImpl` with `LibrarianService` and `RecipeService` injection
4. Built `ShoppingListView.fxml` with left recipe picker panel and right dual-list results panel
5. Wrote `ShoppingListViewController` with selection listener and Generate button bindings
6. Wired the new view into `NavigationService`, `CookYourBooksGuiApp`, and `MainView.fxml`

## Technical Decision Log

**RecipeService vs PlannerService**

We call `RecipeService.generateShoppingList(List<String> recipeIds)` instead of `PlannerService` because `RecipeService` takes IDs directly with no need to look up full Recipe objects first, and it is the method documented for GUI use. `PlannerService` takes `List<Recipe>` which would require two service calls: one to fetch the Recipe objects and one to generate the list.

**Selection snapshot before background thread**

When the user hits Generate, we snapshot the selected IDs into a local list before handing off to the background thread. This means mid-flight selection changes by the user cannot corrupt a generation that is already running. Without the snapshot, the background thread would be reading from a list that the FX thread could modify at any time.

**setSelectedRecipes takes a full List instead of one ID at a time**

The ListView fires one change event covering the entire current selection, not one event per click. So rather than trying to track additions and removals individually, the controller collects all currently selected IDs into a single snapshot and passes that to the ViewModel in one call. This keeps the ViewModel stateless with respect to how the selection changed and makes it trivial to test by just calling setSelectedRecipes(List.of("id1", "id2")) directly.