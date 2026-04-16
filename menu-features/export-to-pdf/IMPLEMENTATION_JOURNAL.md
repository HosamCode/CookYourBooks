# Implementation Journal — Export to PDF

## Technical Decision: PDF Library Choice

**Decision:** Use OpenPDF (a fork of iText) for PDF generation.

**Alternatives considered:**
- **Apache PDFBox:** Popular but lower-level — requires manually positioning every line of text on the page, which makes layout tedious.
- **iText 7:** Powerful but has a commercial license (AGPL), which could cause issues.
- **JavaFX Print API:** Built-in but only supports printing to a physical printer, not saving directly to a PDF file.

**Why OpenPDF:** It's free (LGPL license), has a simple API for adding paragraphs/tables/headers, and handles page breaks automatically for long recipes. It's the best balance of simplicity and capability for our use case.

## Git History

_Will be updated as commits are made._

## PR History

_Will be updated when PR is opened._