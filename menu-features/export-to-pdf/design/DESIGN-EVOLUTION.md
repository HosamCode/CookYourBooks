# Design Evolution — Export to PDF

## Version 1

The initial design was a simple "Export PDF" button placed directly on the recipe detail view. Clicking it would immediately generate and download a PDF with no user input or customization. The layout was fixed, having a recipe title, servings, ingredients, and instructions in a basic format.

## Version 2

After considering Maxwell's persona (low vision, low tech comfort), I realized a one-click export with no preview would leave users unsure of what they're getting. Version 2 introduces a modal dialog before exporting with the following additions:

- **Preview pane:** Shows a live preview of what the PDF will look like before the user commits to exporting, reducing uncertainty.
- **Font size selector (Small / Medium / Large):** Lets users increase the text size for readability. This directly addresses Maxwell's poor eyesight, so now he can bump the font up to Large so the printed recipe is easy to read from the counter.
- **Header toggle:** When enabled, adds "CookYourBooks" and the cookbook name at the top of the PDF. Useful for users who want to remember where the recipe came from.
- **Footer toggle:** When enabled, adds the date and page number at the bottom of the PDF. Helpful for users who print multiple recipes and want to keep them organized.
- **Export button:** Replaces the instant download with a deliberate confirmation step so users export only after reviewing.

## Why We Changed It

The V1 approach assumed all users would be fine with a default layout, but Maxwell's persona showed us that accessibility and user control matter. Adding a preview reduces the chance of wasted prints, and the font size option makes the feature genuinely useful for users with vision difficulties.