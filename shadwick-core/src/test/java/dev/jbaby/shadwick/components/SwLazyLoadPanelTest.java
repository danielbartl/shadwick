package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SkeletonBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class SwLazyLoadPanelTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    List<TagTester> tags(String attribute, String value) { return TagTester.createTagsByAttribute(tester.getLastResponseAsString(), attribute, value, false); }
    public static class Deferred extends SwLazyLoadPanel<Component> {
        boolean ready; int loads, completions;
        Duration interval = Duration.ofSeconds(1);
        Deferred(String id) { super(id); }
        @Override protected boolean isContentReady() { return ready; }
        @Override public Duration getUpdateInterval() { return interval; }
        @Override public Component getLazyLoadComponent(String id) { loads++; return new Label(id, "Report ready"); }
        @Override protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
            super.onContentLoaded(content, target); completions++;
        }
    }
    public static class LazyPage extends WebPage implements IMarkupResourceStreamProvider {
        Deferred first = new Deferred("first"), second = new Deferred("second");
        int replacements;
        public LazyPage() {
            add(first,second,SwButton.onClick("ready", "Ready", target -> { first.ready=true; second.ready=true; }));
            add(SwButton.onClick("replace", "Replace pending", target -> {
                first = new Deferred("first"); first.ready = true; first.setLoadingLabel("Replacement " + ++replacements);
                addOrReplace(first); target.add(first);
            }));
            add(SwButton.onClick("add", "Add panel", target -> {
                second = new Deferred("second"); second.ready = true; addOrReplace(second); target.add(second);
            }));
            add(SwButton.onClick("show", "Show", target -> { first.setVisible(true); target.add(first); }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='first'></div><div wicket:id='second'></div>"
                    + "<button wicket:id='ready'></button><button wicket:id='replace'></button><button wicket:id='add'></button><button wicket:id='show'></button></body></html>");
        }
    }
    @Test void initialRenderIsDeferredWithOneNamedStatusAndDecorativeSkeletons() {
        int[] loads={0};
        var panel=Sw.Components.lazyLoad("lazy", id -> {loads[0]++;return new Label(id,"Loaded content");});
        tester.startComponentInPage(panel);
        assertThat(loads[0]).isZero();
        assertThat(tester.getTagByWicketId("lazy").getAttribute("aria-busy")).isEqualTo("true");
        assertThat(tags("role","status")).hasSize(1);
        assertThat(tester.getTagByWicketId("status").getValue()).isEqualTo("Loading content…");
        assertThat(tags("data-shape","line")).hasSize(3).allMatch(t -> "true".equals(t.getAttribute("aria-hidden")));
        var page=tester.getLastRenderedPage();
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).hasSize(1);
        assertThat(page.getBehaviors(AbstractAjaxTimerBehavior.class).get(0).getUpdateInterval()).isEqualTo(Duration.ofMillis(100));
        assertThat(tester.getLastResponseAsString()).contains("<noscript>", "Load content").doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.executeAllTimerBehaviors(page);
        assertThat(loads[0]).isEqualTo(1); assertThat(panel.isLoaded()).isTrue();
        assertThat(tester.getTagByWicketId("lazy").getAttribute("aria-busy")).isEqualTo("false");
        assertThat(tester.getLastResponseAsString()).contains("Loaded content").doesNotContain("sw-skeleton");
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
    }
    @Test void multiplePanelsShareOneTimerAndUseTheShortestPendingInterval() {
        var page=tester.startPage(LazyPage.class); page.first.interval=Duration.ofMillis(750); page.second.interval=Duration.ofSeconds(2);
        var timer=page.getBehaviors(AbstractAjaxTimerBehavior.class).get(0);
        tester.executeAllTimerBehaviors(page); assertThat(timer.getUpdateInterval()).isEqualTo(Duration.ofMillis(750));
        assertThat(page.first.loads).isZero(); assertThat(page.second.loads).isZero();
        page.first.ready=true; tester.executeAllTimerBehaviors(page);
        assertThat(page.first.loads).isEqualTo(1); assertThat(page.first.completions).isEqualTo(1);
        assertThat(page.second.loads).isZero(); assertThat(timer.getUpdateInterval()).isEqualTo(Duration.ofSeconds(2));
        page.second.ready=true; tester.executeAllTimerBehaviors(page);
        assertThat(page.second.loads).isEqualTo(1); assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
    }
    @Test void pendingReplacementLoadsTheNewComponentAndAnAddedPanelRestartsAStoppedTimer() {
        var page=tester.startPage(LazyPage.class); var old=page.first;
        tester.clickLink("replace"); assertThat(page.first).isNotSameAs(old);
        tester.executeAllTimerBehaviors(page); assertThat(old.loads).isZero(); assertThat(page.first.loads).isEqualTo(1);
        page.second.ready=true; tester.executeAllTimerBehaviors(page);
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
        tester.clickLink("add"); assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).hasSize(1);
        assertThat(tester.getLastResponseAsString()).contains("Wicket.Timer");
        tester.executeAllTimerBehaviors(page); assertThat(page.second.loads).isEqualTo(1);
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
    }
    @Test void hiddenPanelsDoNotLoadAndCanInstallTheirTimerWhenShown() {
        var page=new LazyPage(); page.first.ready=true; page.first.setVisible(false); page.second.ready=true;
        tester.startPage(page); tester.executeAllTimerBehaviors(page);
        assertThat(page.first.loads).isZero(); assertThat(page.second.loads).isEqualTo(1);
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
        tester.clickLink("show"); assertThat(page.first.loads).isZero();
        tester.executeAllTimerBehaviors(page); assertThat(page.first.loads).isEqualTo(1);
    }
    @Test void nativeFallbackWaitsForReadinessAndLoadsWithoutAnAjaxTarget() {
        var page=tester.startPage(LazyPage.class);
        tester.clickLink("first:content:load"); assertThat(page.first.loads).isZero();
        page.first.ready=true; tester.clickLink("first:content:load");
        assertThat(page.first.loads).isEqualTo(1); assertThat(page.first.completions).isEqualTo(1);
        assertThat(tester.getTagByWicketId("first").getAttribute("aria-busy")).isEqualTo("false");
        assertThat(tester.getLastResponseAsString()).contains("Report ready");
    }
    @Test void placeholderShapesAndCountsAreValidatedAndRendered() {
        var panel=SwLazyLoadPanel.of("lazy", id->new Label(id,"Loaded")).setPlaceholder(SkeletonBehavior.Shape.BOX,1);
        tester.startComponentInPage(panel); assertThat(tags("data-shape","box")).hasSize(1);
        assertThatIllegalArgumentException().isThrownBy(()->panel.setPlaceholder(SkeletonBehavior.Shape.LINE,0));
        assertThatNullPointerException().isThrownBy(()->panel.setPlaceholder(null,1));
    }
    @Test void labelModelsLoadOnceAndDetachAfterLoadingAndWhenHidden() {
        int[] counts={0,0};
        IModel<String> label=new IModel<>() {
            @Override public String getObject(){counts[0]++;return "Loading <report>";}
            @Override public void detach(){counts[1]++;}
        };
        var panel=new Deferred("lazy"); panel.setLoadingLabel(label); tester.startComponentInPage(panel);
        assertThat(counts).containsExactly(1,1); assertThat(tester.getTagByWicketId("status").getValue()).isEqualTo("Loading &lt;report&gt;");
        panel.ready=true; tester.executeAllTimerBehaviors(tester.getLastRenderedPage());
        assertThat(counts[0]).isEqualTo(1); assertThat(counts[1]).isGreaterThan(1);
        int detached=counts[1];
        panel.setVisible(false); tester.startPage(tester.getLastRenderedPage());
        assertThat(counts[0]).isEqualTo(1); assertThat(counts[1]).isGreaterThan(detached);
    }
    @ParameterizedTest @CsvSource({"en-US,Loading content…,Load content","de-AT,Inhalt wird geladen…,Inhalt laden",
            "fr,Chargement du contenu…,Charger le contenu","es,Cargando contenido…,Cargar contenido","it,Caricamento del contenuto…,Carica contenuto",
            "ja,コンテンツを読み込み中…,コンテンツを読み込む","nl,Inhoud laden…,Inhoud laden","pt-BR,A carregar conteúdo…,Carregar conteúdo",
            "hr,Učitavanje sadržaja…,Učitaj sadržaj","fi,Loading content…,Load content"})
    void loadingAndNativeFallbackUseLocalizedResources(String language,String status,String link) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(SwLazyLoadPanel.of("lazy",id->new Label(id,"Loaded")));
        assertThat(tester.getTagByWicketId("status").getValue()).isEqualTo(status);
        assertThat(tester.getTagByWicketId("label").getValue()).isEqualTo(link);
    }
    @Test void pendingAndLoadedPagesKeepTheirStateAndCallbacksAfterSerialization() {
        var page=tester.startPage(LazyPage.class);var serializer=tester.getApplication().getFrameworkSettings().getSerializer();
        var restored=(LazyPage)serializer.deserialize(serializer.serialize(page));tester.startPage(restored);
        assertThat(restored.first.loads).isZero();tester.clickLink("ready");tester.executeAllTimerBehaviors(restored);
        assertThat(restored.first.loads).isEqualTo(1);assertThat(restored.second.loads).isEqualTo(1);
        var loaded=(LazyPage)serializer.deserialize(serializer.serialize(restored));tester.startPage(loaded);
        assertThat(loaded.first.loads).isEqualTo(1);assertThat(loaded.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
    }
    static Stream<Duration> intervals(){return Stream.of(null,Duration.ZERO,Duration.ofNanos(1),Duration.ofMillis(-1),Duration.ofDays(100),Duration.ofSeconds(Long.MAX_VALUE));}
    @ParameterizedTest @MethodSource("intervals") void invalidIntervalsFailBeforeInstallingATimer(Duration interval) {
        var panel=new Deferred("lazy");panel.interval=interval;
        assertThatThrownBy(()->tester.startComponentInPage(panel)).hasStackTraceContaining("update interval must be between");
    }
    @Test void factoryRejectsMissingContentAndWrongIds() {
        assertThatNullPointerException().isThrownBy(()->SwLazyLoadPanel.of("x",null));
        var wrong=SwLazyLoadPanel.of("lazy",id->new Label("wrong","Bad"));tester.startComponentInPage(wrong);
        assertThatThrownBy(()->tester.executeAllTimerBehaviors(tester.getLastRenderedPage())).hasStackTraceContaining("supplied id");
    }
    public static class MixedPage extends WebPage implements IMarkupResourceStreamProvider {
        final AjaxLazyLoadPanel<Component> standard=new AjaxLazyLoadPanel<>("standard") {
            @Override public Component getLazyLoadComponent(String id){return new Label(id,"Standard loaded");}
        };
        final SwLazyLoadPanel<Component> shadwick=SwLazyLoadPanel.of("shadwick",id->new Label(id,"Shadwick loaded"));
        public MixedPage(){add(standard,shadwick);}
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c,Class<?> t){return new StringResourceStream("<html><body><div wicket:id='standard'></div><div wicket:id='shadwick'></div></body></html>");}
    }
    @Test void anExistingStandardWicketTimerKeepsItsScheduleAndLoadsBothKindsOfPanel() {
        var page=tester.startPage(MixedPage.class);
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).hasSize(1);
        assertThat(page.getBehaviors(AbstractAjaxTimerBehavior.class).get(0).getUpdateInterval()).isEqualTo(Duration.ofSeconds(1));
        tester.executeAllTimerBehaviors(page);assertThat(tester.getLastResponseAsString()).contains("Standard loaded","Shadwick loaded");
        assertThat(page.getBehaviors(AjaxLazyLoadPanel.AjaxLazyLoadTimer.class)).isEmpty();
    }
}
