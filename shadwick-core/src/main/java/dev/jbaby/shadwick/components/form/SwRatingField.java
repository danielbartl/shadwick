package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

import java.text.NumberFormat;
import java.util.List;
import java.util.stream.IntStream;

/** Whole-star rating on native radios, with the standard labelled fieldset and inline feedback. */
public class SwRatingField extends BaseSwField<Integer, RadioChoice<Integer>, SwRatingField> {
    private int max = 5;
    public SwRatingField(String id) { this(id, null, null, null); }
    public SwRatingField(String id, IModel<Integer> model) { this(id, null, model, null); }
    public SwRatingField(String id, String label, IModel<Integer> model) { this(id, Model.of(label), model, null); }
    public SwRatingField(String id, IModel<String> label, IModel<Integer> model) { this(id, label, model, null); }
    public SwRatingField(String id, IModel<String> label, IModel<Integer> model, IModel<String> description) {
        super(id, label, model, description); get("label").setOutputMarkupId(true);
    }
    public SwRatingField setMax(int value) {
        if (value < 1 || value > 10) throw new IllegalArgumentException("A rating has 1 to 10 stars");
        max = value; return this;
    }
    public int getMax() { return max; }
    @Override protected RadioChoice<Integer> newInput(String id, IModel<Integer> model) {
        var choice = new RadioChoice<Integer>(id, model, (IModel<List<Integer>>) () -> IntStream.rangeClosed(1, max).boxed().toList(), new StarRenderer()) {
            @Override protected Integer convertChoiceIdToChoice(String submitted) {
                Integer selected = super.convertChoiceIdToChoice(submitted);
                if (selected == null && submitted != null && !submitted.isBlank())
                    throw new org.apache.wicket.util.convert.ConversionException("Unknown rating").setResourceKey("SwRatingField.unavailable");
                return selected;
            }
            @Override protected IValueMap getAdditionalAttributes(int index, Integer option) {
                var attributes = new ValueMap(); attributes.put("class", "sw-rating-radio");
                attributes.put("aria-describedby", getDescribedByMarkupIds()); attributes.put("aria-invalid", hasErrorMessage() ? "true" : "false"); return attributes;
            }
            @Override protected IValueMap getAdditionalAttributesForLabel(int index, Integer option) {
                var attributes = new ValueMap(); attributes.put("class", "sw-rating-option"); return attributes;
            }
        };
        choice.setEscapeModelStrings(false); // Only our fixed SVG and escaped resource text are emitted by StarRenderer.
        choice.setLabelPosition(RadioChoice.LabelPosition.WRAP_AFTER); choice.setPrefix("").setSuffix("");
        choice.add(AttributeModifier.replace("aria-labelledby", (IModel<String>) () -> get("label").getMarkupId()));
        return choice;
    }
    private final class StarRenderer extends ChoiceRenderer<Integer> {
        @Override public Object getDisplayValue(Integer value) {
            String name = new StringResourceModel("SwRatingField.stars", SwRatingField.this)
                    .setParameters(NumberFormat.getIntegerInstance(getLocale()).format(value)).getObject();
            String path = "M12 3 14.78 8.63 21 9.53 16.5 13.92 17.56 20.11 12 17.18 6.44 20.11 7.5 13.92 3 9.53 9.22 8.63Z";
            return "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\" focusable=\"false\"><path class=\"sw-rating-outline\" d=\"" + path
                    + "\"/><path class=\"sw-rating-fill\" d=\"" + path + "\"/></svg><span class=\"sw-rating-text\">"
                    + org.apache.wicket.util.string.Strings.escapeMarkup(name) + "</span>";
        }
        @Override public String getIdValue(Integer value, int index) { return String.valueOf(value); }
    }
}
