package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.TabsBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class SwTabsTest {
    static final String MARKUP = "<div wicket:id='tabs'><div wicket:id='tablist'><button wicket:id='tabButtons'><span wicket:id='tabLabel'></span></button></div><div wicket:id='tabPanels'><span wicket:id='panelContent'></span></div></div>";
    WicketTester tester;
    @BeforeEach void before() { tester = new WicketTester(new MockApplication() {
        @Override protected void init() { super.init(); Shadwick.install(this); getCspSettings().blocking().strict(); }
    }); }
    @AfterEach void after() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String role) { return TagTester.createTagsByAttribute(html(), "role", role, false); }
    void render(SwTabs<?> tabs) { tester.startComponentInPage(tabs, Markup.of(MARKUP)); }
    SwTabs<String> tabs() { return Sw.Components.tabs("tabs", Model.ofList(List.of("<Account>", "Password", "History")),
            (item, title) -> item.add(new Label("tabLabel", title)), (item, title) -> item.add(new Label("panelContent", title + " content"))); }
    @Test void serverSelectionWiresEveryTabAndPanelAndEscapesContent() {
        render(tabs().setSelectedTab(1).setLabel("Settings"));
        assertThat(tags("tab")).hasSize(3); assertThat(tags("tabpanel")).hasSize(3);
        for (int i = 0; i < 3; i++) {
            var tab = tags("tab").get(i); var panel = tags("tabpanel").get(i);
            assertThat(tab.getAttribute("aria-controls")).isEqualTo(panel.getAttribute("id"));
            assertThat(panel.getAttribute("aria-labelledby")).isEqualTo(tab.getAttribute("id"));
            assertThat(tab.getAttribute("aria-selected")).isEqualTo(String.valueOf(i == 1));
            assertThat(panel.getAttribute("hidden")).isEqualTo(i == 1 ? null : "hidden");
            assertThat(tab.getAttribute("type")).isEqualTo("button");
        }
        assertThat(tags("tablist").get(0).getAttribute("aria-label")).isEqualTo("Settings");
        tester.assertLabel("tabs:tablist:tabButtons:0:tabLabel", "&lt;Account&gt;");
        assertThat(html()).contains("&lt;Account&gt;");
    }
    @Test void verticalManualAndLineStylesAreServerConfiguration() {
        render(tabs().setOrientation(TabsBehavior.Orientation.VERTICAL).setActivation(TabsBehavior.Activation.MANUAL).setVariant(TabsBehavior.Variant.LINE));
        var root = tester.getTagByWicketId("tabs");
        assertThat(root.getAttribute("data-orientation")).isEqualTo("vertical");
        assertThat(root.getAttribute("data-activation")).isEqualTo("manual");
        assertThat(tags("tablist").get(0).getAttribute("aria-orientation")).isEqualTo("vertical");
        assertThat(tags("tablist").get(0).getAttribute("data-variant")).isEqualTo("line");
    }
    @Test void nativeUrlsDisabledAndHiddenTabsKeepPairedServerMarkupConsistent() {
        SwTabs<String> tabs = new SwTabs<>("tabs", Model.ofList(List.of("Account", "Password", "History"))) {
            @Override protected void populateTab(ListItem<String> item) { item.add(new Label("tabLabel", item.getModelObject())); }
            @Override protected void populatePanel(ListItem<String> item) { item.add(new Label("panelContent", item.getModelObject())); }
            @Override protected CharSequence tabUrl(String item, int index) { return "/settings?tab=" + index; }
            @Override protected boolean isTabEnabled(String item, int index) { return index != 1; }
            @Override protected boolean isTabVisible(String item, int index) { return index != 2; }
        };
        render(tabs.setSelectedTab(1));
        assertThat(tags("tab")).hasSize(2); assertThat(tags("tabpanel")).hasSize(2);
        assertThat(tags("tab").get(0).getName()).isEqualTo("a");
        assertThat(tags("tab").get(0).getAttribute("href")).isEqualTo("/settings?tab=0");
        assertThat(tags("tab").get(0).getAttribute("aria-selected")).isEqualTo("true");
        assertThat(tags("tab").get(1).getAttribute("href")).isNull();
        assertThat(tags("tab").get(1).getAttribute("aria-disabled")).isEqualTo("true");
        assertThat(html()).doesNotContain("History");
    }
    @Test void listModelsAreReadOnceForBothListsAndDetach() {
        AtomicInteger reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<String>> model = new IModel<>() {
            @Override public List<String> getObject() { reads.incrementAndGet(); return List.of("One", "Two"); }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
        render(SwTabs.of("tabs", model, (item, title) -> item.add(new Label("tabLabel", title)),
                (item, title) -> item.add(new Label("panelContent", title))));
        assertThat(reads.get()).isEqualTo(1); assertThat(detaches.get()).isPositive();
    }
    @Test void emptyModelsAndOutOfRangeSelectionHaveDefinedFallbacks() {
        render(tabs().setSelectedTab(99)); assertThat(tags("tab").get(0).getAttribute("aria-selected")).isEqualTo("true");
        render(SwTabs.of("tabs", Model.ofList(List.<String>of()), (item, title) -> {}, (item, title) -> {}));
        assertThat(tags("tab")).isEmpty(); assertThat(tags("tabpanel")).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> tabs().setSelectedTab(-1));
    }
}
