package dev.jbaby.shadwick.util;

import java.math.BigDecimal;

/** Finite HTML numeric attributes; machine formatting never follows the visitor's locale. */
public final class RangeValues {
    private RangeValues() {}
    public static double finite(Number number, String name) {
        double value = number.doubleValue();
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }
    public static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    public static String attribute(double value) { return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
}
