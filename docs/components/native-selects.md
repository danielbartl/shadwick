# Native selections

`SwDropdownChoice<T>` wraps Wicket's native `DropDownChoice<T>`. `SwListMultipleChoice<T>` is a native
`<select multiple>` list box, and `SwCheckBoxMultipleChoice<T>` presents a collection as labelled checkboxes
inside a fieldset. All three accept a choices model and an optional `IChoiceRenderer`, use the standard field
wrapper, and inherit selected models and `.properties` labels by id:

```java
var teams = Model.ofList(List.of("Design", "Engineering", "Support"));
form.add(new SwDropdownChoice<>("team", teams).setRequired(true));
form.add(new SwListMultipleChoice<>("teams", teams).setMaxRows(3).setRequired(true));
form.add(new SwCheckBoxMultipleChoice<>("channels", Model.ofList(List.of("Email", "SMS", "In-app")))
        .setInline(true).setRequired(true));
```

Bind each field to a `<div wicket:id="...">`. The dropdown property holds one choice; multi-selection properties
hold a collection, such as a mutable `List<T>`. Wicket updates existing collections in place. A required
multi-selection needs at least one valid choice; unrecognised submitted IDs cannot satisfy that requirement.
Optional multi-selections can be cleared. Use `dropdown.getInput().setNullValid(true)` to keep a selectable null
option in an optional dropdown. In page properties, `input.nullValid` supplies its text and `input.null`
labels the initial prompt, following Wicket's resource lookup for the wrapped control named `input`.

Use `AjaxFormComponentUpdatingBehavior("change")` for either native select and
`AjaxFormChoiceComponentUpdatingBehavior` for the checkbox group. Pass these to `.add(...)` to attach them to the
wrapped control. Inline feedback updates without replacing the select, checkbox nodes, current selection or focus.
The **Selections** showcase (`/selections`) demonstrates normal and Ajax submits, immediate updates, a custom
renderer with stable IDs, inline and stacked checkbox groups, and disabled selections. Multi-select keyboard
interaction follows the browser and operating system; checkbox groups provide an alternative without modifier keys.
