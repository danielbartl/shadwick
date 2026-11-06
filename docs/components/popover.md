# Popover

`SwPopover` follows OatPopover's content-factory API and renders a native, non-modal popover:

```java
SwPopover filters = new SwPopover("filters", "Filters", id -> new FilterPanel(id, filter))
        .setTitle("Filter projects")
        .setDescription("Choose which projects to show.")
        .setSide(SwPopover.Side.BOTTOM)
        .setAlign(SwPopover.Align.START);
add(filters); // <div wicket:id="filters"></div>
```

The factory receives `SwPopover.CONTENT_ID`. `getTrigger()` exposes the native button for attributes or behaviors;
`setTriggerVariant(...)` changes its default `OUTLINE` appearance. `setContent(...)` can replace the content later.
The optional title names the panel; without it, the trigger label supplies its accessible name. Titles, descriptions
and trigger labels accept models.

The button opens it without a request. Focus moves to its first control (or the panel for informational content),
and Escape, outside clicks or tabbing out dismiss it. The page stays interactive. A nested popover's Escape closes
that panel and returns focus to its trigger. `Side.START`/`END` follow text direction; `TOP`/`BOTTOM` and
`Align.START`/`CENTER`/`END` control placement. CSS anchors position and flip the panel without inline styles. Older
Popover API browsers without CSS anchor positioning show a viewport panel instead.

Re-render `getContent()` to update an open popover; replacing the entire `SwPopover` resets it to closed. A content
panel may own a Wicket `Form` and `SwSubmitButton`, including normal inline field feedback. After applying changes,
call `filters.close(target)` to dismiss it and return focus when focus was inside it. Open state stays in the browser.
