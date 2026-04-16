# Recipe Scaling — Implementation Journal

## How I built it

I started by looking at what already existed in the codebase. Turns out there was already a
`TransformerService.scale()` method that the CLI `scale` command uses. It takes a recipe and a
target serving count and returns a scaled recipe with all the ingredient quantities adjusted. So
I didn't need to write the scaling math myself — I just needed to hook it up to the GUI.

The main things I had to do:
1. Add a servings text field and Scale button to the FXML
2. Add `scaleRecipe(int)` to `RecipeEditorViewModelImpl`
3. Pass `TransformerService` into the ViewModel (it didn't have it before)
4. Update `CookYourBooksGuiApp` to create a `TransformerServiceImpl` and pass it through

The trickiest part was that `RecipeEditorViewModelImpl` previously only took `RecipeRepository` as
a constructor argument. Adding `TransformerService` meant updating the constructor everywhere it
was used — the controller, the app wiring, and the tests.

I also had to make sure scaling didn't mark the recipe as "dirty" (which would force the user to
save). I used the existing `suppressDirtyTracking` flag for this.

## Commits

- `feat: add recipe scaling to Recipe Editor` — main implementation
- `feat: add recipe scaling tests` — unit tests

## A decision I had to make

I had to decide whether scaling should mark the recipe as dirty or not. If it does, the user is
forced to either save or discard every time they scale. If it doesn't, the scaled quantities are
just a preview and go away if you navigate elsewhere.

I went with not marking dirty, so scaling is a preview. The user can still save manually if they
want to keep the scaled version. I documented this as a known limitation.

## Known limitations

- Scaled quantities disappear if you navigate away without saving
- Recipes with no serving info can't be scaled (the app shows an error)
- Only whole number servings work in the UI