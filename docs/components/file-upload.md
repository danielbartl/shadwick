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
The component enhances selection; transport progress, image previews and automatic submission are outside this API.

## Ajax submission and clearing

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

A full page render clears the selection, as native file inputs cannot be populated from the server. The **Uploads**
showcase (`/uploads`) covers native and Ajax submissions, a single-file picker, server count/size/type validation,
request limits, drag/drop/removal and a disabled dropzone. Its receipts keep only escaped filenames and byte counts;
file content is discarded. The landing-page reference-file demo selects a local file without uploading it.
