package dev.jbaby.shadwick.components.form;

import com.github.openjson.JSONObject;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.validation.ValidationError;

import java.util.List;

/**
 * Multipart field base shared by the native input and dropzone. FileUpload objects
 * must be processed in the submitting request: Wicket closes their streams and
 * clears the model on detach. Form-level limits also apply before field validation.
 */
public abstract class BaseSwFileUpload<F extends BaseSwFileUpload<F>>
        extends BaseSwField<List<FileUpload>, FileUploadField, F> {
    private boolean multiple = true;
    private int maxFiles = Integer.MAX_VALUE;
    private Bytes maxFileSize;

    protected BaseSwFileUpload(String id, IModel<String> label, IModel<List<FileUpload>> model, IModel<String> description) {
        super(id, label, model, description);
    }

    public F setMultiple(boolean multiple) { this.multiple = multiple; return self(); }
    public boolean isMultiple() { return maximumFileCount() > 1; }

    /** The picker/dropzone hint; applications validate accepted content on the server. */
    public F setAccept(String accept) { return setAccept(Model.of(accept)); }
    public F setAccept(IModel<String> accept) {
        getInput().add(AttributeModifier.replace("accept", accept));
        return self();
    }

    /** Maximum files in one submission, enforced on the server as well as in the dropzone. */
    public F setMaxFiles(int maximum) {
        if (maximum < 1) throw new IllegalArgumentException("The maximum file count must be positive");
        maxFiles = maximum;
        return self();
    }

    /** Per-file limit; null removes this limit. Configure the form's request limits too. */
    public F setMaxFileSize(Bytes maximum) {
        if (maximum != null && maximum.bytes() < 1) throw new IllegalArgumentException("The maximum file size must be positive");
        maxFileSize = maximum;
        return self();
    }

    protected final int maximumFileCount() { return multiple ? maxFiles : 1; }
    protected final Bytes maximumFileSize() { return maxFileSize; }

    /**
     * Clears the browser's selection after a successful Ajax submit without firing
     * another change event. Process the uploads in the current callback as usual.
     */
    public F clearSelection(AjaxRequestTarget target) {
        target.appendJavaScript("Shadwick.clearFileSelection(" + JSONObject.quote(getInput().getMarkupId()) + ")");
        return self();
    }

    @Override
    protected FileUploadField newInput(String id, IModel<List<FileUpload>> model) {
        return new FileUploadField(id, model) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("type", "file");
                if (isMultiple()) tag.put("multiple", "multiple"); else tag.remove("multiple");
                tag.remove("value");
                super.onComponentTag(tag);
            }

            @Override
            public void validate() {
                super.validate();
                List<FileUpload> files = getConvertedInput();
                if (!isValid() || files == null) return;
                if (files.size() > maximumFileCount()) {
                    error(new ValidationError().addKey("SwFileUpload.tooMany").setVariable("maximum", maximumFileCount()));
                }
                if (maxFileSize != null && files.stream().anyMatch(file -> file.getSize() > maxFileSize.bytes())) {
                    error(new ValidationError().addKey("SwFileUpload.tooLarge").setVariable("maximum", maxFileSize.bytes()));
                }
            }
        };
    }
}
