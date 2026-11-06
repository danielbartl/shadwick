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
