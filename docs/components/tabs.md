# Tabs

`SwTabs<T>` follows OatTabs' caller-owned container API: two lists from one model, with `populateTab` and
`populatePanel` hooks or the `of`/`Sw.Components.tabs` factory. Every panel renders up front; the browser switches
views without a server request, preserving entered values in the panels:

```java
add(SwTabs.of("tabs", Model.ofList(List.of("Account", "Preferences")),
        (item, title) -> item.add(new Label("tabLabel", title)),
        (item, title) -> item.add(new Label("panelContent", title + " content")))
        .setLabel("Workspace settings").setSelectedTab(0));
```

```html
<div wicket:id="tabs">
    <div wicket:id="tablist">
        <button wicket:id="tabButtons"><span wicket:id="tabLabel"></span></button>
    </div>
    <div wicket:id="tabPanels"><div wicket:id="panelContent"></div></div>
</div>
```

The component owns trigger/panel IDs and their ARIA relationships. It renders the initial selected panel and hides
the others before scripts run. `setVariant(TabsBehavior.Variant.LINE)` selects an underline instead of the raised
track. `setOrientation(TabsBehavior.Orientation.VERTICAL)` gives a column;
`setActivation(TabsBehavior.Activation.MANUAL)` moves focus with the arrows and waits for Enter or Space to switch.
The default is horizontal/automatic. Left/Right wrap and follow RTL, vertical tabs use Up/Down, and Home/End reach
the first/last enabled tab. Tab enters on the selected tab and then reaches its panel. Disabled entries are skipped.
Manual focus remains stable during DOM updates and returns the list's tab stop to the selected tab when focus leaves.

For links that also work without JavaScript, subclass and override `tabUrl(T item, int index)` to return a
bookmarkable Wicket URL, for example `urlFor(SettingsPage.class, new PageParameters().set("tab", index))`. The component
renders anchors instead of buttons. Read the parameter and call `setSelectedTab(...)` when constructing the page.
Normal clicks switch in place and use `history.replaceState`, keeping history state and adding no history entry;
modified clicks keep native navigation. Button tabs need JavaScript to switch; without it the initial panel remains
visible. Only same-origin HTTP(S) links targeting this window are enhanced.

Override `isTabVisible(T item, int index)` and `isTabEnabled(...)` to configure both lists together. Hidden entries
omit their panels too; invalid/hidden/disabled initial selections fall back to the first enabled visible entry.
If all visible entries are disabled, the first stays visible as read-only context. Empty groups hide with an
Ajax-safe placeholder. The shared list model is read once per request and detached.

`setSelectedTab` configures server rendering; browser choices do not update a server model. Replacing the whole
component with `target.add(tabs)` restores that configured selection. Updating content inside an existing root keeps
its active view. Panels may contain other tabs; IDs, focus and state stay scoped to their own root. Once enhanced,
inactive enabled panels use `hidden="until-found"`: find-in-page or a fragment link reveals the matching tab, through
nested panels too, in supporting browsers. A non-bubbling `sw-tabs-show` event fires on a newly shown panel, not at
initialization. Disabled panels stay plainly hidden. Hidden-panel geometry remains zero even with host panel styles.
`Sw.Behaviors.tabs()` adds the same runtime/style to caller-owned markup with its own IDs, ARIA and initial state.
