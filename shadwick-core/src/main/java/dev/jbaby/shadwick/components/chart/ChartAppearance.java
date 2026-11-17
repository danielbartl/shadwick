package dev.jbaby.shadwick.components.chart;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.regex.Pattern;

/** Scoped, content-addressed appearance rules; never style attributes or executable CSS input. */
final class ChartAppearance {
    private static final Pattern COLOR = Pattern.compile(
            "#[0-9a-fA-F]{3,4}|#[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?|[a-zA-Z]+|var\\(--[a-zA-Z_][a-zA-Z0-9_-]*\\)"
            + "|(?:rgb|rgba|hsl|hsla|hwb|lab|lch|oklab|oklch|color)\\([a-zA-Z0-9+.,%/\\s-]+\\)", Pattern.CASE_INSENSITIVE);
    private ChartAppearance() {}
    static String color(String value) {
        String checked = Objects.requireNonNull(value).trim();
        if (checked.length() > 256 || !COLOR.matcher(checked).matches())
            throw new IllegalArgumentException("Use a CSS color literal or var(--property), without declarations, URLs or markup");
        return checked;
    }
    static String className(ChartSeries<?> series) {
        if (series.lightColor == null && series.strokeWidth == 2) return "";
        String signature = series.lightColor + "\n" + series.darkColor + "\n" + series.strokeWidth;
        try {
            return "sw-chart-custom-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(signature.getBytes(StandardCharsets.UTF_8)), 0, 12);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    /** A chart's own spacing, as a content-addressed class; empty when it keeps the defaults. */
    static String layoutClass(Integer categoryTickMargin, Integer valueTickMargin, Integer plotMargin) {
        if (categoryTickMargin == null && valueTickMargin == null && plotMargin == null) return "";
        return "sw-chart-layout-" + hash(categoryTickMargin + "\n" + valueTickMargin + "\n" + plotMargin);
    }
    /** Custom properties the structural chart CSS reads; whole pixel numbers only, so nothing else can get in. */
    static String layoutCss(Integer categoryTickMargin, Integer valueTickMargin, Integer plotMargin) {
        var css = new StringBuilder(".sw-chart ." + layoutClass(categoryTickMargin, valueTickMargin, plotMargin) + "{");
        if (categoryTickMargin != null) css.append("--sw-chart-category-tick-margin:").append(categoryTickMargin.intValue()).append("px;");
        if (valueTickMargin != null) css.append("--sw-chart-value-tick-margin:").append(valueTickMargin.intValue()).append("px;");
        if (plotMargin != null) css.append("--sw-chart-plot-margin:").append(plotMargin.intValue()).append("px;");
        return css.append('}').toString();
    }
    private static String hash(String signature) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(signature.getBytes(StandardCharsets.UTF_8)), 0, 12);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    static String css(ChartSeries<?> series) {
        String selector = ".sw-chart ." + className(series);
        var css = new StringBuilder();
        if (series.strokeWidth != 2)
            css.append(selector).append("{--sw-chart-stroke-width:").append(series.strokeWidth).append("px;}");
        if (series.lightColor != null) {
            // A syntactically safe but unsupported CSS color leaves the ordinary palette in place.
            css.append("@supports(color:").append(series.lightColor).append("){").append(selector)
                    .append("{--sw-series-color:").append(series.lightColor).append(";}}");
            css.append("@supports(color:").append(series.darkColor).append("){.dark ").append(selector)
                    .append(",.sw-chart.dark .").append(className(series)).append(",.sw-chart .dark .").append(className(series))
                    .append("{--sw-series-color:").append(series.darkColor).append(";}}");
        }
        return css.toString();
    }
}
