# Buttons

Ajax actions, submit buttons, variants, sizes and icon-only names.

```java
// A button that runs over Ajax
add(SwButton.onClick("save", "Save", target -> save()));
add(SwButton.onClick("delete", "Delete", target -> delete())
        .setVariant(ButtonBehavior.Variant.DESTRUCTIVE));

// Any link or button styled as a shadcn button
add(new BookmarkablePageLink<>("home", HomePage.class)
        .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL)));
```

```html
<button wicket:id="save"></button>
<a wicket:id="home">Home</a>
```

Variants: `DEFAULT`, `SECONDARY`, `OUTLINE`, `GHOST`, `DESTRUCTIVE`, `LINK`. Sizes: `EXTRA_SMALL`, `SMALL`,
`DEFAULT`, `LARGE`, and the square icon sizes `ICON_EXTRA_SMALL`, `ICON_SMALL`, `ICON` and `ICON_LARGE`. An icon-only
button needs an `aria-label`: in development mode, a button without an accessible name fails to render with a message
saying how to fix it, and in deployment it is logged.

> **New in 0.2.0.** The sizes `EXTRA_SMALL`, `ICON_EXTRA_SMALL`, `ICON_SMALL` and `ICON_LARGE`; 0.1.0 has `SMALL`, `DEFAULT`, `LARGE` and `ICON`.

```java
SwButton remove = new SwButton("remove") {
    @Override
    public void onClick(AjaxRequestTarget target) { remove(); }
}.setSize(ButtonBehavior.Size.ICON_SMALL).setVariant(ButtonBehavior.Variant.GHOST);
remove.add(new SwIcon("icon", "x"));
add(remove);
```

```html
<button wicket:id="remove" aria-label="Remove"><svg wicket:id="icon"></svg></button>
```

### Hit area on a touch screen

> **New in 0.2.0.** 0.1.0 doesn't enlarge the hit area.

`ICON_EXTRA_SMALL`, `ICON_SMALL` and `ICON` are smaller than the 44 by 44 pixels touch guidelines ask for. On a touch
screen (`@media (pointer: coarse)`) such a button gets an invisible area around it, so it is at least 44 by 44 pixels
to tap without looking any bigger. With a mouse the hit area is the button itself. Where two enlarged neighbours meet,
the later one takes the overlap, so keep the usual gap between them. A button inside an input group, next to the text
it belongs to, is not enlarged, nor a notification's close button, whose list scrolls at the screen's edge.
`ICON_LARGE` is large enough as it is.

For connected actions or a primary action with alternatives, see [button groups and split buttons](button-groups.md).
