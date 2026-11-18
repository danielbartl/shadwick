# Cards

Use a card to group a heading, description, content and optional actions. `SwCard` keeps your markup.

```java
add(new SwCard("card").setSize(CardBehavior.Size.SMALL));
```

```html
<section wicket:id="card">
  <header class="sw-card-header"><h2 class="sw-card-title">Workspace</h2>
    <p class="sw-card-description">Your team's current projects.</p></header>
  <div class="sw-card-content">Application content</div>
  <footer class="sw-card-footer">Application actions</footer>
</section>
```

Only `<section wicket:id="card">` is required; `SwCard` adds the card's look to your element and leaves its contents
alone, as Wicket Oat's `OatCard` does. The parts are optional classes on your own markup:

| Class | What it is |
| --- | --- |
| `sw-card-header` | The top of the card, holding the title and description; with an `sw-card-action` child, the action sits at the end. |
| `sw-card-title`, `sw-card-description` | The title (use a real heading at the level your page needs) and a muted line under it. |
| `sw-card-content` | The body. |
| `sw-card-footer` | The bottom row, usually actions. |

As in shadcn/ui, the card itself only has vertical spacing and the gap between parts; the parts add the horizontal
padding (24 pixels, 16 for `SMALL`, depending on the skin). So an image or a table placed directly in the card runs
edge to edge, while text should sit in a part: without one, it touches the card's sides.

`CardBehavior.Size.DEFAULT` and `SMALL` control size. `Sw.Behaviors.card()` styles an existing Wicket container.
Use real headings at the appropriate level; a card is not itself a button or a landmark. Put interactive actions
inside it rather than making a container with nested controls clickable. Models, actions and Ajax refresh are
ordinary Wicket behavior; the card adds no data loading or persistence.
