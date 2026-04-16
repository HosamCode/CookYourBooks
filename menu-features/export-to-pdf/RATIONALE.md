# Export to PDF — Design Rationale

## Why We Chose This Feature

We selected Export to PDF because it was identified during our GA0 design sprint as a low-risk, high-value feature that benefits all of our user personas. It requires no new backend logic, and the recipe data already exists in the service layer so the work is focused on presentation and formatting. Additionally, personally I think that it would be cool to learn how to actually do this! It's a very common feature, so understanding the back-end specifics of it seems super interesting.

## User Need

This feature primarily addresses the needs of Maxwell (Import Interface persona), a 62-year-old retired accountant with low technical comfort and poor eyesight. Maxwell struggles to navigate his phone and would benefit from being able to print a recipe on paper with large, clean formatting. Instead of squinting at a screen while cooking, he can have a physical copy on the counter.

It also helps Samuel (Search & Filter persona), who cooks while multitasking in the kitchen. A printed recipe means he doesn't need to keep touching his phone with wet hands or worry about the screen locking mid-step.

## Alternatives Considered

- **Export to Markdown:** Simpler to implement but not useful for non-technical users like Maxwell who wouldn't know what to do with a .md file.
- **Print View (HTML-based):** We considered a browser-style print view, but since CookYourBooks is a JavaFX desktop app, there's no browser to leverage. A direct PDF export is more appropriate for the platform.
- **Export to Image (PNG):** Would preserve formatting but doesn't support multi-page recipes well and isn't as universally shareable as PDF.

We chose PDF because it's the most universally accessible format, meaning that every device can open it, it prints cleanly, and it preserves layout regardless of the user's system. These other alternatives also may need some more experienced users to fully understand how to use it, whereas low educated people and older, less tech savvy people will have no trouble really.