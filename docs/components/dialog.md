# Dialog

A modal on the native `<dialog>` element, with the same API as Wicket Oat's `OatDialog`:

```java
SwDialog dialog = new SwDialog("delete", "Delete the project?") {
    @Override
    protected void onConfirm(AjaxRequestTarget target) {
        projects.delete(project);
    }
};
dialog.setDescription("This cannot be undone.");
dialog.setConfirmLabel("Delete").setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE);
add(dialog);
add(SwButton.onClick("openDelete", "Delete project", dialog::open));
```

Give it content with `setContent(...)` (id `SwDialog.CONTENT_ID`); its form fields are validated on Confirm, and it
stays open while one has an error. `setTrigger("About")` adds a button that opens it in the browser, without a
request.

After a failed Confirm, a Shadwick field shows its input's first error in place, keeping the focus and what was typed.
Any other error - on the form or the dialog, on a field itself rather than its input, or a second one on the same
input - re-renders the form, so a feedback panel or an `SwFormErrors` summary in the content can show it. Session
messages left over from an earlier request don't keep the dialog open.

> **Changed in 0.2.0, not yet released.** In 0.1.0 an error reported on a field itself, or a second error on its input,
> wasn't shown, and an unrelated session error message kept the dialog open.
