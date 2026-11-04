package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/** A native multipart file field enhanced with Shadleaf-style drag/drop, file listing and removal. */
public class SwFileDropzone extends BaseSwFileUpload<SwFileDropzone> {

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
        for (String key : List.of("remove", "dismiss", "selected", "removed", "cleared", "type", "size", "count")) {
            upload.add(AttributeModifier.replace("data-message-" + key,
                    (IModel<String>) () -> getString("SwFileDropzone." + key)));
        }
        add(upload);
        add(new Label("title", (IModel<String>) () -> getString("SwFileDropzone.title")));
        Label browse = new Label("browse", (IModel<String>) () -> getString(isMultiple()
                ? "SwFileDropzone.browse" : "SwFileDropzone.browse.one"));
        browse.add(AttributeModifier.replace("for", (IModel<String>) () -> getInput().getMarkupId()));
        add(browse);
    }
}
