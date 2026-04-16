# Design Evolution

## V1 — My first idea

My first idea was a popup dialog. You'd click a "Scale Recipe" button in the top bar, a dialog
would open, you'd type in your serving count, see a preview, and then confirm or cancel.

I sketched this out and it seemed fine at first, but then I thought about Bella using it. She'd
have to:
1. Click the Scale button
2. Wait for a dialog to open
3. Type the serving count
4. Check the preview (in a different section from the actual ingredients)
5. Click confirm
6. Go back to the editor

That's too many steps. And the preview being separate from the ingredient list means she can't
see the actual recipe while reviewing the scaled quantities.

## V2 — What I actually built

I moved scaling directly into the Recipe Editor as an inline section between the title and the
ingredients. No popup, no extra navigation. Bella just:
1. Types a number in the servings field
2. Clicks Scale
3. Sees the updated quantities right there in the ingredient list

The status label ("Scaled to 6 servings.") confirms it worked. If she doesn't like it she can
just scale again or navigate away.

This is simpler, faster, and more integrated with the rest of the editor.