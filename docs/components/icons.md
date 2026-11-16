# Icons

`SwIcon` renders a bundled Lucide SVG by registered name. Unnamed icons are decorative.

```java
add(new SwIcon("search", "search").setLabel("Search"));
add(new SwIcon("chevron", "chevron-right"));
```

```html
<svg wicket:id="search"></svg>
<svg wicket:id="chevron"></svg>
```

`SwIcon.names()` lists the registry. Use a fixed registered name or an `IModel<String>` for a changing icon.
`ofNullable(id, name)` creates an icon that is hidden for null; `decorativeMarkup(name)` provides trusted registry
SVG for component APIs that explicitly need markup. Do not put user-supplied SVG into those slots.

Set a label for a standalone meaningful icon; leave an icon inside an already named action decorative. Put an
`aria-label` on an icon-only button itself. Icons have no click or keyboard behavior on their own. Bind to an
`svg` or a wrapper; the component supplies its SVG attributes and body. The registry is bundled, so rendering does
not contact an external icon service.

## Size and stroke width

An icon is 1em by default, following the surrounding text, or the size a component gives its icons (16 pixels in a
button). `setSize(pixels)` fixes its width and height, from 1 to 1024 pixels; `setStrokeWidth(width)` replaces
Lucide's stroke width of 2 (more than 0, at most 24, in the icon's 24×24 units). `null` restores either default.

```java
add(new SwIcon("favourite", "star").setSize(32).setStrokeWidth(1.25).setLabel("Favourite"));
```

An explicit size renders `width`, `height` and `data-icon-size`, which the skins' icon sizing inside buttons, input
groups, alert dialogs and similar parts leaves alone. A few parts still size their icons outright, such as a badge's;
there the part's size wins. No `style` attribute is involved, so strict CSP is unaffected.
