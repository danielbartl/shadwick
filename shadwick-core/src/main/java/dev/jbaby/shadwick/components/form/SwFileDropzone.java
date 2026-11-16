package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;
import java.util.Objects;

/**
 * A native multipart file field enhanced with Shadleaf-style drag/drop, file listing and removal. Each chosen file is
 * shown as an attachment, in a list or, with {@link Layout#GRID}, as cards in columns; with
 * {@link #setPreviews(boolean)} an image shows a thumbnail. While an Ajax submit sends the files, each shows how far
 * its upload is, and a Cancel button aborts the request; see the file upload guide.
 */
public class SwFileDropzone extends BaseSwFileUpload<SwFileDropzone> {

    /** How the chosen files are laid out. */
    public enum Layout {
        /** Small attachments one below the other. */
        LIST,
        /** Vertical attachment cards in columns that fill the width, as an attachment group's grid. */
        GRID
    }

    private static final List<String> MESSAGES = List.of("remove", "dismiss", "selected", "removed", "cleared", "type",
            "size", "count", "uploading", "uploading.indeterminate", "processing", "failed", "done",
            "announce.uploading", "announce.uploaded", "announce.failed", "announce.cancelled");

    private Layout layout = Layout.LIST;
    private boolean previews;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwFileDropzone(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwFileDropzone(String id, IModel<List<FileUpload>> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; upload model inherited from a CompoundPropertyModel. */
    public SwFileDropzone(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwFileDropzone(String id, String label, IModel<List<FileUpload>> model) {
        this(id, Model.of(label), model, null);
    }

    public SwFileDropzone(String id, IModel<String> label, IModel<List<FileUpload>> model) {
        this(id, label, model, null);
    }

    public SwFileDropzone(String id, IModel<String> label, IModel<List<FileUpload>> model, IModel<String> description) {
        super(id, label, model, description);
        TransparentWebMarkupContainer upload = new TransparentWebMarkupContainer("upload");
        upload.add(AttributeModifier.replace("data-max-files", (IModel<Integer>) this::maximumFileCount));
        upload.add(AttributeModifier.replace("data-max-size", (IModel<Long>) () -> maximumFileSize() != null ? maximumFileSize().bytes() : null));
        upload.add(AttributeModifier.replace("data-layout", (IModel<String>) () -> layout == Layout.GRID ? "grid" : null));
        upload.add(AttributeModifier.replace("data-previews", (IModel<String>) () -> previews ? "" : null));
        for (String key : MESSAGES) {
            upload.add(AttributeModifier.replace("data-message-" + key.replace('.', '-'),
                    (IModel<String>) () -> getString("SwFileDropzone." + key)));
        }
        add(upload);
        WebMarkupContainer cancel = new WebMarkupContainer("cancel");
        cancel.add(new Label("cancelLabel", (IModel<String>) () -> getString("SwFileDropzone.cancel")));
        add(cancel);
        add(new WebMarkupContainer("imageTemplate") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(previews);
            }
        });
        add(new Label("title", (IModel<String>) () -> getString("SwFileDropzone.title")));
        Label browse = new Label("browse", (IModel<String>) () -> getString(isMultiple()
                ? "SwFileDropzone.browse" : "SwFileDropzone.browse.one"));
        browse.add(AttributeModifier.replace("for", (IModel<String>) () -> getInput().getMarkupId()));
        add(browse);
    }

    /** How the chosen files are laid out; {@link Layout#LIST} unless set. */
    public SwFileDropzone setLayout(Layout layout) {
        this.layout = Objects.requireNonNull(layout, "layout");
        return this;
    }

    public Layout getLayout() { return layout; }

    /**
     * Shows a thumbnail for each chosen image the browser can decode (not SVG, at most 20 MB) in place of the file
     * icon. The image is drawn from the file in the browser, into a canvas: it is not sent for this and no URL is
     * made, so it works under Wicket's default Content-Security-Policy. Off by default.
     */
    public SwFileDropzone setPreviews(boolean previews) {
        this.previews = previews;
        return this;
    }

    public boolean isPreviews() { return previews; }
}
