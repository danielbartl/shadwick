# Collapsible

`SwCollapsible` provides the same native disclosure without an accordion's group or heading semantics:

```java
add(new SwCollapsible("more", "A little more about this workspace")
        .setVariant(ButtonBehavior.Variant.OUTLINE)
        .setContent(new WorkspaceDetailsPanel(SwCollapsible.CONTENT_ID)));
```

```html
<details wicket:id="more"></details>
```

The label is escaped and accepts String/model values, as do text-content overloads. `setContent(Component)` accepts
arbitrary markup with `CONTENT_ID`; `getTrigger()` and `getContent()` expose the parts. A plain summary is the default;
`setVariant` or `setSize` draws it as a button using existing skins. `setOpen(true)` renders it open. `isOpen()` reports
that configured initial state; native user toggles do not post to Wicket. Re-render the component to apply server
configuration. Disabling it locks/skips the summary and omits content when initially closed, while initially open
content stays readable. It works without JavaScript and does not animate its height. `Sw.Behaviors.collapsible()` and
`.collapsibleTrigger()` support caller-owned details/summary; manage content visibility when disabling that markup.

The **Tabs & disclosures** showcase (`/disclosure`) includes bookmarkable tabs, manual/vertical/nested views, native
single/multiple accordions, disabled sections, fragment links and Ajax refreshes. The landing studio's Actions/Details
tabs preview all three families. Lazy server-created tab panels through Wicket's `ITab`/`AjaxTabbedPanel` remain a
separate integration. The data-table module now supplies `wicket-extensions`, so the dependency is available for that future work.
