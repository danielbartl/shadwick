package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SkipLinkBehavior;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AppShellTest {
    private WicketTester tester;
    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    private String html() { return tester.getLastResponseAsString(); }
    @Test void siteHeadersKeepCallerOwnedActionsAndSupportAnInsetContext() {
        SwSiteHeader header = new SwSiteHeader("header").setInset(true);
        WebMarkupContainer end = new WebMarkupContainer("end"); end.add(Sw.Behaviors.siteHeaderEnd());
        end.add(new Label("action", "Save")); header.add(end);
        tester.startComponentInPage(header, Markup.of("<header wicket:id='header'><div wicket:id='end'><span wicket:id='action'></span></div></header>"));
        assertThat(tester.getTagByWicketId("header").getName()).isEqualTo("header");
        assertThat(tester.getTagByWicketId("header").getAttribute("class")).contains("sw-site-header");
        assertThat(tester.getTagByWicketId("header").getAttribute("data-inset")).isEqualTo("true");
        tester.assertLabel("header:end:action", "Save");
        header.setInset(false); tester.startComponentInPage(header,
                Markup.of("<header wicket:id='header'><div wicket:id='end'><span wicket:id='action'></span></div></header>"));
        assertThat(tester.getTagByWicketId("header").getAttribute("data-inset")).isNull();
    }
    @Test void defaultSkipLinksAreNativeAndLabelsRemainEscaped() {
        SwSkipLink skip = new SwSkipLink("skip");
        tester.startComponentInPage(skip, Markup.of("<a wicket:id='skip'></a>"));
        assertThat(tester.getTagByWicketId("skip").getAttribute("href")).isEqualTo("#main");
        assertThat(html()).contains("Skip to main content").doesNotContain("shadwick.js");
        tester.startComponentInPage(new SwSkipLink("skip", "résultats:2026").setLabel(Model.of("<b>Results</b>")),
                Markup.of("<a wicket:id='skip'></a>"));
        assertThat(tester.getTagByWicketId("skip").getAttribute("href")).isEqualTo("#r%C3%A9sultats%3A2026");
        assertThat(html()).contains("&lt;b&gt;Results&lt;/b&gt;");
    }
    public static class SkipPage extends WebPage implements IMarkupResourceStreamProvider {
        final WebMarkupContainer main = new WebMarkupContainer("main");
        final SkipLinkBehavior behavior = Sw.Behaviors.skipLink(main);
        final WebMarkupContainer skip = new WebMarkupContainer("skip");
        public SkipPage() {
            main.setMarkupId("results:2026"); skip.add(behavior); add(skip, main);
            add(new SwSkipLink("buttonSkip", "button"));
            WebMarkupContainer control = new WebMarkupContainer("control"); control.setMarkupId("button");
            add(control, new SwSkipLink("nativeSkip", control));
            main.add(SwButton.onClick("refresh", "Refresh", target -> target.add(main)));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><a wicket:id='skip'>Skip</a><a wicket:id='buttonSkip'></a><a wicket:id='nativeSkip'></a>"
                    + "<button wicket:id='control'>Native</button><main wicket:id='main'><button wicket:id='refresh'></button></main></body></html>");
        }
    }
    @Test void componentTargetsGetAnIdAndFocusabilityThatSurvivesAjax() {
        SkipPage page = tester.startPage(SkipPage.class);
        assertThat(tester.getTagByWicketId("skip").getAttribute("href")).isEqualTo("#results%3A2026");
        assertThat(tester.getTagByWicketId("main").getAttribute("tabindex")).isEqualTo("-1");
        assertThat(tester.getTagByWicketId("control").getAttribute("tabindex")).isNull();
        tester.clickLink("main:refresh");
        tester.assertComponentOnAjaxResponse(page.main);
        assertThat(tester.getTagByWicketId("main").getAttribute("tabindex")).isEqualTo("-1");
    }
    @Test void explicitTabindexSurvivesAndUnbindingRemovesOnlyTheAddedBehavior() {
        SkipPage page = new SkipPage(); page.main.add(org.apache.wicket.AttributeModifier.replace("tabindex", "0"));
        tester.startPage(page);
        assertThat(tester.getTagByWicketId("main").getAttribute("tabindex")).isEqualTo("0");
        page.skip.remove(page.behavior); tester.startPage(page);
        assertThat(tester.getTagByWicketId("main").getAttribute("tabindex")).isEqualTo("0");
        assertThat(tester.getTagByWicketId("skip").getAttribute("href")).isNull();
    }
    @Test void callerOwnedBreadcrumbsPreserveAccessibleNames() {
        WebMarkupContainer nav = new WebMarkupContainer("nav"); nav.add(Sw.Behaviors.breadcrumb());
        tester.startComponentInPage(nav, Markup.of("<nav wicket:id='nav' aria-labelledby='title'></nav>"));
        assertThat(tester.getTagByWicketId("nav").getAttribute("aria-label")).isNull();
        assertThat(tester.getTagByWicketId("nav").getAttribute("aria-labelledby")).isEqualTo("title");
    }
    @Test void emptyTargetIdsAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SwSkipLink("skip", " "));
    }
}
