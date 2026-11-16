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

## Small, and with an icon

`setSize(SwConfirmDialog.Size.SMALL)` makes the dialog narrower, centres its header and puts the two buttons side by
side in equal halves. A button is never narrower than its label: when a label does not fit its half, as happens soon
in German, Dutch or French, the buttons stack at full width with the action on top. Keyboard and reading order stay
Cancel, then the action. Button labels do not wrap, so keep them shorter than the dialog's full width.

`setIcon(name)` shows a decorative `SwIcon` above the title, or beside the title and description in a default-size
dialog on a wide screen. `setIconVariant(SwConfirmDialog.IconVariant.DESTRUCTIVE)` puts it on a destructive background,
to match a destructive action. The question itself still says what will happen: the icon is hidden from screen readers.

```java
SwConfirmDialog remove = new SwConfirmDialog("remove")
        .setSize(SwConfirmDialog.Size.SMALL)
        .setIcon("trash-2")
        .setIconVariant(SwConfirmDialog.IconVariant.DESTRUCTIVE)
        .setConfirmLabel("Remove");
```

`null` restores the default size and variant, and `setIcon(null)` removes the icon. On a 320-pixel phone the small
dialog keeps a margin at each side.
