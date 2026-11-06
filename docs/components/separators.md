# Separators

A separator divides related groups visually. It is decorative by default.

```java
add(new SwSeparator("divider"));
add(new SwSeparator("columns").setOrientation(SeparatorBehavior.Orientation.VERTICAL)
        .setDecorative(false));
```

```html
<div wicket:id="divider"></div>
<div class="my-flex-row"><div wicket:id="columns"></div></div>
```

Use `HORIZONTAL` or `VERTICAL`; a vertical divider needs a flex parent with an appropriate height in your own CSS.
`setDecorative(false)` exposes a separator role and the vertical orientation to assistive technology. Keep ordinary
visual dividers decorative to avoid unnecessary announcements. `Sw.Behaviors.separator()` styles an existing
container. Separators have no focus, model input or Ajax action behavior.
