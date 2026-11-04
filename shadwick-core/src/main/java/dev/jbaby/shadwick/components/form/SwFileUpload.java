package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * A shadcn-styled form field wrapping a plain multi-file
 * {@code <input type="file">} ({@link FileUploadField}). {@link SwFileDropzone}
 * is the richer, drag-and-drop-capable alternative to this component.
 */
public class SwFileUpload extends BaseSwFileUpload<SwFileUpload> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwFileUpload(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwFileUpload(String id, IModel<List<FileUpload>> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; upload model inherited from a CompoundPropertyModel. */
    public SwFileUpload(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwFileUpload(String id, String label, IModel<List<FileUpload>> model) {
        this(id, Model.of(label), model, null);
    }

    public SwFileUpload(String id, IModel<String> label, IModel<List<FileUpload>> model) {
        this(id, label, model, null);
    }

    public SwFileUpload(String id, IModel<String> label, IModel<List<FileUpload>> model, IModel<String> description) {
        super(id, label, model, description);
    }
}
