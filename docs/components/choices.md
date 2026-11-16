# Checkboxes switches and radios

These use the same field wrapper, model binding, `.properties` labels and inline feedback as text fields:

```java
SwCheckBox accepted = new SwCheckBox("accepted", "Accept the terms", Model.of(false))
        .setDescription("Please read the terms before continuing.");
form.add(accepted);
form.add(new SwSwitch("notifications", "Email notifications", Model.of(true))
        .setSize(SwitchBehavior.Size.SMALL));
form.add(new SwRadioChoice<>("contact", "Contact preference", Model.of("Email"),
        Model.ofList(List.of("Email", "SMS", "Phone")))
        .setRequired(true).setInline(true));
```

Bind each to a `<div wicket:id="..."></div>`, as with text fields. `SwRadioChoice<T>` wraps Wicket's
`RadioChoice<T>` with a fieldset, legend, individually labelled native radios and an optional `IChoiceRenderer`.
Choices stack by default; `.setInline(true)` puts them in a wrapping row. Help text and validation errors describe
both the group and each radio. The checkbox and switch remain native Boolean controls, including keyboard and
disabled behavior; `Sw.Behaviors.swSwitch()` styles an existing Wicket `CheckBox` as a switch.

As with Wicket's `CheckBox`, unchecked means `false`, and `setRequired(true)` does **not** require it to be checked.
For consent or another rule that requires `true`, add a validator:

```java
accepted.addValidator(value -> {
    if (!Boolean.TRUE.equals(value.getValue())) {
        value.error(new ValidationError().setMessage("Accept the terms to continue."));
    }
});
```

Use `AjaxFormComponentUpdatingBehavior("change")` on a checkbox or switch, and
`AjaxFormChoiceComponentUpdatingBehavior` on a radio group. Passing them to the field's `.add(...)` routes them to
the underlying input. Plain and Ajax form submits work too; field errors update in place without resetting focus
or the current selection. The showcase's **Choices** page demonstrates all of these paths.

## Mixed state

A checkbox can show the mixed state, a minus, when it stands for a group of which only some are selected:

```java
SwCheckBox all = new SwCheckBox("allProjects", "All projects", allSelectedModel)
        .setIndeterminate(() -> someSelected() && !allSelected());
all.setOutputMarkupId(true);
```

HTML has no attribute for it, only the DOM property `indeterminate`. The input renders `data-sw-indeterminate`, and
Shadwick's script sets the property when the page loads and after every Ajax update, so screen readers report the box
as mixed. The model is read on every render. A click clears the mixed state, as on any checkbox, and the box submits
only checked or unchecked: recompute the mixed state on the server and re-render the box (for example from the
children's `AjaxFormComponentUpdatingBehavior`) when the group changes. The Choices page has a working "select all".
