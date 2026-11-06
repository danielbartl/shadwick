package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.RangeValues;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.text.NumberFormat;

/** Read-only star rating, rounded to half a star; use SwRatingField for editable whole-star ratings. */
public class SwRating extends WebComponent {
    private final IModel<? extends Number> value;
    private int max = 5;
    private transient Integer halves;
    public SwRating(String id, Number value) { this(id, Model.of(value)); }
    public SwRating(String id, IModel<? extends Number> value) {
        super(id); this.value = value; add(new StyledBehavior("sw-rating")); setOutputMarkupId(true);
    }
    public SwRating setMax(int value) {
        if (value < 1 || value > 10) throw new IllegalArgumentException("A rating has 1 to 10 stars");
        max = value; return this;
    }
    public int getMax() { return max; }
    private int halves() {
        if (halves == null) {
            Number reading = value == null ? null : value.getObject();
            halves = reading == null ? -1 : (int) Math.round(RangeValues.clamp(RangeValues.finite(reading, "Rating"), 0, max) * 2);
        }
        return halves;
    }
    @Override protected void onConfigure() { super.onConfigure(); halves = null; }
    @Override protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag); if (tag.isOpenClose()) tag.setType(org.apache.wicket.markup.parser.XmlTag.TagType.OPEN);
        tag.put("role", "img");
        NumberFormat numbers = NumberFormat.getNumberInstance(getLocale()); numbers.setMaximumFractionDigits(1);
        tag.put("aria-label", halves() < 0 ? getString("SwRating.none") : new StringResourceModel("SwRating.label", this)
                .setParameters(numbers.format(halves() / 2.0), numbers.format(max)).getObject());
    }
    @Override public void onComponentTagBody(MarkupStream stream, ComponentTag tag) {
        String path = "M12 3 14.78 8.63 21 9.53 16.5 13.92 17.56 20.11 12 17.18 6.44 20.11 7.5 13.92 3 9.53 9.22 8.63Z";
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= max; i++) {
            String fill = halves() >= i * 2 ? "full" : halves() == i * 2 - 1 ? "half" : "empty";
            stars.append("<span data-fill=\"").append(fill).append("\" aria-hidden=\"true\"><svg viewBox=\"0 0 24 24\" focusable=\"false\"><path class=\"sw-rating-outline\" d=\"")
                    .append(path).append("\"/><path class=\"sw-rating-fill\" d=\"").append(path).append("\"/></svg></span>");
        }
        replaceComponentTagBody(stream, tag, stars);
    }
    @Override protected void onDetach() { if (value != null) value.detach(); halves = null; super.onDetach(); }
}
