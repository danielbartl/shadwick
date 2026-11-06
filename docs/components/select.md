# Custom select

`SwSelect<T>` is a single-choice field with the same constructors and model/renderer contracts as
`SwDropdownChoice<T>`, plus groups, icons, separators and disabled choices:

```java
var workspace = new SwSelect<>("workspace", "Workspace", selectedWorkspace, workspaceChoices,
        new ChoiceRenderer<Workspace>("name", "id"))
        .setPlaceholder("Choose a workspace")
        .setGroup(Workspace::group)
        .setIcon(Workspace::icon) // registered SwIcon name, or null
        .setDisabledWhen(Workspace::disabled)
        .setSeparatorBefore(w -> w.id().equals("support"))
        .setSize(SelectBehavior.Size.SMALL)
        .setRequired(true);
form.add(workspace);
```

Bind it to `<div wicket:id="workspace"></div>`. Choices retain their original order and renderer IDs;
adjacent equal group labels produce an optgroup. Dynamic labels, group names and placeholders are escaped.
Unknown, removed and disabled IDs fail server conversion, including for optional fields. Use stable renderer IDs
for changing collections. The choices model is read once per request and detached normally.

## Native and enhanced behavior

The real `<select>` always owns the value. Browsers supporting `appearance: base-select` draw the styled native
picker; other Popover API browsers receive an accessible combobox/listbox enhancement. Without JavaScript or
these APIs, the platform picker still submits and validates. No frontend dependency or CSP exception is needed.
Arrow keys, Home/End, typing, Enter/Space and Escape use the picker; the fallback also supports PageUp/PageDown.
Escape/outside dismiss without changing the value; Tab commits the highlighted fallback choice and moves focus.
Disabled choices are skipped. All skins, sizes, RTL, reduced motion and forced colors are supported.

## Placeholder and validation

`setPlaceholder(String/IModel<String>)` supplies the empty prompt. It is hidden from the list by default;
`getInput().setNullValid(true)` makes it a selectable clearing choice. Required validation still rejects empty values.
`SwSelect.placeholder` and `SwSelect.unavailable` are localizable resources. This is single selection;
searchable and multiple custom selection are provided by the combobox fields below. Native multi-selection remains
available through `SwListMultipleChoice` and `SwCheckBoxMultipleChoice`.

## Ajax and caller-owned markup

Attach `AjaxFormComponentUpdatingBehavior("change")` with `.add(...)` for immediate updates. Normal submit,
Ajax submit and inline feedback use `BaseSwField`; replacing the field with `target.add(select)` initializes the
new picker automatically. The fallback mirrors the source's name, description, error, required and disabled state,
including disabled fieldsets. When changing the native value from application JavaScript, dispatch `input`/`change`
to synchronize dependent UI. Native form reset restores the rendered default without firing change or updating
the server model. Refresh dependent server UI explicitly when an application needs a server-side reset.

For caller-owned markup, add `Sw.Behaviors.select()` to a wrapper containing a single `<select class="sw-select">`
with an associated label. Use `sw-select-item`, `sw-select-group`, `sw-select-separator` and a decorative
`sw-select-icon` for the same anatomy. `SwIcon.decorativeMarkup(name)` supplies trusted registry SVG for native
option bodies. The **Custom select** showcase (`/custom-select`) covers normal/Ajax validation, immediate updates,
reset, changing choices, groups/icons, a disabled field and a longer list. The landing studio previews plan selection
using the actual component CSS/runtime.
