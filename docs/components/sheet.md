# Sheet

`SwSheet` is a native modal panel with `SwDialog`'s content, validation and open/close API, following OatDrawer's role:

```java
SwSheet settings = new SwSheet("settings", "Project settings") {
    @Override
    protected void onConfirm(AjaxRequestTarget target) {
        service.save(project); // content fields have validated and updated their models
    }
};
settings.setDescription("Edit the project, then save your changes.")
        .setContent(new SettingsPanel(SwSheet.CONTENT_ID, project))
        .setConfirmLabel("Save")
        .setTrigger("Edit project")
        .setSide(SwSheet.Side.END);
add(settings);
```

Render it on `<div wicket:id="settings"></div>`. `Side.END` is the default; `START` and `END` follow text direction,
while `TOP` and `BOTTOM` span the viewport width. The header and footer stay visible while the body scrolls. The
corner close button, Cancel, Escape and backdrop dismiss it. `setFooterVisible(false)` supports informational sheets.

Open it from any Ajax handler with `open(target)`, or use `open(content, target)` for temporary content removed on
close. Re-rendering an open sheet keeps it modal. Invalid fields update in place and keep focus; unconfirmed browser
input is cleared when it closes. Native modality handles the inert background and focus return. The **Overlays**
showcase (`/overlays`) covers confirmations, follow-up questions, nested overlays and all four sheet edges. The landing
page previews a confirmation and an end sheet as browser-only demos.
