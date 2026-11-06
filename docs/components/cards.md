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

`CardBehavior.Size.DEFAULT` and `SMALL` control size. `Sw.Behaviors.card()` styles an existing Wicket container.
Use real headings at the appropriate level; a card is not itself a button or a landmark. Put interactive actions
inside it rather than making a container with nested controls clickable. Models, actions and Ajax refresh are
ordinary Wicket behavior; the card adds no data loading or persistence.
