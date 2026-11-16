# File upload

`SwFileUpload` is a native file input; `SwFileDropzone` adds Shadleaf's dropzone appearance, a selected-file list,
removal buttons and announcements. Both bind to `IModel<List<FileUpload>>` or an inherited `List<FileUpload>` property,
use the regular field label/description/error API, and default to multiple selection:

```java
Form<Documents> form = new Form<>("form", new CompoundPropertyModel<>(documents));
form.setMultiPart(true);
form.setMaxSize(Bytes.megabytes(10));       // whole multipart request, including overhead
form.setFileMaxSize(Bytes.megabytes(5));    // parser limit for each file
form.add(new SwFileDropzone("files").setRequired(true).setAccept(".pdf,.txt")
        .setMaxFiles(3).setMaxFileSize(Bytes.megabytes(2))
        .setDescription("Up to three documents, 2 MB each."));
```

```html
<form wicket:id="form" method="post">
    <div wicket:id="files"></div>
    <button wicket:id="save" type="submit">Upload</button>
</form>
```

## Storage and request lifecycle

Add a normal Wicket submit button or `SwSubmitButton` to process the form. **Read or persist every `FileUpload` inside
the submit callback.** Wicket closes upload streams and clears the upload model at request detach; do not retain
`FileUpload` objects in page/session state or asynchronous work. The library does not store files automatically.
Use `getInputStream()` for streaming application processing; use application storage keys rather than treating a
client filename as a destination path.

## Limits and validation

`setMultiple(false)` or `setMaxFiles(1)` renders a single picker and enforces that limit server-side.
`setMaxFiles(...)` and `setMaxFileSize(Bytes)` validate the submitted files, including requests that bypass the browser.
A named zero-byte file counts as a selection. Parser-level failures occur before field validation: keep form request
limits in place and show a `SwFeedbackPanel` for form-level errors. In an Ajax form, add that panel to the target in
the error callback; inline field errors update themselves. `setAccept(...)` is a picker/drop hint, not server-side
content validation—add application validators appropriate to the files you process.

## Dropzone and native behavior

With the dropzone, repeated picks and drops append files, matching name/size/modification-time duplicates are ignored,
and files can be removed before submission. Files rejected by its type/count/size hints are listed separately and
excluded from the native `FileList`. Native selection and keyboard access still work without JavaScript. If the browser
lacks the `DataTransfer` support needed for enhancement, selection uses the native picker and removal buttons stay disabled.
Each chosen file shows as a small [attachment](attachments.md) with its name and size, a rejected one as an error card
with the reason. The dropzone never submits by itself.

### Grid and image previews

```java
SwFileDropzone photos = new SwFileDropzone("photos", "Inspection photos", Model.ofList(new ArrayList<>()))
        .setLayout(SwFileDropzone.Layout.GRID)
        .setPreviews(true)
        .setAccept("image/*,.pdf");
```

`Layout.GRID` shows the files as cards in columns that fill the width, the grid of an attachment group, with the remove
button in each card's corner; order, focus after a remove and announcements stay those of the list. With
`setPreviews(true)` an image shows a thumbnail in place of the file icon: big enough in the grid to check, say, the
photos of an inspection before they are sent.

- **Drawn in the browser.** The thumbnail is decoded from the chosen file (`createImageBitmap`) and drawn into a canvas.
  Nothing is sent for it and no URL is made, so it works under Wicket's default Content-Security-Policy: no
  `img-src blob:` is needed, and no page-origin URL for the file is ever opened.
- **Which files.** Images the browser can decode, up to 20 MB (a larger photo takes much memory to decode). SVG keeps its
  icon, and so does a file the browser cannot decode (HEIC outside Safari): the thumbnail replaces the icon only once it
  is drawn, so no broken image shows. Its canvas is hidden from assistive technology; the file's name is beside it.

## Ajax submission, progress and clearing

After an invalid Ajax submit, leave the upload component out of `target.add(...)` to keep the selection and focus.
The browser sends those files again on the next submit. After success, clear the browser selection explicitly:

```java
SwFileDropzone files = new SwFileDropzone("files", "Documents", Model.ofList(new ArrayList<>()));
form.add(files);
form.add(SwSubmitButton.onSubmit("save", "Upload", target -> {
    service.process(files.getModelObject()); // consume files during this request
    files.clearSelection(target);           // no extra change event or submit
}));
```

### Upload progress and Cancel

When an Ajax submit of a multipart form sends a dropzone's files (`SwSubmitButton`, `AjaxButton` and the like), the files
show how far it is, through the [attachment's states](attachments.md), with nothing to configure:

1. **Uploading.** Each file shows the share of its bytes sent, "Uploading · 64%". The browser reports the progress of the
   whole request; the files go into it one after the other, so each one's share is worked out from where its bytes are.
   A **Cancel upload** button takes the Browse button's place; it aborts the request, and the files are back as they
   were, ready to send again. Meanwhile files cannot be added or removed.
2. **Processing**, once every byte is sent and the server is working on them. Cancel is gone now: aborting could no
   longer undo what the server does with them.
3. **The outcome.** Wicket answers 200 also when validation fails, so a finished request alone leaves the files as they
   were, in the input. `clearSelection(target)` in the success callback, as above, shows them as **Uploaded** until the
   next choice, and empties the input so they are not sent twice. A failed request (an error status, a dropped
   connection, a timeout) shows **Upload failed** and keeps them, ready to send again.

A screen reader hears the start, the end, a failure or a cancel, not each percentage. A normal (non-Ajax) submit shows
the browser's own progress, if any. This works with both of Wicket's Ajax engines (jQuery, the default, and the native
one). The form's own limits (`setMaxSize`, `setFileMaxSize`, `setFileCountMax`) and your servlet container's request
limits should allow what the form may send: a request over them fails as a whole.

A full page render clears the selection, as native file inputs cannot be populated from the server. The **Uploads**
showcase (`/uploads`) covers native and Ajax submissions, a single-file picker, server count/size/type validation,
request limits, drag/drop/removal, a photo grid with previews and progress (with a slow server to show processing and
Cancel) and a disabled dropzone. Its receipts keep only escaped filenames and byte counts;
file content is discarded. The landing-page reference-file demo selects a local file without uploading it.
