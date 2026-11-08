# Fieldsets and custom fields

`SwFieldset` groups related fields in a native `fieldset`. It owns the legend and optional description;
you own the body markup. Its API follows Wicket Oat's fieldset, with Shadleaf fieldset/legend styling.

```java
var pricing = new SwFieldset("pricing", "Quote details")
        .setDescription("Amounts before tax.");
pricing.add(new SwMoneyField("budget").setCurrency(Currency.getInstance("EUR")));
form.add(pricing);
```

```html
<fieldset wicket:id="pricing">
  <div wicket:id="budget"></div>
</fieldset>
```

Fields still inherit a parent `CompoundPropertyModel` by their own IDs. Calling `pricing.setEnabled(false)`
disables both Wicket form processing and the native fieldset. Keep enable/disable actions outside its body.
The legend is escaped text. `getLegend()` returns its model; descriptions accept strings or models.
Bind this Border to a `fieldset`, rather than a `div`.

## Compound inputs

`SwCustomField<T>` wraps an application-owned `FormComponentPanel<T>` in a fieldset, legend, description
and inline error. Its serializable factory receives the required input ID and a model bound to the wrapper.

```java
form.add(new SwCustomField<String>("reference", "Project reference",
        Model.of("STUDIO-1"), ReferenceInput::new)
        .setDescription("A project code and optional revision."));
```

```html
<div wicket:id="reference"></div>
```

`ReferenceInput` is an ordinary Wicket `FormComponentPanel<String>` with its own panel markup. Add and label
its child controls, validate them, and implement `convertInput()` to call `setConvertedInput(...)` with the
combined value. The `ReferenceInput` class in the examples module shows the implementation. Run the local examples
at `/form-completion` to try both native and Ajax forms.

Use a panel that renders on a `div`; the factory must return a non-null component with the provided ID.
Give every nested control its own label: a legend names the group, not each input. Initialize controls in
`onInitialize()` when reading an inherited model, because the component is attached to its parent by then.

The wrapper shows the first error on its root or nested form components. Each child receives description/error
IDs without losing its own `aria-describedby`; invalid children receive `aria-invalid`. New children are wired
on the next render, including an Ajax replacement. An Ajax submit that changes nested validation state refreshes
the compound wrapper; applications should expect focus to move when their compound markup is replaced.
Ordinary single-input fields retain their existing in-place Ajax feedback behavior.

Use `getInput()` for validators over the combined value and `getInput().get("childId")` for a particular control.
Factories, models, child components and callbacks must support Wicket page serialization. For a reusable
application input, keep its Java and matching `.html` resource together.
