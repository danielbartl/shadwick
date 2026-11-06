# Attachments

`SwAttachment` displays a stored file or image. It does not upload files or retain Wicket's request-scoped
`FileUpload` objects. Supply application-owned URLs and authorize access at their endpoints.

```java
var notes = new SwAttachment("notes", Model.of("Project notes.txt"))
        .setDescription("Text document · 4 KB")
        .setFullWidth(true)
        .setLink("/files/notes", "Open Project notes.txt");
add(notes);

var preview = new SwDialog("preview", "Landscape preview").setFooterVisible(false);
preview.setContent(new LandscapePanel(SwDialog.CONTENT_ID));
add(preview);
add(new SwAttachment("photo", "Wicklow hills.svg")
        .setDescription("Illustration · 800 × 600")
        .setImageUrl("/files/wicklow/thumbnail")
        .setOrientation(AttachmentBehavior.Orientation.VERTICAL)
        .setPreview(preview.get("dialog"), "Preview Wicklow hills.svg"));
```

```html
<div wicket:id="notes"></div>
<div wicket:id="photo"></div>
<div wicket:id="preview"></div>
```

Title, description, image URL and image alt accept String/model setters. Models load once per request and detach.
A thumbnail crops to a square; the default file icon remains as its fallback while loading or after failure.
The picture is decorative beside a filename. `setShowContent(false)` creates a compact media-only card whose media
and fallback use the title as their accessible name; `setImageAlt(...)` can describe the picture instead.
`setIcon(...)` changes the fallback; `setMedia(component)` accepts custom markup using `MEDIA_ID` (`mediaContent`).

`setActions(panel)` accepts arbitrary Wicket markup using `ACTIONS_ID` (`actions`). For example, a Panel can contain
a native `ResourceLink` with a `download` attribute, a preview link and an Ajax remove button. Add
`Sw.Behaviors.attachmentAction().setLabel("Download Project notes.txt")` to each icon control. It reuses existing
button styling/accessibility checks, or installs a compact ghost button when none is present. With `SwButton`,
choose `GHOST` and `ICON` yourself and use a null label model to render its icon markup.
The actions stay above the stretched trigger, occur before it in tab order and never activate it.

`setLink(url, label)` creates a native card link. `setPreview(dialogElement, label)` creates a native button with
`commandfor`/`show-modal`; pass the actual dialog element, such as `preview.get("dialog")`. This opens in the browser
without JavaScript in supporting browsers; the existing runtime supplies its older-browser fallback. Such an external
native invoker does not call `SwDialog.open(target)` or synchronize its server-side `isOpen()` state. If an application
needs that state or replaces an open dialog over Ajax, supply a named `SwButton` trigger that calls `preview.open(target)`
via `setTrigger(...)`, using `TRIGGER_ID` (`trigger`). A custom trigger binds to an anchor in the supplied template;
a raw `WebMarkupContainer` can change its tag to a button in `onComponentTag`.
Disabled native links lose their href and tab stop; disabled buttons use the native disabled attribute.

`AttachmentBehavior.State` is `DONE` (default), `IDLE`, `UPLOADING`, `PROCESSING` or `ERROR`. The working states set
`aria-busy` and shimmer the title; reduced motion and forced colors disable shimmer. State is presentation only:
also supply readable text such as “Uploading · 64%” or “Upload failed · Please try again”. Cards add no live regions;
a page can announce updates in one polite status region. Sizes are `DEFAULT`, `SMALL`, `EXTRA_SMALL`.

`SwAttachmentGroup` owns a caller-supplied container. Its default `ROW` layout scrolls and snaps; `GRID` wraps.
Use direct attachment children (set ListView items to render their body only), vertical/full-width cards in a grid,
and `setLabel("Project files")` for a named group. A named row takes keyboard focus for native arrow-key scrolling;
a grid adds no tab stop. RTL and print layouts are supported. The same APIs are available as
`Sw.Behaviors.attachment()`, `attachmentGroup()`, `attachmentAction()` and `attachmentTrigger()` on existing components;
caller-owned part markup uses `sw-attachment-media`, `-content`, `-title`, `-description` and `-actions` classes.

The `/attachments` showcase includes working downloads, a native image preview, row/grid groups, sizes, image fallbacks,
Ajax removal/restoration and a server-rendered status cycle. The landing studio includes a downloadable illustration
with a preview dialog using the actual embedded CSS and runtime.
