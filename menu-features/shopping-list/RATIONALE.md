# Shopping List — Design Rationale

## Why We Chose This Feature

We selected Shopping List because it is the feature that most directly closes the loop between the app's core purpose and real-world use. CookYourBooks already lets users browse and select recipes, and Shopping List is the natural next step that turns that selection into something actionable. It also gave us a chance to exercise the full MVVM stack in a meaningful way: background threading, two distinct output types, multi-selection state management, and service layer coordination. Compared to other features on the buffet, it struck the right balance of scope and depth for our timeline.

## User Need

This feature directly addresses the needs of Mohamed Ali, our Library View persona, a 27-year-old home cook who cooks a few times a week for himself and his roommates. Mohamed's core frustration is that his recipes are scattered across multiple apps and tabs, making it hard to know what he has saved or what he needs to buy. He often browses his library while already at the grocery store, which means he needs to quickly turn a set of recipes into a consolidated shopping list with no friction.

Shopping List solves this exactly. He selects the recipes he wants to cook that week, hits Generate, and gets a single aggregated list he can reference in the store aisle. The two-panel 
output, with measured items on one side and uncountable items on the other, was a direct response to his context. Mohamed needs to know precisely what to buy ("2 cups flour") versus what is already assumed to be in the pantry ("salt to taste"). Mixing those together in one flat list would create confusion at exactly the moment he needs clarity most.

## Alternatives Considered

- **Meal Planning:** We considered this as our primary feature but ruled it out due to scope. 
  Meal Planning requires calendar UI, scheduling logic, and persistence of planned meals. That 
  was too much surface area to do well in the GA2 window. Shopping List delivers most of the 
  same user value with a much tighter scope.
- **Recipe Scaling:** Also on our shortlist. Scaling is useful but it is a single-recipe 
  operation that does not require multi-selection state or aggregation logic, so it would have 
  exercised less of the architecture. Shopping List felt more interesting to build and more 
  useful to Mohamed's specific grocery-store use case.
- **Using PlannerService instead of RecipeService:** At the service layer, we considered routing 
  through PlannerService but it takes a List of Recipe objects, which would have required 
  fetching full Recipe objects before generating the list, meaning two service calls instead of 
  one. RecipeService.generateShoppingList(List<String> recipeIds) takes IDs directly and is 
  documented for GUI use, so we used that instead.

We chose Shopping List because it delivers real value for Mohamed's grocery-store workflow, fits cleanly within our existing architecture, and gave every layer of the stack something meaningful to do.