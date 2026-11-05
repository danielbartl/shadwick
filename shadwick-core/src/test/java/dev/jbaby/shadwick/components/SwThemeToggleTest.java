package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.ShadwickSettings;
import dev.jbaby.shadwick.ThemeMode;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.settings.RequestCycleSettings;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class SwThemeToggleTest {
    WicketTester tester;
    @BeforeEach void before() {
        tester = new WicketTester(new MockApplication() {
            @Override protected void init() {
                super.init(); getCspSettings().blocking().strict(); Shadwick.install(this);
                getRequestCycleSettings().setRenderStrategy(RequestCycleSettings.RenderStrategy.ONE_PASS_RENDER);
            }
        });
    }
    @AfterEach void after() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }

    @Test void theMenuUsesNativeInvokersRadioItemsAndLocalizedNames() {
        var toggle = new SwThemeToggle("theme"); tester.startComponentInPage(toggle);
        TagTester trigger = tester.getTagByWicketId("trigger"), menu = tester.getTagByWicketId("menu");
        assertThat(trigger.getAttribute("type")).isEqualTo("button");
        assertThat(trigger.getAttribute("aria-label")).isEqualTo("Theme");
        assertThat(trigger.getAttribute("aria-haspopup")).isEqualTo("menu");
        assertThat(trigger.getAttribute("popovertarget")).isEqualTo(menu.getAttribute("id"));
        assertThat(menu.getAttribute("role")).isEqualTo("menu");
        assertThat(menu.getAttribute("data-align")).isEqualTo("end");
        assertThat(trigger.getAttribute("aria-pressed")).isNull();
        var items = TagTester.createTagsByAttribute(html(), "role", "menuitemradio", false);
        assertThat(items).extracting(item -> item.getAttribute("data-sw-theme-choice")).containsExactly("light", "dark", "system");
        assertThat(items).allSatisfy(item -> {
            assertThat(item.getAttribute("type")).isEqualTo("button");
            assertThat(item.getAttribute("aria-checked")).isEqualTo("false");
        });
        tester.assertLabel("theme:dropdown:menu:modes:2:label", "System");
    }

    @Test void aSingleButtonHasNoPopupAndNoInventedServerPressedState() {
        tester.startComponentInPage(new SwThemeToggle("theme").setPresentation(SwThemeToggle.Presentation.BUTTON));
        tester.assertInvisible("theme:dropdown");
        var button = tester.getTagByWicketId("button");
        assertThat(button.getAttribute("aria-label")).isEqualTo("Dark mode");
        assertThat(button.getAttribute("popovertarget")).isNull();
        assertThat(button.getAttribute("aria-pressed")).isNull();
    }

    @Test void customLabelsVariantsAndLogicalMenuPlacementAreSupported() {
        tester.startComponentInPage(new SwThemeToggle("theme").setLabel("Appearance <choice>")
                .setVariant(ButtonBehavior.Variant.GHOST).setSide(DropdownMenuBehavior.Side.START).setAlign(DropdownMenuBehavior.Align.CENTER));
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-label")).isEqualTo("Appearance <choice>");
        assertThat(html()).contains("Appearance &lt;choice&gt;");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("data-variant")).isEqualTo("ghost");
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-side")).isEqualTo("left");
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-align")).isEqualTo("center");
    }

    @Test void disablingTheComponentDisablesBothFormsOfControl() {
        var toggle = new SwThemeToggle("theme"); toggle.setEnabled(false); tester.startComponentInPage(toggle);
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNotNull();
        toggle.setPresentation(SwThemeToggle.Presentation.BUTTON); tester.startComponentInPage(toggle);
        assertThat(tester.getTagByWicketId("button").getAttribute("disabled")).isNotNull();
    }

    @Test void customLabelModelsReadOnceAndDetach() {
        AtomicInteger reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<String> label = new IModel<>() {
            @Override public String getObject() { reads.incrementAndGet(); return "Appearance"; }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
        tester.startComponentInPage(new SwThemeToggle("theme").setLabel(label));
        assertThat(reads.get()).isEqualTo(1); assertThat(detaches.get()).isPositive();
    }

    @Test void rootsUseSettingsAndRememberOnlyTheThreeDefinedModes() {
        assertThat(ShadwickSettings.get().getDefaultThemeMode()).isEqualTo(ThemeMode.SYSTEM);
        ShadwickSettings.get().setDefaultThemeMode(ThemeMode.DARK).setThemeStorageKey("app:appearance");
        var root = new WebMarkupContainer("root"); root.add(new ThemeBehavior());
        tester.startComponentInPage(root, Markup.of("<div wicket:id='root' class='workspace custom'></div>"));
        var tag = tester.getTagByWicketId("root");
        assertThat(tag.getAttribute("class")).isEqualTo("workspace custom dark");
        assertThat(tag.getAttribute("data-sw-theme-default")).isEqualTo("dark");
        assertThat(tag.getAttribute("data-sw-theme-storage-key")).isEqualTo("app:appearance");
        assertThat(tag.getAttribute("data-sw-theme-remember")).isEqualTo("true");
    }

    @Test void rootOverridesPreserveOtherClassesAndCanDisablePersistence() {
        var root = new WebMarkupContainer("root");
        root.add(new ThemeBehavior().setDefaultMode(ThemeMode.LIGHT).setStorageKey("preview-mode").setRememberChoice(false));
        tester.startComponentInPage(root, Markup.of("<div wicket:id='root' class='workspace dark'></div>"));
        var tag = tester.getTagByWicketId("root");
        assertThat(tag.getAttribute("class")).isEqualTo("workspace");
        assertThat(tag.getAttribute("data-sw-theme-default")).isEqualTo("light");
        assertThat(tag.getAttribute("data-sw-theme-storage-key")).isEqualTo("preview-mode");
        assertThat(tag.getAttribute("data-sw-theme-remember")).isEqualTo("false");
        assertThatIllegalArgumentException().isThrownBy(() -> new ThemeBehavior().setStorageKey("x\" onclick=\"y"));
        assertThatIllegalArgumentException().isThrownBy(() -> ShadwickSettings.get().setThemeStorageKey(""));
    }

    public static class ThemeTestPage extends WebPage implements IMarkupResourceStreamProvider {
        public ThemeTestPage() {
            var root = new WebMarkupContainer("html"); root.add(new ThemeBehavior()); add(root);
            root.add(new SwThemeToggle("one"), new SwThemeToggle("two").setPresentation(SwThemeToggle.Presentation.BUTTON));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html wicket:id='html'><head></head><body><div wicket:id='one'></div><div wicket:id='two'></div></body></html>");
        }
    }

    @Test void thePriorityExternalScriptRunsBeforeStylesAndIsDeduplicatedUnderStrictCsp() {
        ThemeTestPage page = tester.startPage(ThemeTestPage.class);
        assertThat(page.isPageStateless()).isTrue();
        assertThat(html()).containsOnlyOnce("js/theme");
        assertThat(html().indexOf("js/theme")).isLessThan(html().indexOf("css/shadwick"));
        assertThat(html()).doesNotContain("localStorage", "classList.toggle");
        assertThat(tester.getLastResponse().getHeader("Content-Security-Policy"))
                .contains("script-src 'strict-dynamic' 'nonce-").doesNotContain("unsafe-inline", "unsafe-eval");
    }

    @Test void aToggleCanTargetAnIndependentComponentRoot() {
        var target = new WebMarkupContainer("preview");
        tester.startComponentInPage(new SwThemeToggle("theme").setTarget(target));
        assertThat(target.getOutputMarkupId()).isTrue();
        assertThat(tester.getTagByWicketId("theme").getAttribute("data-sw-theme-target")).isEqualTo(target.getMarkupId());
    }
}
