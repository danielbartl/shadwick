package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.RangeTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Objects;
import java.math.BigDecimal;
import org.apache.wicket.validation.ValidationError;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="range">}
 * ({@link RangeTextField}), with fluent min/max bounds and server-validated step alignment.
 *
 * Defaults to 0..100 with step 1. Initialize the model inside the configured range;
 * otherwise the browser chooses its own midpoint. Step must be positive.
 *
 * @param <N> the numeric model type
 */
public class SwRangeField<N extends Number & Comparable<N>> extends BaseSwField<N, RangeTextField<N>, SwRangeField<N>> {

    private IModel<N> minimumModel;
    private IModel<N> maximumModel;
    private IModel<N> stepModel;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwRangeField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwRangeField(String id, IModel<N> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwRangeField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwRangeField(String id, String label, IModel<N> model) {
        this(id, Model.of(label), model, null);
    }

    public SwRangeField(String id, IModel<String> label, IModel<N> model) {
        this(id, label, model, null);
    }

    public SwRangeField(String id, IModel<String> label, IModel<N> model, IModel<String> description) {
        super(id, label, model, description);
        addValidator(validatable -> {
            BigDecimal value;
            try { value = decimal(validatable.getValue()); }
            catch (IllegalArgumentException invalid) {
                validatable.error(new ValidationError().addKey("SwRangeField.invalid"));
                return;
            }
            BigDecimal min = bound(value(minimumModel), "0");
            BigDecimal max = bound(value(maximumModel), "100");
            BigDecimal step = bound(value(stepModel), "1");
            checkConstraints(min, max, step);
            if (value.compareTo(min) < 0 || value.compareTo(max) > 0 || value.subtract(min).remainder(step).signum() != 0)
                validatable.error(new ValidationError().addKey("SwRangeField.invalid"));
        });
    }

    public SwRangeField<N> setMin(N min) {
        getInput().setMinimum(min);
        return this;
    }

    public SwRangeField<N> setMax(N max) {
        getInput().setMaximum(max);
        return this;
    }

    /** Positive step, checked on the server relative to the minimum (zero by default). */
    public SwRangeField<N> setStep(N step) {
        if (step != null && decimal(step).signum() <= 0) throw new IllegalArgumentException("Step must be positive");
        getInput().setStep(step);
        return this;
    }

    public SwRangeField<N> setMin(IModel<N> min) {
        getInput().setMinimum(Objects.requireNonNull(min));
        return this;
    }

    public SwRangeField<N> setMax(IModel<N> max) {
        getInput().setMaximum(Objects.requireNonNull(max));
        return this;
    }

    public SwRangeField<N> setStep(IModel<N> step) {
        getInput().setStep(Objects.requireNonNull(step));
        return this;
    }

    private static Number value(IModel<? extends Number> model) { return model == null ? null : model.getObject(); }

    private static BigDecimal decimal(Number value) {
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Range values must be finite", ex); }
    }

    private static BigDecimal bound(Number value, String fallback) {
        return value == null ? new BigDecimal(fallback) : decimal(value);
    }

    private static void checkConstraints(BigDecimal min, BigDecimal max, BigDecimal step) {
        if (min.compareTo(max) > 0 || step.signum() <= 0)
            throw new IllegalArgumentException("Range requires min <= max and positive step");
    }

    @Override
    protected void onDetach() {
        // RangeTextField 10.11 does not detach its constraint models.
        if (minimumModel != null) minimumModel.detach();
        if (maximumModel != null) maximumModel.detach();
        if (stepModel != null) stepModel.detach();
        super.onDetach();
    }

    @Override
    protected RangeTextField<N> newInput(String id, IModel<N> model) {
        return new RangeTextField<>(id, model) {
            @Override public RangeTextField<N> setMinimum(N value) { return setMinimum(Model.of(value)); }
            @Override public RangeTextField<N> setMaximum(N value) { return setMaximum(Model.of(value)); }
            @Override public RangeTextField<N> setStep(N value) { return setStep(Model.of(value)); }
            @Override public RangeTextField<N> setMinimum(IModel<N> value) { minimumModel = Objects.requireNonNull(value); super.setMinimum(value); return this; }
            @Override public RangeTextField<N> setMaximum(IModel<N> value) { maximumModel = Objects.requireNonNull(value); super.setMaximum(value); return this; }
            @Override public RangeTextField<N> setStep(IModel<N> value) { stepModel = Objects.requireNonNull(value); super.setStep(value); return this; }
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("type", "range");
                checkConstraints(bound(value(minimumModel), "0"), bound(value(maximumModel), "100"), bound(value(stepModel), "1"));
                super.onComponentTag(tag);
                if (value(minimumModel) == null) tag.put("min", "0");
                if (value(maximumModel) == null) tag.put("max", "100");
                if (value(stepModel) == null) tag.put("step", "1");
            }
        };
    }
}
