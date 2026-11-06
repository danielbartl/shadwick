# Dropdown menus

`SwDropdown<T>` follows OatDropdown's action-menu API. It uses native buttons in a non-modal popover:

```java
add(SwDropdown.of("actions", "Export as", Model.ofList(List.of("CSV", "JSON", "XML")),
        format -> format,
        (target, format) -> export(format)));
```

Bind it to `<div wicket:id="actions"></div>`. The action factory invokes the Ajax callback and closes the menu;
`getTrigger()` and `getMenu()` expose its parts. An empty or null list disables the trigger. Replacing the whole
component over Ajax resets it to closed. `setSide(DropdownMenuBehavior.Side.END)` and
`setAlign(DropdownMenuBehavior.Align.END)` change placement; start/end follow the text direction.

For custom rows, subclass and implement `populateItem(ListItem<T>)`, or use the four-argument `of` factory.
Supply a `Label("label", ...)` child for each repeated button. Use `item.setEnabled(false)` for unavailable actions,
and configure its existing `DropdownMenuItemBehavior` for destructive styling or inset text. Add the action behavior
you need; the client dismisses the menu after activation, and `close(target)` is available to server callbacks.

For navigation, groups or checked choices, compose behaviors on your own markup:

```java
WebMarkupContainer trigger = new WebMarkupContainer("trigger");
trigger.add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE));
WebMarkupContainer menu = new WebMarkupContainer("menu");
menu.add(Sw.Behaviors.dropdownMenu(trigger));
menu.add(new BookmarkablePageLink<>("settings", SettingsPage.class)
        .add(Sw.Behaviors.dropdownMenuItem()));
add(trigger, menu);
```

```html
<button wicket:id="trigger" type="button">Account</button>
<div wicket:id="menu"><a wicket:id="settings">Settings</a></div>
```

Use one menu behavior per menu, with a native `type="button"` trigger. Other helpers are `dropdownMenuLabel()`,
`dropdownMenuSeparator()`, `dropdownMenuGroup()` and `dropdownMenuShortcut()`. Give groups an accessible name.
Shortcut text is a visual hint; it does not register a key binding. Use buttons or Wicket links for actionable rows,
and native form controls for ordinary form fields (`SwDropdownChoice<T>` is the native select field).

`dropdownMenuCheckboxItem(IModel<Boolean>)` and `dropdownMenuRadioItem(IModel<Boolean>)` render server-owned
`aria-checked` states and detach their models. Your callback updates the value, enforces radio exclusivity and adds
the item or menu to the Ajax target. The browser does not change checked state optimistically. Disabled items have
ARIA/native disabled state; disabled links lose their destination, and Wicket enforces server-side enabled state.

Enter/Space or Arrow Down opens with the first enabled item focused; Arrow Up opens at the last. Arrow keys,
Home/End and Page Up/Down navigate, and typing searches labels (repeated letters cycle matches). Disabled items
are skipped. Pointer opening focuses the menu; pointer movement focuses rows. Actions and Escape return focus
to the trigger, Tab/Shift+Tab close and continue through the page, and outside interaction dismisses the menu.
Placement, viewport limits, reduced motion and forced colors use the shared popup CSS in all skins.

The **Menus** showcase (`/menus`) covers Ajax actions/replacement, empty lists, checked choices, links,
confirmations and menus inside a sheet. The landing page's **Project actions** menu uses the same library script.
