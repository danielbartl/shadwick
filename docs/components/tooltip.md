# Tooltip

Add `TooltipBehavior` to an existing control, or use the facade:

```java
button.add(Sw.Behaviors.tooltip("Download a CSV copy."));
field.addToInput(new TooltipBehavior(Model.of("Use the public project name."))
        .setPlacement(TooltipBehavior.Placement.BOTTOM)
        .setDelay(Duration.ofMillis(300)));
```

The API retains Oat's `Placement.TOP`/`BOTTOM`/`LEFT`/`RIGHT`; left/right follow the inline start/end in RTL.
Tooltips contain escaped plain text and supplement an existing accessible name. Attach them to the actual focusable
component, such as a field's input. `Mode.DESCRIPTION` preserves existing `aria-describedby` help/error references;
`Mode.LABEL` visually repeats an existing name without adding a duplicate description, for example on an icon button
with its own `aria-label`. Null or blank labels produce no tooltip.

Keyboard focus shows a tooltip immediately; pointer hover uses a 300 ms default delay. It stays while the pointer is
over the trigger or tooltip, has no interactive content, and Escape dismisses it without closing a surrounding
popover or dialog. Touch does not show hover hints. Model changes and Ajax replacement refresh the text and remove
stale tooltip elements. Without JavaScript, the native `title` remains available as a fallback. Keep essential help
in visible text or an interactive popover.

The **Popovers & tooltips** showcase (`/popups`) covers forms, nested panels, reactive hints, label mode, touch and
positioning. The landing page uses the same library script for its Java badge popover and preview-control hints.
