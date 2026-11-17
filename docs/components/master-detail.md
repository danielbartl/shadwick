# Master–detail

A list of items beside the details of the one selected, one pane at a time where there is room for one.

> **New in 0.2.0.** `SwMasterDetail` isn't in 0.1.0.

`SwMasterDetail<T>` follows Wicket Oat's `OatMasterDetail`. The list is yours; give it a factory, and one for the details
of the selected item:

```java
SwMasterDetail<Ticket> inbox = new SwMasterDetail<>("inbox", new Model<>());
inbox.setMaster(id -> new TicketList(id, inbox));   // its links call inbox.select(target, ticket)
inbox.setDetail((id, ticket) -> new TicketPanel(id, ticket));
add(inbox);
```

```html
<div wicket:id="inbox"></div>
```

- **Selecting.** The list's items call `select(AjaxRequestTarget, item)` from an Ajax link, or `select(item)` from an
  ordinary link, which also works without JavaScript. The detail factory is called again with the selection model;
  until something is selected an [empty state](empty-state.md) asks for a choice (`newEmptyState(id)` changes it).
  A selection set through the model directly also gets its details on the next render.
- **Marking the selection** is the list's: use `isSelected(item)` (for example for `aria-current`) and re-render the
  list in the same request. A `ListView` with `setReuseItems(true)` keeps its links' ids when it re-renders, so the
  keyboard focus stays on the link that was used. A reused item isn't populated again, so read the item through
  `item.getModel()` in `populateItem` (`item.getModel().map(Ticket::subject)`) rather than its value at the time.
- **One pane at a time.** The component measures its own width: narrower than 40rem (a phone, or a narrow column)
  it shows only the list or only the details. Choosing an item shows the details with a **Back** button
  (`SwMasterDetail.back`) to the list; the selection stays. The switch doesn't re-render the list, so it keeps its scroll
  position, and the keyboard focus follows: to the details, and on Back to the item it came from. `showList(target)`
  (or `showList()`) goes back from your own code.

The panes are plain containers, so the list can be anything - a navigation list, a table, cards - and so can the
details.
