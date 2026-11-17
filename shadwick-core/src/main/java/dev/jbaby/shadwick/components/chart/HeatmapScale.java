package dev.jbaby.shadwick.components.chart;

import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** The five shared intensity bands of the heatmaps: exact decimal bounds, levels and value checks. */
final class HeatmapScale {
    record Band(BigDecimal lower, BigDecimal upper, String text) implements Serializable {}

    private static final BigDecimal LIMIT = BigDecimal.ONE.scaleByPowerOfTen(100);

    private HeatmapScale() {}

    /** The value as an exact decimal, or null for a missing one; finite, with magnitude at most 1e100. */
    static BigDecimal amount(Number value) {
        if (value == null) return null;
        double number = value.doubleValue();
        if (!Double.isFinite(number) || Math.abs(number) > 1e100)
            throw new IllegalArgumentException("Heatmap values must be finite with magnitude at most 1e100");
        BigDecimal result;
        try { result = value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
        catch (NumberFormatException ignored) { result = BigDecimal.valueOf(number); }
        if (result.abs().compareTo(LIMIT) > 0)
            throw new IllegalArgumentException("Heatmap values must be finite with magnitude at most 1e100");
        return result;
    }

    /**
     * The scale's lower and upper bounds. Automatic bounds take the observed extremes and zero (0 to 1 when they are
     * equal); an explicit range must be defined and increasing, with every observation inside it.
     */
    static BigDecimal[] bounds(List<BigDecimal> observed, IModel<? extends Number> minimum, IModel<? extends Number> maximum) {
        if (minimum == null) {
            BigDecimal lower = BigDecimal.ZERO, upper = BigDecimal.ZERO;
            for (BigDecimal value : observed) { lower = lower.min(value); upper = upper.max(value); }
            if (lower.compareTo(upper) == 0) upper = BigDecimal.ONE;
            return new BigDecimal[] {lower, upper};
        }
        BigDecimal lower = amount(minimum.getObject()), upper = amount(maximum.getObject());
        if (lower == null || upper == null || lower.compareTo(upper) >= 0)
            throw new IllegalArgumentException("Heatmap range bounds must be defined and increasing");
        // Only observations, rather than the automatic zero anchor, constrain an explicit range.
        for (BigDecimal value : observed)
            if (value.compareTo(lower) < 0 || value.compareTo(upper) > 0)
                throw new IllegalArgumentException("Heatmap values must stay within the configured range");
        return new BigDecimal[] {lower, upper};
    }

    /** Five equal bands from lower to upper, each labelled with its formatted bounds. */
    static List<Band> bands(BigDecimal lower, BigDecimal upper, Function<BigDecimal, String> format) {
        BigDecimal step = upper.subtract(lower).divide(BigDecimal.valueOf(5));
        List<Band> bands = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BigDecimal start = lower.add(step.multiply(BigDecimal.valueOf(i))), end = start.add(step);
            bands.add(new Band(start, end, format.apply(start) + " – " + format.apply(end)));
        }
        return List.copyOf(bands);
    }

    /** The band of a value: lower-inclusive and upper-exclusive, except the last, which includes its maximum. */
    static int level(List<Band> bands, BigDecimal value) {
        for (int i = 0; i < 4; i++) if (value.compareTo(bands.get(i).upper()) < 0) return i;
        return 4;
    }
}
