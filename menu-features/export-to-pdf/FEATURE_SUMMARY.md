# Feature Summary — Export to PDF

## Screenshots

Since the Library View does not currently have recipes loaded in the GA2 build, the Export PDF button is visible but cannot be fully demonstrated end-to-end. The button appears in the Recipe Editor top bar alongside Edit, Save, and Discard.

_Screenshot: Export PDF button visible in the Recipe Editor toolbar_
![Export PDF button](design/export-button-screenshot.png)

## Integration Notes

Export to PDF integrates with the existing Recipe Editor view. The feature adds:

- A new `PdfExportService` class that takes a `Recipe` object and generates a formatted PDF using the OpenPDF library
- An "Export PDF" button in the Recipe Editor toolbar that opens a file save dialog
- The PDF includes a CookYourBooks header, recipe title, servings, ingredients (bulleted), and numbered instructions

The feature reads directly from the `RecipeEditorViewModelImpl` to get the current recipe data, so it always exports what the user is currently viewing.

## Status

- **Complete:** PdfExportService generates formatted PDFs with title, servings, ingredients, and instructions. Unit tests pass. Export button is wired into the Recipe Editor view.
- **In progress:** End-to-end testing blocked by Library View not loading recipes in the GA2 build.
- **Known limitations:** No font size selector or header/footer toggle from the V2 wireframe — the current implementation exports with default formatting. The FileChooser dialog requires a windowed environment (won't work in headless/CI mode).