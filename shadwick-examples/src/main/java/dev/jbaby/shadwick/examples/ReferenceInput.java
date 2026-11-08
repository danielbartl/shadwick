package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.validation.validator.StringValidator;

/** Application-owned compound input: nested controls retain their own accessible labels. */
public class ReferenceInput extends FormComponentPanel<String> {
    public ReferenceInput(String id, IModel<String> model) { super(id, model); }
    @Override protected void onInitialize() {
        super.onInitialize(); String[] parts = getModelObject().split("-", 2);
        add(new TextField<>("code", Model.of(parts[0])).setLabel(Model.of("Project code"))
                .setRequired(true).add(StringValidator.minimumLength(3)).add(new dev.jbaby.shadwick.behaviors.StyledBehavior("sw-input")));
        add(new TextField<>("revision", Model.of(parts.length > 1 ? parts[1] : ""))
                .setLabel(Model.of("Revision")).add(new dev.jbaby.shadwick.behaviors.StyledBehavior("sw-input")));
    }
    @Override public void convertInput() {
        String code = ((FormComponent<?>)get("code")).getConvertedInput().toString();
        Object revision = ((FormComponent<?>)get("revision")).getConvertedInput();
        setConvertedInput(code + (revision == null || revision.toString().isBlank() ? "" : "-" + revision));
    }
}
