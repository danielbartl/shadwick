# Progress

Use `SwProgress` for native task completion or `SwRadialProgress` for an SVG ring. A null value is indeterminate.

```java
add(new SwProgress("work", completedModel).setLabel("Import progress"));
add(new SwRadialProgress("profile", completedModel).setLabel("Profile completion"));
```

```html
<progress wicket:id="work"></progress>
<div wicket:id="profile"></div>
```

## Values and options

The default maximum is 100. Both accept `(id, valueModel, maxModel)`. Finite values are clamped to the configured
bounds; invalid maxima or nonfinite numbers fail clearly. Null means unknown progress, rather than zero progress.
The radial percentage is localized; numeric HTML/SVG attributes keep machine formatting.

Native progress supports `ProgressBehavior.Variant.DEFAULT`, `SECONDARY` and `DESTRUCTIVE`. Radial progress has
its own `SwRadialProgress.Variant` with equivalent variants, plus model-based `setText(...)` for center content
such as `3/5`. Keep custom text consistent with the numeric value supplied to assistive technology.

`Sw.Behaviors.progress()` styles caller-owned native progress while preserving its values. Use a
[meter](./meters.md) for a measurement with quality thresholds rather than task completion.

## Accessibility and integration

Supply `setLabel(...)`, `aria-label`, `aria-labelledby` or a native associated label. Progress is not a clickable
control and does not need a tab stop. Native progress works without JavaScript; radial progress is server-rendered
SVG. Unknown-state animation respects reduced motion. To change a reading over Ajax, update its model and refresh
the containing output-ID component; Shadwick does not poll a background task for you.
