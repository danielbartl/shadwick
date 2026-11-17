# Filter

A row of chips choosing one value, such as the status of the orders shown below it.

> **New in 0.2.0.** `SwFilter` isn't in 0.1.0.

`SwFilter<T>` follows Wicket Oat's `OatFilter`. Choosing a chip sets the model and runs `onChange` over Ajax, for example
to re-render the list it filters:

```java
add(SwFilter.onChange("status", status, Model.ofList(List.of(Status.values())),
                target -> target.add(orders))
        .setChoiceRenderer(new EnumChoiceRenderer<>(this))
        .setNullValid(true));
```

```html
<div wicket:id="status"></div>
```

- **The chips** are native radio buttons in the [toggle](toggles-input-groups.md) style, outlined and small: Tab
  reaches the group, the arrow keys choose, and the chosen chip is filled. They aren't re-rendered when one is chosen,
  so the focus stays where the arrow keys put it.
- **"All".** `setNullValid(true)` adds a first chip for no value (`null`), named by the `SwFilter.all` resource
  ("All"). Without it a value must be chosen, also by a request made up or out of date.
- **Values.** Strings, numbers and enums are their own ids; set `setChoiceRenderer(...)` for other types and for
  translated texts (`new EnumChoiceRenderer<>(this)` looks up `Status.OPEN` and so on). The choices are read once per
  request; a `null` among them is left out.
- **The model** can be inherited from a `CompoundPropertyModel` by the filter's id, as a field's is.
- **Its name for screen readers** is looked up by its id in your `.properties` files, or set with `setLabel(...)`.

Without JavaScript an **Apply** button (`SwFilter.apply`) shows next to the chips: it submits the choice, and the page
renders again with the list filtered. `onChange` is only for Ajax.
