# Button groups and split buttons

Connect related controls with the active skin’s outer corners, shared borders and focus treatment. Groups keep
ordinary button/link semantics and Tab order. They do not create a toolbar, selection state or arrow-key navigation.

## Group existing controls

Attach `ButtonGroupBehavior`, also available as `Sw.Behaviors.buttonGroup()`, to the controls’ parent. Its direct
children should be styled buttons/links. Give the group an accessible name with `setLabel`, `aria-label` or
`aria-labelledby`. Each control still needs its own name, including icon-only controls.

```java
var actions = new WebMarkupContainer("actions");
actions.add(Sw.Behaviors.buttonGroup().setLabel("Quote actions"));
actions.add(SwSubmitButton.onSubmit("save", "Save", target -> saveQuote())
        .setVariant(ButtonBehavior.Variant.OUTLINE));
actions.add(new BookmarkablePageLink<>("cancel", QuotesPage.class)
        .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
form.add(actions);
```

```html
<div wicket:id="actions">
  <button wicket:id="save"></button>
  <a wicket:id="cancel">Cancel</a>
</div>
```

Use `setOrientation(ButtonGroupBehavior.Orientation.VERTICAL)` to stack controls. Joins follow the page’s text
direction; a horizontal group also works in RTL. Controls keep their individual variants, sizes and enabled state.
Keep sizes consistent within a group. Groups stay together on one line; the application owns scrolling or layout
for groups too wide for its container. The parent does not clip focus rings.

## Repeat model-backed actions

`SwButtonGroup<T>` follows the Wicket Oat `populateItem` and `of` pattern. Supply the `items` wrapper and its control
in your markup. The component adds `sw-button-group-item` to each wrapper and marks `li` wrappers as presentational
because the parent has group semantics. Use one styled control per item and set
visibility on the item, so hidden entries do not leave an empty segment.

```java
add(SwButtonGroup.of("group", Model.ofList(List.of("Previous", "Next")), (item, action) -> {
    item.add(SwButton.onClick("button", action, target -> navigate(action))
            .setVariant(ButtonBehavior.Variant.OUTLINE));
}).setLabel("Browse records"));
```

```html
<menu wicket:id="group">
  <li wicket:id="items"><button wicket:id="button"></button></li>
</menu>
```

The callback is also available through `Sw.Components.buttonGroup`. Normal Wicket list-model detachment and
component visibility/enabled rules apply. Set an output markup ID when replacing the group over Ajax.

## Split a primary action from alternatives

`SwSplitButton` owns two joined buttons and a native dropdown menu. Its constructor takes the primary label and
a serializable Ajax callback. `addAction` returns a `MenuAction` with `setEnabled`, `setVisible` and menu-specific
`setVariant` settings. Labels accept strings or detachable models and are escaped.

```java
var save = new SwSplitButton("save", "Save", target -> saveDraft())
        .setVariant(ButtonBehavior.Variant.OUTLINE)
        .setSize(ButtonBehavior.Size.SMALL);
save.addAction("Save and send", target -> send());
save.addAction("Discard", target -> discard())
        .setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
add(save);
```

```html
<div wicket:id="save"></div>
```

Button variant and size apply to both halves. The chevron trigger is named by `SwSplitButton.more` (“More actions”),
with built-in translations in all nine languages. Application resource bundles can override it. For several split
buttons with the same primary label, supply a more specific trigger name through `getTrigger()` and an
`AttributeModifier`. Use `getPrimaryButton()` for an independent primary enabled state or opt-in busy feedback.

The trigger is disabled if no visible, enabled alternatives exist. Disabling the panel disables both buttons and
all menu actions. Disabled or hidden actions are checked on the server before invoking callbacks. Their label
models detach even when they have not rendered. Configure action availability from application authorization;
the component does not implement authorization itself.

The menu defaults to bottom/end placement; `setSide` and `setAlign` use the existing dropdown enums. Enter, Space,
Up/Down, Home/End and typeahead use the [dropdown menu keyboard behavior](dropdown-menu.md). Escape closes the menu
and restores focus to the trigger. Selecting an action closes it; `close(target)` is available to other callbacks.
Replacing the panel over Ajax starts with a closed menu and updated labels/action availability.

## Forms and native behavior

Both split-button callbacks are Ajax commands on `type="button"` controls: they do not submit, validate or update
the models of a surrounding form. To submit a validated form, compose `SwSubmitButton` or an ordinary Wicket
submit button inside a group, as above. A reset button remains a native `type="reset"` control.

Groups require no JavaScript. Native links, submit buttons and reset buttons retain their normal behavior without
script. The split menu’s native popover can open in supporting browsers without script; executing its Ajax commands
requires JavaScript. No additional runtime, dependency, inline styles or CSP exceptions are introduced.
