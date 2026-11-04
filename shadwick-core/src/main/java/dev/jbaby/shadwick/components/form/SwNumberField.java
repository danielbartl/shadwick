package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Objects;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="number">}
 * ({@link NumberTextField}), with fluent min/max bounds and a native step hint.
 *
 * @param <N> the numeric model type
 */
public class SwNumberField<N extends Number & Comparable<N>> extends BaseSwInputField<N, NumberTextField<N>, SwNumberField<N>> {

    private IModel<N> minimumModel;
    private IModel<N> maximumModel;
    private IModel<N> stepModel;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwNumberField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwNumberField(String id, IModel<N> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwNumberField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwNumberField(String id, String label, IModel<N> model) {
        this(id, Model.of(label), model, null);
    }

    public SwNumberField(String id, IModel<String> label, IModel<N> model) {
        this(id, label, model, null);
    }

    public SwNumberField(String id, IModel<String> label, IModel<N> model, IModel<String> description) {
        super(id, label, model, description);
    }

    public SwNumberField<N> setMin(N min) {
        minimumModel = null;
        getInput().setMinimum(min);
        return this;
    }

    public SwNumberField<N> setMax(N max) {
        maximumModel = null;
        getInput().setMaximum(max);
        return this;
    }

    /** HTML step hint. Wicket enforces min/max, but step alignment needs an application validator. */
    public SwNumberField<N> setStep(N step) {
        stepModel = null;
        getInput().setStep(step);
        return this;
    }

    public SwNumberField<N> setMin(IModel<N> min) {
        minimumModel = Objects.requireNonNull(min);
        getInput().setMinimum(min);
        return this;
    }

    public SwNumberField<N> setMax(IModel<N> max) {
        maximumModel = Objects.requireNonNull(max);
        getInput().setMaximum(max);
        return this;
    }

    public SwNumberField<N> setStep(IModel<N> step) {
        stepModel = Objects.requireNonNull(step);
        getInput().setStep(step);
        return this;
    }

    @Override
    protected void onDetach() {
        // NumberTextField 10.11 does not detach its constraint models.
        if (minimumModel != null) minimumModel.detach();
        if (maximumModel != null) maximumModel.detach();
        if (stepModel != null) stepModel.detach();
        super.onDetach();
    }

    @Override
    protected NumberTextField<N> newInput(String id, IModel<N> model) {
        return new NumberTextField<>(id, model) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("type", "number");
                super.onComponentTag(tag);
            }
        };
    }
}
