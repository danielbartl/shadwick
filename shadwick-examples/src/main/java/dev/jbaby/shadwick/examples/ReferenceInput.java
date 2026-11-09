package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.StyledBehavior;

import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.validation.validator.StringValidator;

/** Application-owned compound input: nested controls retain their own accessible labels. */
public class ReferenceInput extends FormComponentPanel<String> {

    public ReferenceInput(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        String[] parts = getModelObject().split("-", 2);
        add(code(parts[0]), revision(parts.length > 1 ? parts[1] : ""));
    }

    private TextField<String> code(String value) {
        TextField<String> code = new TextField<>("code", Model.of(value));
        code.setLabel(Model.of("Project code"));
        code.setRequired(true);
        code.add(StringValidator.minimumLength(3), new StyledBehavior("sw-input"));
        return code;
    }

    private TextField<String> revision(String value) {
        TextField<String> revision = new TextField<>("revision", Model.of(value));
        revision.setLabel(Model.of("Revision"));
        revision.add(new StyledBehavior("sw-input"));
        return revision;
    }

    @Override
    public void convertInput() {
        String code = ((FormComponent<?>) get("code")).getConvertedInput().toString();
        Object revision = ((FormComponent<?>) get("revision")).getConvertedInput();
        setConvertedInput(
                code + (revision == null || revision.toString().isBlank() ? "" : "-" + revision));
    }
}
