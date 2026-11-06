package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.*;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class ValueControlsTest {
    WicketTester tester;
    @BeforeEach void setup() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void cleanup() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }
    TagTester tag() { return tester.getTagByWicketId("value"); }
    void progress(SwProgress value) { tester.startComponentInPage(value, Markup.of("<progress wicket:id='value' aria-label='Task'></progress>")); }
    void meter(SwMeter value) { tester.startComponentInPage(value, Markup.of("<meter wicket:id='value' aria-label='Storage'></meter>")); }
    void ring(SwRadialProgress value) { tester.startComponentInPage(value, Markup.of("<div wicket:id='value' aria-label='Task'></div>")); }
    void rating(SwRating value) { tester.startComponentInPage(value, Markup.of("<span wicket:id='value'></span>")); }

    @ParameterizedTest @CsvSource({"-5,0", "37.5,37.5", "140,100"})
    void progressUsesNativeFiniteClampedValues(double current, String expected) {
        progress(new SwProgress("value", current));
        assertThat(tag().getName()).isEqualTo("progress"); assertThat(tag().getAttribute("value")).isEqualTo(expected);
        assertThat(tag().getAttribute("max")).isEqualTo("100");
    }
    @Test void nullProgressClearsAnAuthoredValueAndReevaluatesDynamicModels() {
        Model<Integer> value = Model.of(12), max = Model.of(24);
        SwProgress progress = new SwProgress("value", value, max).setLabel("Updated work");
        progress(progress); assertThat(tag().getAttribute("value")).isEqualTo("12");
        value.setObject(null);
        tester.startComponentInPage(progress, Markup.of("<progress wicket:id='value' value='99'></progress>"));
        assertThat(tag().getAttribute("value")).isNull(); assertThat(tag().getAttribute("aria-label")).isEqualTo("Updated work");
        value.setObject(48); max.setObject(60); progress(progress);
        assertThat(tag().getAttribute("max")).isEqualTo("60"); assertThat(tag().getAttribute("value")).isEqualTo("48");
    }
    @Test void nativeBehaviorsPreserveCallerValuesAndOtherAttributes() {
        var progress = new WebMarkupContainer("value").add(new ProgressBehavior());
        tester.startComponentInPage(progress, Markup.of("<progress wicket:id='value' value='3' max='8' aria-labelledby='task' class='host'></progress>"));
        assertThat(tag().getAttribute("max")).isEqualTo("8"); assertThat(tag().getAttribute("value")).isEqualTo("3");
        assertThat(tag().getAttribute("aria-labelledby")).isEqualTo("task"); assertThat(tag().getAttribute("class")).contains("host", "sw-progress");
        var meter = new WebMarkupContainer("value").add(new MeterBehavior());
        tester.startComponentInPage(meter, Markup.of("<meter wicket:id='value' min='10' max='50' value='30' low='20' high='40' optimum='15'></meter>"));
        assertThat(tag().getAttribute("value")).isEqualTo("30"); assertThat(tag().getAttribute("data-state")).isEqualTo("suboptimum");
    }
    @ParameterizedTest @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidProgressMaximumsFailWithoutWritingInvalidAttributes(double max) {
        assertThatThrownBy(() -> progress(new SwProgress("value", Model.of(1), Model.of(max))))
                .hasStackTraceContaining("Progress maximum");
    }
    @Test void nonfiniteReadingsAndWrongNativeTagsAreRejected() {
        assertThatThrownBy(() -> progress(new SwProgress("value", Double.NaN))).hasStackTraceContaining("must be finite");
        assertThatThrownBy(() -> meter(new SwMeter("value", Double.POSITIVE_INFINITY))).hasStackTraceContaining("must be finite");
        assertThatThrownBy(() -> tester.startComponentInPage(new SwProgress("value", 1), Markup.of("<div wicket:id='value'></div>"))).hasStackTraceContaining("requires <progress>");
        assertThatThrownBy(() -> tester.startComponentInPage(new SwMeter("value", 1), Markup.of("<div wicket:id='value'></div>"))).hasStackTraceContaining("requires <meter>");
    }
    @ParameterizedTest @CsvSource({"15,20,70,10,optimum", "40,20,70,10,suboptimum", "90,20,70,10,critical", "90,20,70,90,optimum", "40,20,70,90,suboptimum", "10,20,70,90,critical", "40,20,70,40,optimum", "10,20,70,40,suboptimum"})
    void meterQualityMatchesTheNativeThresholdBands(int value, int low, int high, int optimum, String state) {
        meter(new SwMeter("value", value).setMax(100).setLow(low).setHigh(high).setOptimum(optimum));
        assertThat(tag().getAttribute("data-state")).isEqualTo(state);
        assertThat(tag().getAttribute("low")).isEqualTo(String.valueOf(low)); assertThat(tag().getAttribute("optimum")).isEqualTo(String.valueOf(optimum));
    }
    @Test void meterUsesTheMinimumForNullAndClampsOutOfRangeValues() {
        meter(new SwMeter("value", (Number) null).setMin(10).setMax(20)); assertThat(tag().getAttribute("value")).isEqualTo("10");
        meter(new SwMeter("value", 40).setMin(10).setMax(20)); assertThat(tag().getAttribute("value")).isEqualTo("20");
        assertThatThrownBy(() -> meter(new SwMeter("value", 1).setMin(10).setMax(5))).hasStackTraceContaining("greater than");
        assertThatThrownBy(() -> meter(new SwMeter("value", .5).setLow(.8).setHigh(.2))).hasStackTraceContaining("thresholds");
    }
    @Test void radialProgressKeepsNativeBoundsAndAccessibleValuesConsistentWithItsSvg() {
        ring(new SwRadialProgress("value", Model.of(130), Model.of(100)));
        assertThat(tag().getAttribute("aria-valuenow")).isEqualTo("100"); assertThat(html()).contains("stroke-dasharray=\"100 100\"");
        ring(new SwRadialProgress("value", 29)); assertThat(tag().getAttribute("aria-valuetext")).isEqualTo("29%");
        ring(new SwRadialProgress("value", 0)); assertThat(html()).doesNotContain("stroke-dasharray=");
        ring(new SwRadialProgress("value", Model.of()));
        assertThat(tag().getAttribute("aria-valuenow")).isNull(); assertThat(tag().getAttribute("data-indeterminate")).isEqualTo("true");
    }
    @Test void radialModelsAreReadOnceAndCustomTextIsEscaped() {
        AtomicInteger reads = new AtomicInteger();
        IModel<Number> value = () -> { reads.incrementAndGet(); return 3; };
        ring(new SwRadialProgress("value", value, Model.of(5)).setText(Model.of("<3 & 5>")));
        assertThat(reads.get()).isEqualTo(1); assertThat(html()).contains("&lt;3 &amp; 5&gt;");
        assertThat(tag().getAttribute("aria-valuetext")).isEqualTo("<3 & 5>");
    }
    @ParameterizedTest @CsvSource({"4.3,4.5,4,1", "3,3,3,0", "8,5,5,0", "-1,0,0,0"})
    void ratingsClampAndRoundToHalfStars(double value, String shown, int full, int half) {
        rating(new SwRating("value", value));
        assertThat(tag().getAttribute("aria-label")).isEqualTo(shown + " out of 5 stars");
        assertThat(TagTester.createTagsByAttribute(html(), "data-fill", "full", false)).hasSize(full);
        assertThat(TagTester.createTagsByAttribute(html(), "data-fill", "half", false)).hasSize(half);
    }
    @Test void ratingsAreLocalizedAndUnknownIsDifferentFromZero() {
        tester.getSession().setLocale(Locale.forLanguageTag("hr-HR")); rating(new SwRating("value", 3.5));
        assertThat(tag().getAttribute("aria-label")).isEqualTo("3,5 od 5 zvjezdica");
        rating(new SwRating("value", Model.of())); assertThat(tag().getAttribute("aria-label")).isEqualTo("Još nije ocijenjeno");
        assertThatIllegalArgumentException().isThrownBy(() -> new SwRating("value", 1).setMax(11));
        assertThatThrownBy(() -> rating(new SwRating("value", Double.NaN))).hasStackTraceContaining("must be finite");
    }
    @Test void dynamicModelsAreDetachedAlongWithTheirStylingModels() {
        AtomicInteger count = new AtomicInteger();
        IModel<Number> value = new IModel<>() { public Number getObject() { return 25; } public void detach() { count.incrementAndGet(); } };
        progress(new SwProgress("value", value)); assertThat(count.get()).isPositive();
    }
}
