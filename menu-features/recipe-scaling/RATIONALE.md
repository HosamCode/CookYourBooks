# Recipe Scaling — Design Rationale

## Why We Chose This Feature

Recipe Scaling directly addresses a core pain point for our persona, Bella Johnson — a college
student who cooks to save money and plan meals for the week. Bella often finds recipes online that
serve 4–6 people, but she is cooking for 1 or 2. Without scaling, she would have  to manually do the math
for every ingredient, which is tedious and error prone.

This feature lets Bella enter her desired serving size and instantly see recalculated ingredient
quantities meaning that no mental math is required. It fits naturally into the Recipe Editor, which Bella already
uses to clean up and organize imported recipes.

## What User Need It Addresses

Bella's goals include:
- Quickly editing ingredient amounts to match her actual needs
- Keeping recipes organized and easy to use later

Scaling removes a friction point that would otherwise push her to do manual calculations outside
the app. It makes CookYourBooks more useful as a complete recipe management tool, not just a storage
app.

## Alternatives Considered

**Alternative 1: Scale at cook time (not in the editor)**
We considered adding a scaling input only when viewing a recipe in "cook mode." This would keep the
editor clean, but it means the scaled quantities are not saved and Bella would have to rescale every
time. Since Bella plans meals ahead of time, saving the scaled version is more useful.

**Alternative 2: A slider instead of a text field**
A slider for servings would be visually appealing, but it is imprecise — dragging to exactly 3
servings is harder than typing "3". A text input is faster and more reliable for Bella's use case.

**Alternative 3: Separate "Scaled Recipe" page**
We considered showing the scaled recipe on a new screen, separate from the editor. This adds
unnecessary navigation steps. Keeping scaling in the editor means Bella can scale and then
immediately save or edit the result — all in one place.

## Conclusion

Scaling in the Recipe Editor, triggered by a simple text field and button, is the most direct
solution to Bella's need. It is fast, integrated, and saves her from doing math by hand.