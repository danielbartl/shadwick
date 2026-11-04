package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Sw;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class TooltipBehaviorTest {
    private WicketTester tester;
    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() { tester.destroy(); }

    @Test void aTooltipKeepsANativeFallbackAndEscapesItsPlainTextConfiguration() {
        Label label = new Label("hint", "Help");
        label.add(Sw.Behaviors.tooltip("Read <this> & \"that\""));
        tester.startComponentInPage(label);
        TagTester tag = tester.getTagByWicketId("hint");
        assertThat(tag.getAttribute("id")).isNotEmpty();
        assertThat(tag.getAttribute("title")).isEqualTo(tag.getAttribute("data-sw-tooltip"));
        assertThat(tag.getAttribute("data-tooltip-placement")).isEqualTo("top");
        assertThat(tag.getAttribute("data-tooltip-delay")).isEqualTo("300");
        assertThat(tag.getAttribute("data-tooltip-mode")).isEqualTo("description");
        assertThat(tester.getLastResponseAsString()).contains("&lt;this&gt;", "&amp;", "&quot;that&quot;")
                .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        // Client initialization appends a tooltip reference without replacing existing help/error references.
        assertThat(tag.getAttribute("aria-describedby")).isNull();
    }

    @ParameterizedTest @EnumSource(TooltipBehavior.Placement.class)
    void placementRetainsOatsTooltipApi(TooltipBehavior.Placement placement) {
        Label label = new Label("hint", "Help");
        label.add(new TooltipBehavior("Details").setPlacement(placement).setDelay(Duration.ZERO).setMode(TooltipBehavior.Mode.LABEL));
        tester.startComponentInPage(label);
        TagTester tag = tester.getTagByWicketId("hint");
        assertThat(tag.getAttribute("data-tooltip-placement")).isEqualTo(placement.name().toLowerCase(java.util.Locale.ROOT));
        assertThat(tag.getAttribute("data-tooltip-delay")).isEqualTo("0");
        assertThat(tag.getAttribute("data-tooltip-mode")).isEqualTo("label");
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"  "})
    void anAbsentLabelAddsNoTooltip(String text) {
        Label label = new Label("hint", "Help");
        label.add(new TooltipBehavior(text));
        tester.startComponentInPage(label);
        assertThat(tester.getTagByWicketId("hint").getAttribute("data-sw-tooltip")).isNull();
        assertThat(tester.getTagByWicketId("hint").getAttribute("title")).isNull();
    }

    @Test void labelsAreReactiveAndDetachAndDelayBoundsAreChecked() {
        int[] detached = {0};
        Model<String> text = new Model<>("Before") { @Override public void detach() { detached[0]++; } };
        Label label = new Label("hint", "Help");
        TooltipBehavior behavior = Sw.Behaviors.tooltip(text);
        label.add(behavior);
        tester.startComponentInPage(label);
        assertThat(detached[0]).isPositive();
        text.setObject("After");
        tester.startComponentInPage(label);
        assertThat(tester.getTagByWicketId("hint").getAttribute("title")).isEqualTo("After");
        assertThatIllegalArgumentException().isThrownBy(() -> behavior.setDelay(Duration.ofMillis(-1)));
        assertThatIllegalArgumentException().isThrownBy(() -> behavior.setDelay(Duration.ofDays(100)));
        assertThatNullPointerException().isThrownBy(() -> behavior.setPlacement(null));
        assertThatNullPointerException().isThrownBy(() -> behavior.setMode(null));
    }
}
