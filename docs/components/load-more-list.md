# Load-more list

A long list that shows its first items and appends more as the user asks for them, instead of pages.

> **New in 0.2.0.** `SwLoadMoreList` isn't in 0.1.0.

`SwLoadMoreList<T>` follows Wicket Oat's `OatLoadMoreList`. Give it a Wicket `IDataProvider`, a batch size and a factory
for an item's component:

```java
SwLoadMoreList<Event> feed = new SwLoadMoreList<>("feed", new EventProvider(), 20,
        (id, event) -> new EventRow(id, event))
        .setLabel("Recent activity");
add(feed);
```

```html
<div wicket:id="feed"></div>
```

- **Batches.** The first batch renders with the page. **Load more** (`SwLoadMoreList.more`) appends the next one over
  Ajax without re-rendering the items already shown, and a count beside it says how many there are ("20 of 134",
  `SwLoadMoreList.count`). Once all are shown, the button and count go.
- **Focus and announcements.** After Load more the keyboard focus moves to the first new item (the button it was on
  has gone), and screen readers hear "Showing 40 of 134" (`SwLoadMoreList.status`). Each item is a
  `<div role="listitem">` in a `role="list"` named by `setLabel`.
- **Without JavaScript** Load more is an ordinary link: the page renders again with the next batch, and the address
  ends in the new batch's id, so the browser scrolls there and the next Tab goes into the new items.
- **Load on scroll.** `setLoadOnScroll(true)` also loads the next batch when the button comes near the viewport, as in
  an endless feed. The focus then stays where it was, and the button stays as the keyboard's way in.
- **Starting again.** After the provider's filter changes, call `reset(target)` (or `reset()` in an ordinary request) to
  show the first batch again. `loadMore(target)` loads the next batch from your own code.
- **Counting.** The provider's `size()` is read once per request. If it yields fewer items than it counted (some were
  deleted meanwhile), the list ends there.

Loaded items stay in the page, so a load-more list suits hundreds of items. For more, or when users jump around, page
with a [data table](data-table.md) or [pagination](pagination.md) instead. Items added or removed between loads shift
the batches, as with any offset paging.
