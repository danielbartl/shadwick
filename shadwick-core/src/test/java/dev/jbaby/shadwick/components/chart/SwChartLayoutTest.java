package dev.jbaby.shadwick.components.chart;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Category grid lines, tick margins and the plot margin. */
class SwChartLayoutTest {
    record Row(String name, Double value) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() {
        assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }
    SwChart<Row> chart() {
        return new SwChart<>("chart", Model.ofList(List.of(new Row("Jan", 4.0), new Row("Feb", 7.0), new Row("Mar", 5.0))), Row::name)
                .addSeries(ChartSeries.bar("Sales", Row::value));
    }
    long categoryLines() { return Pattern.compile("data-category-grid").matcher(html()).results().count(); }

    public static class ChartPage extends WebPage implements IMarkupResourceStreamProvider {
        public ChartPage(SwChart<?> chart) { add(chart); }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><head></head><body><div wicket:id='chart'></div></body></html>");
        }
    }

    @Test void categoryGridLinesAreOptInAndOnePerCategoryInBothOrientations() {
        tester.startComponentInPage(chart());
        assertThat(categoryLines()).isZero();
        tester.startComponentInPage(chart().setShowCategoryGrid(true));
        assertThat(categoryLines()).isEqualTo(3);
        // Through each category's centre, independent of the value grid
        assertThat(html()).contains("data-category-grid=\"\" x1=\"166.66667\"", "x1=\"500\"", "x1=\"833.33333\"");
        tester.startComponentInPage(chart().setShowCategoryGrid(true).setShowGrid(false));
        assertThat(categoryLines()).isEqualTo(3);
        tester.startComponentInPage(chart().setShowCategoryGrid(true).setOrientation(SwChart.Orientation.HORIZONTAL));
        assertThat(categoryLines()).isEqualTo(3);
    }

    @Test void marginsAreWholePixelsFromZeroTo64AndNullRestoresTheDefault() {
        var chart = chart();
        assertThatIllegalArgumentException().isThrownBy(() -> chart.setCategoryTickMargin(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> chart.setValueTickMargin(65));
        assertThatIllegalArgumentException().isThrownBy(() -> chart.setPlotMargin(100)).withMessageContaining("Plot margin");
        chart.setCategoryTickMargin(0).setValueTickMargin(64).setPlotMargin(12).setPlotMargin(null)
                .setCategoryTickMargin(null).setValueTickMargin(null);
        tester.startComponentInPage(chart);
        assertThat(html()).doesNotContain("sw-chart-layout-");
    }

    @Test void marginsBecomeAContentAddressedClassWithANonceProtectedRule() {
        String layout = ChartAppearance.layoutClass(4, 8, 12);
        assertThat(layout).matches("sw-chart-layout-[a-f0-9]{24}").isEqualTo(ChartAppearance.layoutClass(4, 8, 12))
                .isNotEqualTo(ChartAppearance.layoutClass(8, 4, 12));
        assertThat(ChartAppearance.layoutClass(null, null, null)).isEmpty();
        assertThat(ChartAppearance.layoutCss(4, null, 12)).isEqualTo(".sw-chart ." + ChartAppearance.layoutClass(4, null, 12)
                + "{--sw-chart-category-tick-margin:4px;--sw-chart-plot-margin:12px;}");

        tester.startPage(new ChartPage(chart().setCategoryTickMargin(4).setValueTickMargin(8).setPlotMargin(12)));
        assertThat(html()).contains("class=\"sw-chart-container " + layout + "\"",
                "--sw-chart-category-tick-margin:4px;--sw-chart-value-tick-margin:8px;--sw-chart-plot-margin:12px;");
        // A header stylesheet, deduplicated by its class (Wicket adds the CSP nonce where CSP is on; the browser test checks)
        assertThat(html()).contains("<style type=\"text/css\" id=\"" + layout + "\">");
        tester.startPage(new ChartPage(chart().setOrientation(SwChart.Orientation.HORIZONTAL).setPlotMargin(12)));
        assertThat(html()).contains("class=\"sw-chart-container " + ChartAppearance.layoutClass(null, null, 12) + "\" data-orientation=\"horizontal\"");
        // Without settings, no rule and no extra class
        tester.startPage(new ChartPage(chart()));
        assertThat(html()).contains("class=\"sw-chart-container\"").doesNotContain("sw-chart-layout-");
    }
}
