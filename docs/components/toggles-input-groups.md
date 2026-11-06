# Toggles and input groups

`SwToggle` is a Boolean checkbox drawn as a toggle button. `SwToggleChoice<T>` is a single-selection radio group;
`SwToggleMultipleChoice<T>` binds a collection of selected values. Their labels, descriptions and errors use the same
field API as the other controls:

```java
form.add(new SwToggle("highlight").setIcon("pencil").setIconOnly(true)
        .setVariant(ToggleBehavior.Variant.OUTLINE));
form.add(new SwToggleChoice<>("alignment", Model.ofList(List.of("Left", "Center", "Right")))
        .setRequired(true).setVariant(ToggleBehavior.Variant.OUTLINE).setSpacing(0));
form.add(new SwToggleMultipleChoice<>("formats", Model.ofList(List.of("Bold", "Italic", "Underline")))
        .setSize(ToggleBehavior.Size.SMALL));
```

Each binds to a `<div wicket:id="...">` and inherits its value and resource label by id. Toggles expose native checked
state: Space changes a checkbox, arrow keys move through a single-selection group, and clicking an already selected
radio keeps it selected. A Boolean toggle's `setRequired(true)` follows Wicket's checkbox semantics and allows `false`;
use a validator when `true` is required. Required groups need at least one valid choice. Group constructors support
explicit models and `IChoiceRenderer`, like `SwRadioChoice` and `SwCheckBoxMultipleChoice`; renderer labels stay escaped
text. An icon-only toggle keeps its text caption as the accessible name.

Variants are `DEFAULT` and `OUTLINE`; sizes are `DEFAULT`, `SMALL` and `LARGE`. Group `.setSpacing(0)` joins items;
1–4 select a gap (default 2). `.setOrientation(ToggleGroupBehavior.Orientation.VERTICAL)` stacks them. Use
`AjaxFormComponentUpdatingBehavior("change")` for `SwToggle`, and `AjaxFormChoiceComponentUpdatingBehavior` for either
group. Passing these to `.add(...)` attaches them to the underlying inputs. The browser owns selection and focus;
feedback updates in place.

Text-like fields now offer `.setPrefix(...)` and `.setSuffix(...)` for text or `IModel<String>` addons:

```java
form.add(new SwTextField<String>("workspace").setPrefix("https://").setSuffix(".example"));
form.add(new SwNumberField<Integer>("weight").setMin(1).setMax(100).setSuffix("kg"));
form.add(new SwTextArea<String>("notes").setPrefix("Draft").setSuffix("Markdown supported"));
form.add(new SwTextField<String>("project").setSuggestions(List.of("Shadwick", "Atlas", "Nimbus")));
```

A null addon model or model value hides it. The group wrapper appears only when an addon is present. Prefixes and
suffixes describe the input without changing its name or submitted value, remain escaped, and keep the same
`field:input` path. Textarea addons sit above/below the control. Datalist suggestions are native browser hints on
single-line inputs, with browser-dependent availability; they do not restrict submitted values. Add validators to
limit allowed values. Prefix/suffix and suggestion models are detached with the field.

For custom markup with icons or action buttons, compose the `Sw.Behaviors` helpers:

```java
group.add(Sw.Behaviors.inputGroup());
input.add(Sw.Behaviors.inputGroupInput());       // or inputGroupTextArea()
addon.add(Sw.Behaviors.inputGroupAddon()
        .setAlign(InputGroupAddonBehavior.Align.INLINE_END));
button.add(Sw.Behaviors.inputGroupButton());    // small ghost button by default
```

```html
<label for="search-input">Search library</label>
<div wicket:id="group" aria-label="Search tools">
    <input wicket:id="input" id="search-input" />
    <div wicket:id="addon"><button wicket:id="button" type="button">Clear</button></div>
</div>
```

Keep the control before addons in the DOM so it comes first in the tab order; CSS places `INLINE_START`, `INLINE_END`,
`BLOCK_START` and `BLOCK_END` addons. `.setBorder(true)` separates a block addon from its control.
`Sw.Behaviors.inputGroupText()` styles addon text. Group buttons use `ButtonBehavior.Variant` and `Size.SMALL` or
`Size.ICON`; name icon-only buttons. Use the behavior on a plain Wicket button or link; `SwButton` already has a
button behavior. Clicking noninteractive addon space focuses the control, including after Ajax replacement; buttons
and links keep their own actions. Rich groups should have a separate group name, as in the example above.

The **Groups** showcase (`/groups`) demonstrates normal and Ajax submissions, immediate keyboard-driven formatting,
joined and vertical groups, icon-only and disabled toggles, validation, datalists, textareas, and a custom group with
Clear and Ajax replacement actions.
