package dev.jbaby.shadwick.behaviors;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.Sw;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

class ToastBehaviorTest {
    private static JSONObject options(String script) {
        return new JSONObject(script.substring(script.indexOf(",{", script.indexOf("Shadwick.toast(")) + 1, script.lastIndexOf(");")));
    }

    @ParameterizedTest @EnumSource(ToastBehavior.Variant.class)
    void scriptsCarryTheVariantTitleAndDefaultDuration(ToastBehavior.Variant variant) {
        String script = ToastBehavior.script("Body", variant, "Title");
        JSONObject options = options(script);
        assertThat(script).startsWith("Shadwick.toast(\"Body\",").endsWith(");");
        assertThat(options.getString("variant")).isEqualTo(variant.name().toLowerCase(Locale.ROOT));
        assertThat(options.getString("title")).isEqualTo("Title");
        assertThat(options.getLong("duration")).isEqualTo(5000);
        assertThat(options.getString("dismissLabel")).isEqualTo("Dismiss notification");
        assertThat(options.getString("regionLabel")).isEqualTo("Notifications (Alt+T)");
    }
    @Test void nullVariantAndBlankTitleUseTheDefaultWithoutATitle() {
        JSONObject options = options(ToastBehavior.script("Body", null, "  ", Duration.ZERO));
        assertThat(options.getString("variant")).isEqualTo("default");
        assertThat(options.has("title")).isFalse();
        assertThat(options.getLong("duration")).isZero();
    }
    @Test void scriptArgumentsEscapeMarkupQuotesControlCharactersAndUnicodeSeparators() {
        String text = "</script><script>window.unsafe=true</script>\"\\\n\u2028\u2029 Zoë & 李";
        String script = ToastBehavior.script(text, ToastBehavior.Variant.ERROR, text);
        assertThat(script).doesNotContain("<", "\n", "\u2028", "\u2029")
                .contains("\\u003c", "\\u2028", "\\u2029", "Zoë & 李");
        assertThat(options(script).getString("title")).isEqualTo(text);
    }
    @Test void invalidDurationsAreRejectedRatherThanOverflowingBrowserTimers() {
        for (Duration duration : new Duration[]{Duration.ofMillis(-1), Duration.ofNanos(1),
                Duration.ofMillis(2147483648L), Duration.ofSeconds(Long.MAX_VALUE)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> ToastBehavior.script("Body", null, null, duration));
        }
        assertThatNullPointerException().isThrownBy(() -> ToastBehavior.script(null, null, null));
        assertThatNullPointerException().isThrownBy(() -> ToastBehavior.script("Body", null, null, null));
    }

    public static class ToastPage extends WebPage implements IMarkupResourceStreamProvider {
        public ToastPage() {
            add(Sw.Behaviors.toasts());
            add(new AjaxLink<Void>("notify") {
                @Override public void onClick(AjaxRequestTarget target) { Sw.toast(target, "Saved", ToastBehavior.Variant.SUCCESS, "Done", Duration.ofSeconds(8)); }
            });
            add(new AjaxLink<Void>("plain") {
                @Override public void onClick(AjaxRequestTarget target) { Sw.toast(target, "One"); Sw.toast(target, "Two", ToastBehavior.Variant.INFO); }
            });
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><head></head><body><a wicket:id='notify'>Notify</a><a wicket:id='plain'>Plain</a></body></html>");
        }
    }
    @Test void ajaxHelpersNeedNoFeedbackMessagesAndUseTheResourceBehavior() {
        WicketTester tester = new WicketTester();
        try {
            ToastPage page = tester.startPage(ToastPage.class);
            assertThat(tester.getLastResponseAsString()).contains("shadwick.js", "shadwick-vega");
            tester.clickLink("notify");
            String response = tester.getLastResponseAsString();
            assertThat(response).contains("Shadwick.toast(\"Saved\",")
                    .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
            assertThat(options(response.substring(response.indexOf("Shadwick.toast("), response.indexOf(");", response.indexOf("Shadwick.toast(")) + 2))
                    .getLong("duration")).isEqualTo(8000);
            assertThat(page.getFeedbackMessages().isEmpty()).isTrue();
            tester.clickLink("plain");
            assertThat(tester.getLastResponseAsString()).contains("Shadwick.toast(\"One\",", "Shadwick.toast(\"Two\",");
        } finally { tester.destroy(); }
    }
    public static class InitialPage extends ToastPage {
        @Override public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.render(OnDomReadyHeaderItem.forScript(ToastBehavior.script(this, "Full render", null, null, Duration.ZERO)));
        }
    }
    @Test void fullPageScriptsUseAnAttachedResourceBehaviorAndLocalizedControlLabels() {
        WicketTester tester = new WicketTester();
        try {
            tester.getApplication().getResourceSettings().getStringResourceLoaders().add(0,
                    new org.apache.wicket.resource.loader.ComponentStringResourceLoader() {
                        @Override public String loadStringResource(org.apache.wicket.Component component, String key, Locale locale, String style, String variation) {
                            return switch (key) { case "ToastBehavior.close" -> "Schließen"; case "ToastBehavior.label" -> "Mitteilungen (Alt+T)"; default -> null; };
                        }
                    });
            tester.startPage(InitialPage.class);
            assertThat(tester.getLastResponseAsString()).contains("shadwick.js", "Shadwick.toast(\"Full render\",", "Schließen", "Mitteilungen (Alt+T)")
                    .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
            tester.clickLink("notify");
            assertThat(tester.getLastResponseAsString()).contains("Schließen", "Mitteilungen (Alt+T)");
        } finally { tester.destroy(); }
    }
}
