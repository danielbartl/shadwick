package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.RangeValues;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;

import java.text.NumberFormat;
import java.util.Locale;

/** SVG progress ring with localized percentage text. Name it with aria-label/aria-labelledby or setLabel. */
public class SwRadialProgress extends WebComponent {
    public enum Variant { DEFAULT, SECONDARY, DESTRUCTIVE }
    private final IModel<? extends Number> value, max;
    private IModel<Variant> variant = Model.of(Variant.DEFAULT);
    private IModel<String> text, label;
    private transient Snapshot snapshot;
    private record Snapshot(Double value, double max, double percent, String text) {}
    public SwRadialProgress(String id, Number value) { this(id, Model.of(value)); }
    public SwRadialProgress(String id, IModel<? extends Number> value) { this(id, value, Model.of(100)); }
    public SwRadialProgress(String id, IModel<? extends Number> value, IModel<? extends Number> max) {
        super(id); this.value = value; this.max = max;
        add(new StyledBehavior("sw-radial-progress")); setOutputMarkupId(true);
    }
    public SwRadialProgress setVariant(Variant value) { return setVariant(Model.of(value)); }
    public SwRadialProgress setVariant(IModel<Variant> value) { variant = value; return this; }
    public SwRadialProgress setText(IModel<String> value) { text = value; return this; }
    public SwRadialProgress setLabel(String value) { return setLabel(Model.of(value)); }
    public SwRadialProgress setLabel(IModel<String> value) { label = value; return this; }
    private Snapshot snapshot() {
        if (snapshot != null) return snapshot;
        Number maximum = max == null ? null : max.getObject();
        double upper = maximum == null ? 100 : RangeValues.finite(maximum, "Progress maximum");
        if (upper <= 0) throw new IllegalArgumentException("Progress maximum must be positive");
        Number reading = value == null ? null : value.getObject();
        Double current = reading == null ? null : RangeValues.clamp(RangeValues.finite(reading, "Progress value"), 0, upper);
        double percent = current == null ? 0 : current / upper * 100;
        String shown = text != null ? text.getObject() : current == null ? null : NumberFormat.getPercentInstance(getLocale()).format(java.math.BigDecimal.valueOf(current)
                .multiply(java.math.BigDecimal.valueOf(100)).divide(java.math.BigDecimal.valueOf(upper), 0, java.math.RoundingMode.DOWN).movePointLeft(2));
        return snapshot = new Snapshot(current, upper, percent, shown);
    }
    @Override protected void onConfigure() { super.onConfigure(); snapshot = null; }
    @Override protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag); if (tag.isOpenClose()) tag.setType(org.apache.wicket.markup.parser.XmlTag.TagType.OPEN);
        Snapshot state = snapshot(); tag.put("role", "progressbar"); tag.put("aria-valuemin", "0");
        tag.put("aria-valuemax", RangeValues.attribute(state.max));
        if (state.value == null) { tag.remove("aria-valuenow"); tag.put("data-indeterminate", "true"); }
        else { tag.put("aria-valuenow", RangeValues.attribute(state.value)); tag.remove("data-indeterminate"); }
        if (state.text == null || state.text.isBlank()) tag.remove("aria-valuetext"); else tag.put("aria-valuetext", state.text);
        Variant appearance = variant == null ? null : variant.getObject();
        if (appearance == null || appearance == Variant.DEFAULT) tag.remove("data-variant");
        else tag.put("data-variant", appearance.name().toLowerCase(Locale.ROOT));
        if (label != null) { String name = label.getObject(); if (name != null) tag.put("aria-label", name); }
    }
    @Override public void onComponentTagBody(MarkupStream stream, ComponentTag tag) {
        Snapshot state = snapshot();
        String bar = state.value == null || state.percent == 0 ? "" : "<circle class=\"sw-radial-progress-value\" cx=\"32\" cy=\"32\" r=\"27\" pathLength=\"100\" stroke-dasharray=\"" + RangeValues.attribute(state.percent) + " 100\"/>";
        replaceComponentTagBody(stream, tag, "<svg viewBox=\"0 0 64 64\" aria-hidden=\"true\" focusable=\"false\"><circle class=\"sw-radial-progress-track\" cx=\"32\" cy=\"32\" r=\"27\"/>" + bar + "</svg><span class=\"sw-radial-progress-text\" aria-hidden=\"true\">" + Strings.escapeMarkup(state.text == null ? "" : state.text) + "</span>");
    }
    @Override protected void onDetach() {
        for (IModel<?> model : new IModel<?>[]{value, max, variant, text, label}) if (model != null) model.detach();
        snapshot = null; super.onDetach();
    }
}
