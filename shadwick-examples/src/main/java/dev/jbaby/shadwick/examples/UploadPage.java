package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwFeedbackPanel;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.BaseSwFileUpload;
import dev.jbaby.shadwick.components.form.SwFileDropzone;
import dev.jbaby.shadwick.components.form.SwFileUpload;
import dev.jbaby.shadwick.components.form.SwTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.validation.ValidationError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Native multipart submission and progressively enhanced dropzones; demo content is read, then discarded. */
public class UploadPage extends BasePage {
    public UploadPage(PageParameters parameters) {
        super(parameters);
        Label saved = new Label("saved", Model.of(""));
        Form<Documents> form = new Form<>("form", new CompoundPropertyModel<>(new Documents())) {
            @Override protected void onSubmit() { saved.setDefaultModelObject(receipt(getModelObject())); }
        };
        limits(form);
        form.add(new SwTextField<String>("title").setRequired(true));
        form.add(configure(new SwFileUpload("files")));
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved, new SwFeedbackPanel("feedback", form));
        add(form);

        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Documents> ajaxForm = new Form<>("ajaxForm", new CompoundPropertyModel<>(new Documents()));
        limits(ajaxForm);
        ajaxForm.add(new SwTextField<String>("title").setRequired(true));
        SwFileDropzone dropzone = configure(new SwFileDropzone("files"));
        ajaxForm.add(dropzone);
        SwFeedbackPanel feedback = new SwFeedbackPanel("feedback", ajaxForm);
        feedback.setOutputMarkupId(true);
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Upload documents", target -> {
            ajaxSaved.setDefaultModelObject(receipt(ajaxForm.getModelObject()));
            dropzone.clearSelection(target);
            target.add(ajaxSaved, feedback);
        }, target -> target.add(feedback)));
        ajaxForm.add(ajaxSaved, feedback);
        add(ajaxForm);

        Form<Void> single = new Form<>("singleForm");
        limits(single);
        SwFileUpload document = configure(new SwFileUpload("document", "Single document", Model.ofList(new ArrayList<>())))
                .setMultiple(false).setDescription("One .txt file, up to 64 KB. Submitted over Ajax.");
        Label singleSaved = new Label("saved", Model.of(""));
        singleSaved.setOutputMarkupId(true);
        SwFeedbackPanel singleFeedback = new SwFeedbackPanel("feedback", single);
        singleFeedback.setOutputMarkupId(true);
        single.add(document, singleSaved, singleFeedback);
        single.add(SwSubmitButton.onSubmit("save", "Upload one file", target -> {
            singleSaved.setDefaultModelObject("Received single: " + describe(document.getModelObject()) + ".");
            document.clearSelection(target);
            target.add(singleSaved, singleFeedback);
        }, target -> target.add(singleFeedback)));
        add(single);

        Model<Integer> ticks = Model.of(0);
        Label heartbeat = new Label("heartbeat", (IModel<String>) () -> "Server checks: " + ticks.getObject());
        heartbeat.setOutputMarkupId(true);
        add(heartbeat);
        add(SwButton.onClick("unrelated", "Check server status", target -> {
            ticks.setObject(ticks.getObject() + 1);
            target.add(heartbeat);
        }));
        var disabled = new SwFileDropzone("managed", "Managed attachments", Model.ofList(new ArrayList<>()))
                .setDescription("This upload is managed by your workspace.");
        disabled.setEnabled(false);
        add(disabled);
    }

    private static void limits(Form<?> form) {
        form.setMultiPart(true);
        form.setMaxSize(Bytes.kilobytes(256));
        form.setFileMaxSize(Bytes.kilobytes(96));
        form.setFileCountMax(10);
    }

    private static <F extends BaseSwFileUpload<F>> F configure(F field) {
        return field.setRequired(true).setAccept(".txt").setMaxFiles(3).setMaxFileSize(Bytes.kilobytes(64))
                .setDescription("Up to three .txt files, 64 KB each. This demo reads and discards their content.")
                .addValidator(value -> {
                    if (value.getValue().stream().anyMatch(file -> !file.getClientFileName().toLowerCase(java.util.Locale.ROOT).endsWith(".txt"))) {
                        value.error(new ValidationError().setMessage("Choose .txt files for this example."));
                    }
                });
    }

    private static String receipt(Documents documents) {
        return "Received " + documents.title + ": " + describe(documents.files) + ".";
    }

    private static String describe(List<FileUpload> files) {
        return files.stream().map(file -> file.getClientFileName() + " (" + file.getBytes().length + " bytes)")
                .collect(Collectors.joining(", "));
    }

    public static class Documents implements Serializable {
        public String title;
        public List<FileUpload> files;
    }

    @Override protected String getTitle() { return "File upload"; }
}
