package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.util.SerializableBiFunction;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisit;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;

/** Labelled compound input, normally a caller-owned FormComponentPanel rendering on a div. */
public class SwCustomField<T> extends BaseSwField<T, FormComponent<T>, SwCustomField<T>> {
    private static final MetaDataKey<Boolean> WIRED = new MetaDataKey<>() {};
    private final SerializableBiFunction<String, IModel<T>, ? extends FormComponent<T>> factory;

    /** Model inherited from the parent; label looked up using the component ID. */
    public SwCustomField(String id, SerializableBiFunction<String, IModel<T>, ? extends FormComponent<T>> factory) {
        this(id, null, null, null, factory);
    }

    public SwCustomField(String id, String label, IModel<T> model,
                         SerializableBiFunction<String, IModel<T>, ? extends FormComponent<T>> factory) {
        this(id, Model.of(label), model, null, factory);
    }

    public SwCustomField(String id, IModel<String> label, IModel<T> model, IModel<String> description,
                         SerializableBiFunction<String, IModel<T>, ? extends FormComponent<T>> factory) {
        super(id, label, model, description);
        this.factory = Objects.requireNonNull(factory);
        get("container").add(new Behavior() {
            @Override
            public void onComponentTag(Component component, ComponentTag tag) {
                if (!getInput().isEnabledInHierarchy()) tag.put("disabled", "disabled");
            }
        });
    }

    @Override
    protected FormComponent<T> newInput(String id, IModel<T> model) {
        FormComponent<T> input = factory.apply(id, model);
        if (input == null || !id.equals(input.getId())) {
            throw new IllegalArgumentException("The input factory must return a component using id \"" + id + "\"");
        }
        return input;
    }

    @Override
    protected FeedbackMessage currentError() {
        FeedbackMessage own = super.currentError();
        if (own != null) return own;
        return getInput().visitChildren(FormComponent.class, (Component child, IVisit<FeedbackMessage> visit) -> {
            FeedbackMessage error = child.getFeedbackMessages().first(FeedbackMessage.ERROR);
            if (error != null) visit.stop(error);
        });
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        // Child onInitialize/onBeforeRender may have added or replaced controls. Wire after their preparation.
        getInput().visitChildren(FormComponent.class, (Component child, IVisit<Void> visit) -> {
            if (Boolean.TRUE.equals(child.getMetaData(WIRED))) return;
            child.setMetaData(WIRED, true);
            child.add(new Behavior() {
                @Override
                public void onComponentTag(Component component, ComponentTag tag) {
                    var ids = new LinkedHashSet<String>();
                    String existing = tag.getAttribute("aria-describedby");
                    if (existing != null) ids.addAll(Arrays.asList(existing.trim().split("\\s+")));
                    ids.addAll(Arrays.asList(getDescribedByMarkupIds().split("\\s+")));
                    ids.remove("");
                    tag.put("aria-describedby", String.join(" ", ids));
                    tag.put("aria-invalid", String.valueOf(SwCustomField.super.currentError() != null || component.hasErrorMessage()));
                }
            });
        });
    }
}
