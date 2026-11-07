package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import java.util.*;
import org.apache.wicket.markup.*;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

/** A decorative native SVG trend. Present its current value and meaning as nearby text. */
public class SwSparkline extends WebComponent {
    private int color = 1;
    private boolean filled, showLastPoint = true;

    public SwSparkline(String id, IModel<? extends List<? extends Number>> values) {
        super(id, Objects.requireNonNull(values));
        add(new StyledBehavior("sw-sparkline"));
        setOutputMarkupId(true);
    }
    /** Use chart token 1–5. */
    public SwSparkline setColor(int value) {
        if (value < 1 || value > 5) throw new IllegalArgumentException("color must be 1–5");
        color = value; return this;
    }
    public SwSparkline setFilled(boolean value) { filled = value; return this; }
    public SwSparkline setShowLastPoint(boolean value) { showLastPoint = value; return this; }

    @Override protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        tag.append("class", "sw-chart-series-" + color, " ");
        tag.put("aria-hidden", "true");
        if (tag.isOpenClose()) tag.setType(XmlTag.TagType.OPEN);
    }
    @Override @SuppressWarnings("unchecked")
    public void onComponentTagBody(MarkupStream stream, ComponentTag tag) {
        var input = (List<? extends Number>) getDefaultModelObject();
        replaceComponentTagBody(stream, tag, svg(input));
    }
    private String svg(List<? extends Number> input) {
        if (input == null || input.isEmpty()) return "";
        List<Double> values = new ArrayList<>();
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        int last = -1;
        for (Number number : input) {
            Double value = number == null ? null : number.doubleValue();
            if (value != null) {
                if (!Double.isFinite(value) || Math.abs(value) > 1e100)
                    throw new IllegalArgumentException("Sparkline values must be finite, with magnitude at most 1e100");
                min = Math.min(min, value); max = Math.max(max, value); last = values.size();
            }
            values.add(value);
        }
        if (last < 0) return "";
        StringBuilder b = new StringBuilder("<svg class=\"sw-sparkline-line-svg\" viewBox=\"0 0 100 32\" preserveAspectRatio=\"none\" aria-hidden=\"true\" focusable=\"false\">");
        List<Integer> isolated = new ArrayList<>();
        for (int start = 0; start < values.size();) {
            while (start < values.size() && values.get(start) == null) start++;
            if (start == values.size()) break;
            int end = start;
            while (end + 1 < values.size() && values.get(end + 1) != null) end++;
            if (end == start) isolated.add(start);
            else {
                StringBuilder points = new StringBuilder();
                for (int i = start; i <= end; i++) points.append(i == start ? "" : " ").append(num(x(i, values.size()))).append(',').append(num(y(values.get(i), min, max)));
                if (filled) b.append("<polygon class=\"sw-sparkline-area\" points=\"").append(num(x(start, values.size()))).append(",29 ")
                        .append(points).append(' ').append(num(x(end, values.size()))).append(",29\"/>");
                b.append("<polyline class=\"sw-sparkline-line\" points=\"").append(points).append("\"/>");
            }
            start = end + 1;
        }
        b.append("</svg><svg class=\"sw-sparkline-points-svg\" width=\"100%\" height=\"32\" aria-hidden=\"true\" focusable=\"false\">");
        for (int i : isolated) if (!showLastPoint || i != last)
            dot(b, i, values.size(), y(values.get(i), min, max), "sw-sparkline-point", 1.5);
        if (showLastPoint) dot(b, last, values.size(), y(values.get(last), min, max), "sw-sparkline-last", 3);
        return b.append("</svg>").toString();
    }
    private static double x(int i, int size) { return size == 1 ? 50 : i * 100.0 / (size - 1); }
    private static double y(double value, double min, double max) { return min == max ? 16 : 3 + (1 - (value - min) / (max - min)) * 26; }
    private static String num(double value) { return value == 0 ? "0" : Double.toString(value); }
    private static void dot(StringBuilder b, int i, int size, double y, String css, double radius) {
        b.append("<circle class=\"").append(css).append("\" cx=\"").append(num(x(i, size))).append("%\" cy=\"").append(num(y)).append("\" r=\"").append(num(radius)).append("\"/>");
    }
}
