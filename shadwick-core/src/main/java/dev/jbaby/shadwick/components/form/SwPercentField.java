package dev.jbaby.shadwick.components.form;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * A Shadwick-styled form field for a percentage, bound to a {@link BigDecimal} that holds
 * the number as shown: 19 means 19%. The number is shown and parsed in the user's
 * locale ({@code 7,5} in German, {@code 7.5} in English) with a {@code %} addon after
 * the input, and input is rounded half-even to {@link #setFractionDigits} decimals
 * (2 by default). Typing the {@code %} sign along with the number is fine.
 */
public class SwPercentField extends BaseSwDecimalField<SwPercentField> {

    private int fractionDigits = 2;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwPercentField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwPercentField(String id, IModel<BigDecimal> model) {
        this(id, null, model, null);
    }

    public SwPercentField(String id, String label, IModel<BigDecimal> model) {
        this(id, Model.of(label), model, null);
    }

    public SwPercentField(String id, IModel<String> label, IModel<BigDecimal> model) {
        this(id, label, model, null);
    }

    public SwPercentField(String id, IModel<String> label, IModel<BigDecimal> model, IModel<String> helper) {
        super(id, label, model, helper);
        setSuffix("%");
    }

    /** The most decimals a percentage is shown and stored with (2 by default). */
    public SwPercentField setFractionDigits(int fractionDigits) {
        if (fractionDigits < 0) {
            throw new IllegalArgumentException("fractionDigits must not be negative: " + fractionDigits);
        }
        this.fractionDigits = fractionDigits;
        return this;
    }

    @Override
    protected int getMinFractionDigits() {
        return 0;
    }

    @Override
    protected int getMaxFractionDigits() {
        return fractionDigits;
    }

    @Override
    protected String stripInput(String input, Locale locale) {
        return stripUnit(input, locale, String.valueOf(java.text.DecimalFormatSymbols.getInstance(locale).getPercent()), "%");
    }
}
