package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.BigDecimalConverter;
import org.apache.wicket.validation.validator.RangeValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

/** Locale-aware decimal input with exact values, half-even rounding and server-side bounds. */
public abstract class BaseSwDecimalField<F extends BaseSwDecimalField<F>>
        extends BaseSwInputField<BigDecimal, TextField<BigDecimal>, F> {

    private BigDecimal min;
    private BigDecimal max;
    private RangeValidator<BigDecimal> range;

    public BaseSwDecimalField(String id, IModel<String> label, IModel<BigDecimal> model, IModel<String> description) {
        super(id, label, model, description);
    }

    protected abstract int getMinFractionDigits();
    protected abstract int getMaxFractionDigits();

    /** Optional unit removal before complete localized-number validation. */
    protected String stripInput(String input, Locale locale) {
        return input;
    }

    /** Inclusive minimum, or null to clear. Bounds apply to the rounded converted value. */
    public F setMin(BigDecimal value) {
        if (value != null && max != null && value.compareTo(max) > 0) {
            throw new IllegalArgumentException("Minimum exceeds maximum");
        }
        min = value;
        updateRange();
        return self();
    }

    /** Inclusive maximum, or null to clear. */
    public F setMax(BigDecimal value) {
        if (value != null && min != null && value.compareTo(min) < 0) {
            throw new IllegalArgumentException("Maximum precedes minimum");
        }
        max = value;
        updateRange();
        return self();
    }

    private void updateRange() {
        if (range != null) getInput().remove(range);
        range = min == null && max == null ? null : new RangeValidator<>(min, max);
        if (range != null) getInput().add(range);
    }

    @Override
    protected TextField<BigDecimal> newInput(String id, IModel<BigDecimal> model) {
        return new TextField<>(id, model, BigDecimal.class) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("type", "text");
                tag.put("inputmode", "decimal");
                super.onComponentTag(tag);
            }

            @Override
            protected IConverter<?> createConverter(Class<?> type) {
                // A fresh converter follows changing currency/precision models without stale format caches.
                return BigDecimal.class.isAssignableFrom(type)
                        ? new DecimalConverter(getMinFractionDigits(), getMaxFractionDigits())
                        : super.createConverter(type);
            }
        };
    }

    private final class DecimalConverter extends BigDecimalConverter {
        private final int minimum;
        private final int maximum;

        private DecimalConverter(int minimum, int maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }

        @Override
        protected NumberFormat newNumberFormat(Locale locale) {
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            format.setMinimumFractionDigits(minimum);
            format.setMaximumFractionDigits(maximum);
            format.setRoundingMode(RoundingMode.HALF_EVEN);
            return format;
        }

        @Override
        public BigDecimal convertToObject(String value, Locale locale) {
            if (value == null || trim(value).isEmpty()) return null;
            String text = trim(stripInput(value, locale));
            DecimalFormat format = (DecimalFormat) getNumberFormat(locale);
            char grouping = format.getDecimalFormatSymbols().getGroupingSeparator();
            if (Character.isSpaceChar(grouping)) {
                text = text.replace(' ', grouping).replace('\u00a0', grouping).replace('\u202f', grouping);
            }
            if (!wellFormed(text, format)) {
                throw newConversionException("Invalid localized decimal", value, locale);
            }
            BigDecimal number = super.convertToObject(text, locale);
            if (number.scale() > maximum) return number.setScale(maximum, RoundingMode.HALF_EVEN);
            return number.scale() < minimum ? number.setScale(minimum) : number;
        }
    }

    // DecimalFormat deliberately tolerates malformed groups; check their complete structure first.
    private static boolean wellFormed(String value, DecimalFormat format) {
        String text = value;
        String prefix = format.getNegativePrefix();
        String suffix = format.getNegativeSuffix();
        if (!prefix.isEmpty() && text.startsWith(prefix)) {
            text = text.substring(prefix.length());
            if (!suffix.isEmpty() && text.endsWith(suffix)) {
                text = text.substring(0, text.length() - suffix.length());
            }
        }
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        String[] parts = text.split(Pattern.quote(String.valueOf(symbols.getDecimalSeparator())), -1);
        if (parts.length > 2 || parts.length == 2 && !digits(parts[1])) return false;
        String[] groups = parts[0].split(Pattern.quote(String.valueOf(symbols.getGroupingSeparator())), -1);
        if (groups.length == 1) return digits(groups[0]) || groups[0].isEmpty() && parts.length == 2;
        int size = format.getGroupingSize();
        if (!format.isGroupingUsed() || !digits(groups[0]) || groups[0].length() > size) return false;
        for (int i = 1; i < groups.length; i++) {
            if (!digits(groups[i]) || groups[i].length() != size) return false;
        }
        return true;
    }

    private static boolean digits(String value) {
        return !value.isEmpty() && value.codePoints().allMatch(c -> Character.digit(c, 10) >= 0);
    }

    private static String trim(String value) {
        return value.replaceAll("^[\\s\\u00a0\\u202f]+|[\\s\\u00a0\\u202f]+$", "");
    }

    /** Removes one optional edge unit, preserving signs. Embedded or repeated units remain invalid. */
    protected final String stripUnit(String value, Locale locale, String... units) {
        String text = trim(value);
        String sign = "";
        String negative = ((DecimalFormat) NumberFormat.getNumberInstance(locale)).getNegativePrefix();
        if (!negative.isEmpty() && text.startsWith(negative)) {
            sign = negative;
            text = trim(text.substring(negative.length()));
        }
        for (String unit : Arrays.stream(units).filter(s -> s != null && !s.isEmpty()).distinct()
                .sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            if (text.startsWith(unit)) return sign + trim(text.substring(unit.length()));
            if (text.endsWith(unit)) return sign + trim(text.substring(0, text.length() - unit.length()));
        }
        return sign + text;
    }
}
