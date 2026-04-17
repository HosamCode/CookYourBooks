# Implementation Journal — Export to PDF

## Technical Decision: PDF Library Choice

**Decision:** Use OpenPDF (a fork of iText) for PDF generation.

**Alternatives considered:**
- **Apache PDFBox:** Popular but lower-level — requires manually positioning every line of text on the page, which makes layout tedious.
- **iText 7:** Powerful but has a commercial license (AGPL), which could cause issues.
- **JavaFX Print API:** Built-in but only supports printing to a physical printer, not saving directly to a PDF file.

**Why OpenPDF:** It's free (LGPL license), has a simple API for adding paragraphs/tables/headers, and handles page breaks automatically for long recipes. It's the best balance of simplicity and capability for our use case.

## Git History

- `f446f0b` — Added RATIONALE.md for Export to PDF feature
- `5c5bbca` — Added design wireframes and design-evolution.md
- `71cf0f1` — Added PdfExportService, wired Export PDF button into RecipeEditorView, added unit tests
- `9ecbafc` — Added feature summary and implementation journal
- `b1974a9` — Updated implementation journal with PR link

## PR History

- [PR #1: Add Export to PDF feature](https://github.com/neu-cs3100/sp26-hw-cyb12-group-4619/pull/1) — Adds feature summary, implementation journal, PdfExportService, unit tests, and Export PDF button in Recipe Editor toolbar.