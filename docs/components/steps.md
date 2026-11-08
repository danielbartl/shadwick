# Steps indicator

`SwSteps` shows where an ordered process stands. It follows Wicket Oat’s title/current models and optional
completed-step selection API, with the existing Shadwick skin tokens and native ordered-list semantics.

```java
var current = Model.of(1);
var steps = new SwSteps("stages", List.of("Details", "Review", "Publish"), current)
        .setLabel("Project stages");
steps.setOnSelect((target, index) -> {
    current.setObject(index);
    target.add(steps);
});
form.add(steps);
```

```html
<div wicket:id="stages"></div>
```

The constructors accept either a `List<String>` or an `IModel<? extends List<String>>`, plus a required
`IModel<Integer>` for the current index. Indices start at zero. Earlier steps are completed, the current step
has `aria-current="step"`, and later steps are upcoming. A null/negative index means not started. An index at
or beyond the list size means all completed, without a current item. Null lists render empty; single-step lists
use the same rules. Titles are escaped, including strings that look like markup. Null/blank entries render
without a title; provide meaningful titles for useful process names.

## State and model ownership

Titles are copied in their supplied order for each render, and the current index is also read through a render
snapshot. Updating either model and rendering the component repopulates its list. Source models and a configured
label model detach even while the component is hidden. The component has an output markup ID, so it can be added
directly to an Ajax target. Replacing the entire component is supported too.

The indicator does not own a form, advance progress, validate fields, fetch stages or enforce application
authorization. The `/steps` example composes ordinary native/Ajax submission, a required project-name field,
completion/reset commands and a replaceable indicator. Form validation happens in those controls; the indicator
continues to show the model’s current stage when validation fails. A normal native submit works without script.

## Selecting completed steps

Without `setOnSelect`, all stages are plain text. With it, only completed stages become native anchors handled
by Wicket Ajax. Current and future stages remain inert spans rather than disabled controls or fake buttons.
The callback receives the Ajax target and selected zero-based index. It must update the application’s models
and add the appropriate components to the target. Selecting a completed stage does not submit a surrounding
form, so returning to an earlier stage can preserve an unsaved draft. Application code owns focus navigation;
the example explicitly focuses its project-name input after returning to a completed stage.

Callbacks recheck the current index, list bounds and captured title, and require an enabled component and a
current callback. An old rendered link cannot select a step that has become future, disappeared or changed title
in that component’s current model. Set the callback to null to remove selection. Business rules and permissions
must still be checked by the callback. Indices and titles are not stable entity identifiers; applications that
reorder a live process should rebuild its indicator and resolve their own stage identity before navigation.

After a callback, render snapshots are cleared so title/current model updates appear in the same Ajax response.
The source models still follow their normal request detach lifecycle. A callback that changes server state
without adding the indicator to its target leaves the rendered state unchanged until the next render.

Without JavaScript, completed action anchors are hidden through the scripting media query and a `noscript`
copy shows the same number, title and state as plain text. The list remains fully readable without offering
nonfunctional navigation links. This fallback does not invoke the Ajax callback. Supply ordinary native form
controls or application links when the process also needs native navigation.

## Accessibility, localization and layout

The component renders an `ol` with explicit `role="list"` and native `li` children. Every item shows its number,
title and a visible localized state caption: Completed, Current or Upcoming. The current item additionally has
`aria-current="step"`; there is at most one current item per list. State is communicated in text as well as
marker colors. The component adds no progressbar, tablist, toolbar, live region or automatic focus move.
Completed links use ordinary Tab/Shift+Tab and Enter behavior, with visible focus outlines.

The default list name “Steps” and all three captions ship in English, German, French, Spanish, Italian, Japanese,
Dutch, Portuguese and Croatian. Override `SwSteps.label`, `.done`, `.current` and `.upcoming` through normal Wicket
resources. `setLabel(String/IModel<String>)` supplies a process-specific accessible name; null/blank values use
the localized default. Step titles and application messages remain application-owned.

At container widths of at least 32rem, numbered stages sit in columns with horizontal connectors. Narrower
containers stack them vertically, including a compact sidebar on a wide screen. Long and unbroken titles wrap
without covering other markers. Logical positioning follows LTR/RTL, and all nine skins supply their own type,
density and marker shape. Sera uses square markers and Sketch retains its irregular border/dashed connectors.
Forced colors preserve explicit outlines, completed fills and a heavier current border. Print styles retain
text, marker borders and connectors without depending on background printing. There is no animation, new
runtime or dependency; completed selection uses Wicket’s existing Ajax support and strict CSP.
