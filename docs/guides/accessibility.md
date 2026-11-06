# Accessibility

Shadwick supplies native semantics, focus handling and field feedback. Accessible applications also need meaningful
names, sensible document structure and content that remains understandable as state changes.

## Name controls and regions

Keep visible labels on form fields and action buttons. Use `setLabel(...)` for standalone icons, spinners, progress,
meters and avatars when the graphic communicates information. An icon inside a named button should normally remain
decorative; the button owns the action's name. Give distinct table, pagination and navigation regions distinct names.

```java
add(new SwSpinner("loading").setLabel("Loading invoices"));
add(new SwProgress("import", progressModel).setLabel("Invoice import"));
add(new SwDataTable<>("invoices", columns, provider, 20).setLabel("Invoices"));
```

An icon-only button needs an `aria-label` or equivalent accessible name. Development mode reports missing names;
name checks do not replace evaluating whether the name describes the action accurately.

Defaults are `AccessibilityChecks.FAIL` in development and `WARN` in deployment.
`Shadwick.install(this).setAccessibilityChecks(...)` configures the policy; `OFF` disables these render-time
checks. Keep checks enabled while developing and review names in the application context.

## Keep structure meaningful

Use real headings in a sensible hierarchy, native table headers for tabular data, and the landmark roles provided
by app-shell components. Include a [skip link](../components/app-shell.md) before repeated navigation. Do not place
an input, button or link inside another interactive control. Keep status meaning in text as well as color.

Decorative skeletons, separators and unnamed icons do not announce themselves. Put a named loading status on the
surrounding region when placeholder content replaces meaningful data. A rating of zero differs from an unknown
rating; indeterminate progress differs from a meter measurement.

## Keyboard behavior

| Control | Expected behavior |
| --- | --- |
| Native fields and choices | Browser Tab, typing and selection behavior |
| Tabs | Arrow keys and Home/End; activation follows the configured automatic/manual mode |
| Menus | Arrows, Home/End, typeahead, Escape and focus return |
| Dialogs and sheets | Modal focus behavior, Escape where allowed, and return to the trigger |
| Accordion and collapsible | Native summary activation with Enter or Space |
| Calendar and picker | Grid navigation plus explicit selection; read the calendar guide for range behavior |
| Inline table editor | Enter saves, Escape cancels, and focus returns to the cell |
| Toasts | Alt+T enters notifications; Escape dismisses the focused notification |

Test nested interactions too: Escape should dismiss the active inner layer before the surrounding modal. Preserve
focus when replacing controls over Ajax, and avoid refreshing unrelated content around an active editor.

## Errors and announcements

Fields associate hints and their first inline error with the native input. Use server validation and ensure that
errors explain a correction. Page feedback panels/toasts avoid repeating the same first field error. Keep important
results in persistent page content; short-lived notifications should not be the only way to learn that work failed.

Add a `role="status"` region when an application-owned asynchronous update needs a polite announcement. Do not turn
large tables or entire pages into live regions: many ordinary row updates do not need to be read aloud.

## Progressive enhancement

Native fields, standalone forms, links, tables and disclosures retain useful behavior without JavaScript. Enhanced
menus, selects and date pickers have their own fallback contracts. Ajax callbacks, selection/bulk actions, nested
cell forms and browser appearance preferences need their documented runtime. Read each guide's integration notes
when a no-script flow matters to your application.

## Verify your application

Try keyboard-only navigation, at least one screen reader, browser zoom and narrow screens. Check your color-token
overrides for contrast in light and dark mode. The skins share reduced-motion and forced-color behavior, but your
own CSS, custom slots, chart labels and third-party content need the same review. Automated axe checks help catch
many issues; they do not evaluate the clarity of names, the accuracy of descriptions or every assistive-technology
interaction.
