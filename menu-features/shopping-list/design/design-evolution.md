# Design Evolution

## V1 to V2

Our initial wireframe had a single flat output list beneath the recipe picker. The idea was simple: select recipes, hit Generate, see everything in one place. That worked conceptually but fell apart when we thought through the actual data. Measured ingredients like "2 cups flour" and uncountable ingredients like "salt to taste" are fundamentally different types. Putting them in the same list would either require a union type with conditional rendering logic in the View, or we would lose type safety entirely by mixing ShoppingItemSummary records and plain Strings in one ObservableList.

Splitting into two panels solved both problems at once. Each ListView binds to a single well-typed list with no branching in the cell factory. It also makes the output clearer for the user. Mohamed can scan the measured items to know exactly what to buy and what quantities, then glance at the uncountable items separately as a reminder of pantry staples. The layout change from V1 to V2 was driven by the type system, not just aesthetics.