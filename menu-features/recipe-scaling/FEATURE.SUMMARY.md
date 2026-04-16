# Recipe Scaling — Feature Summary

## What's done

The feature is fully working. You can open any recipe in the Recipe Editor, type a serving count
into the Scale Recipe section, click Scale, and the ingredient quantities update instantly. There's
also a status label that says "Scaled to X servings." so you know it worked.

I also wrote 5 unit tests that cover:
- Scaling doubles ingredient count correctly
- Scaling doesn't mark the recipe dirty
- Ingredient count stays the same after scaling
- `recipeLoaded` property is set correctly
- Ingredient names are preserved after scaling

## What's not done / known issues

- Scaled quantities don't save automatically — you have to click Edit then Save if you want to keep them
- If a recipe has no serving info, scaling fails with an error message
- You can only type whole numbers for servings

## How it connects to the rest of the app

Recipe Scaling lives inside the Recipe Editor. It uses `TransformerService` (same service the CLI
`scale` command uses) to do the math. The only new dependencies were:
- `TransformerService` added to `RecipeEditorViewModelImpl`
- `TransformerServiceImpl` created in `CookYourBooksGuiApp` and passed to the editor controller

Everything else (the ingredient list, the editor layout) was already there from GA1.

## Screenshots
See `design/Screenshot.jpeg` for a screenshot of the feature working with a test recipe scaled to 6 servings.