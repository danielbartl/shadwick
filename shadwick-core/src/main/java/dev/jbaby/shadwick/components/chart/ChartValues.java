package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.util.SerializableBiFunction;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/** Shared value formatting for Cartesian and part-to-whole charts. */
final class ChartValues {
    private ChartValues() {}
    static String format(Number value, Locale locale, SwChart.ValueFormat namedFormat, Currency currency,
                         SerializableBiFunction<Number, Locale, String> custom) {
        if (value == null) return "";
        if (custom != null) return Objects.toString(custom.apply(value, locale), "");
        NumberFormat f = switch (namedFormat) {
            case CURRENCY -> NumberFormat.getCurrencyInstance(locale);
            case PERCENT -> NumberFormat.getPercentInstance(locale);
            case COMPACT -> NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT);
            case INTEGER -> NumberFormat.getIntegerInstance(locale);
            default -> NumberFormat.getNumberInstance(locale);
        };
        if (namedFormat == SwChart.ValueFormat.CURRENCY) {
            if (currency == null) throw new IllegalStateException("Currency formatting requires setCurrency(Currency)");
            f.setCurrency(currency);
            f.setMinimumFractionDigits(currency.getDefaultFractionDigits());
            f.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        } else if (namedFormat != SwChart.ValueFormat.INTEGER) {
            f.setMaximumFractionDigits(namedFormat == SwChart.ValueFormat.COMPACT ? 1 : 2);
        }
        return f.format(value);
    }
}
