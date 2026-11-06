# Confirmation dialog

`SwConfirmDialog` follows Wicket Oat's reusable `ask(...)` API. Add one to the page, then ask it from any Ajax callback:

```java
SwConfirmDialog confirm = new SwConfirmDialog("confirm").setConfirmLabel("Delete");
add(confirm);
add(SwButton.onClick("delete", "Delete invoice", target ->
        confirm.ask(target, "Delete invoice?", "This cannot be undone.", t -> {
            invoices.delete(invoice);
            t.add(table);
        })));
```

Render it on `<div wicket:id="confirm"></div>`. The native dialog has `role="alertdialog"`, takes its accessible name
and description from the question, and focuses Cancel initially. Escape and Cancel dismiss it; clicking the backdrop
leaves it open. There is no corner close button by default. The confirm button defaults to `DESTRUCTIVE`; use
`setConfirmVariant(...)` and `setConfirmLabel(...)` for other actions.

The callback stays on the server, is consumed before it runs, and is forgotten when the dialog closes. Asking another
question replaces the pending callback. The callback may call `ask(...)` again to show a follow-up question without
closing. An ACTIVE Ajax channel ignores repeated confirm clicks while a request is in flight. Titles and descriptions
also accept `IModel<String>`. Subclasses overriding `onClose` must call `super.onClose(target)` to forget the callback.
