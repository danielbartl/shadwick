package dev.jbaby.shadwick.examples;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jbaby.shadwick.behaviors.ThemeBehavior;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

class BasePageLifecycleTest {
    @Test
    void themeHookCanUseSubclassStateAndSurvivesAjaxAndSerialization() {
        WicketTester tester = new WicketTester(new ExamplesApplication());
        try {
            tester.startPage(new ThemePage(new PageParameters()));
            assertThat(tester.getLastResponseAsString())
                    .contains("data-sw-theme-storage-key=\"shadwick-example-theme\"");

            var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
            var restored =
                    (ThemePage)
                            serializer.deserialize(
                                    serializer.serialize(tester.getLastRenderedPage()));
            tester.startPage(restored);
            tester.clickLink("refresh");
            tester.assertComponentOnAjaxResponse("primary");
            assertThat(restored.get("html").getBehaviors(ThemeBehavior.class)).hasSize(1);
            tester.assertNoErrorMessage();
        } finally {
            tester.destroy();
        }
    }
}
