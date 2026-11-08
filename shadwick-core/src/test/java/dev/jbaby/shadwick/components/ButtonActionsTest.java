package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.ButtonGroupBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.ListenerInvocationNotAllowedException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

class ButtonActionsTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); }
    @AfterEach void stop() { tester.destroy(); }

    public static class ActionsPage extends WebPage implements IMarkupResourceStreamProvider {
        final List<String> chosen = new ArrayList<>();
        final Model<String> label = Model.of("Save <quote>");
        final SwSplitButton split = new SwSplitButton("split", label, target -> chosen.add("save"))
                .setVariant(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL);
        final SwSplitButton.MenuAction draft = split.addAction("Draft <quote>", target -> chosen.add("draft"));
        final SwSplitButton.MenuAction discard = split.addAction("Discard", target -> chosen.add("discard"))
                .setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
        public ActionsPage() {
            add(split); add(SwButton.onClick("refresh", "Refresh", target -> target.add(split)));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='split'></div><button wicket:id='refresh'></button></body></html>");
        }
    }
    @Test void splitIsNamedConnectedEscapedAndUsesSeparateButtonBehaviors() {
        var p = tester.startPage(ActionsPage.class);
        var primary = tester.getTagByWicketId("primary"); var trigger = tester.getTagByWicketId("trigger");
        var menu = tester.getTagByWicketId("menu");
        assertThat(primary.getValue()).isEqualTo("Save &lt;quote&gt;");
        assertThat(primary.getAttribute("type")).isEqualTo("button");
        assertThat(trigger.getAttribute("type")).isEqualTo("button");
        assertThat(trigger.getAttribute("aria-label")).isEqualTo("More actions");
        assertThat(trigger.getAttribute("aria-haspopup")).isEqualTo("menu");
        assertThat(trigger.getAttribute("aria-controls")).isEqualTo(menu.getAttribute("id"));
        assertThat(trigger.getAttribute("popovertarget")).isEqualTo(menu.getAttribute("id"));
        assertThat(menu.getAttribute("aria-labelledby")).isEqualTo(trigger.getAttribute("id"));
        assertThat(menu.getAttribute("data-align")).isEqualTo("end");
        assertThat(primary.getAttribute("data-size")).isEqualTo("sm");
        assertThat(trigger.getAttribute("data-size")).isEqualTo("sm");
        assertThat(primary.getAttribute("data-variant")).isEqualTo("outline");
        assertThat(trigger.getAttribute("data-variant")).isEqualTo("outline");
        assertThat(p.split.getPrimaryButton().getBehaviors(ButtonBehavior.class).get(0))
                .isNotSameAs(p.split.getTrigger().getBehaviors(ButtonBehavior.class).get(0));
        assertThat(tester.getLastResponseAsString()).contains("Draft &lt;quote&gt;", "data-variant=\"destructive\"")
                .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }
    @Test void callbacksAreDistinctAndMenuActionClosesTheMenu() {
        var p = tester.startPage(ActionsPage.class);
        tester.executeAjaxEvent("split:primary", "click");
        tester.executeAjaxEvent("split:menu:items:0", "click");
        assertThat(p.chosen).containsExactly("save", "draft");
        assertThat(tester.getLastResponseAsString()).contains("Shadwick.closePopover(\"" + p.split.getMenu().getMarkupId());
    }
    @Test void actionAvailabilityIsRecheckedEvenBeforeARerender() {
        var p = tester.startPage(ActionsPage.class);
        p.draft.setEnabled(false); p.discard.setVisible(false);
        tester.executeAjaxEvent("split:menu:items:0", "click");
        tester.executeAjaxEvent("split:menu:items:1", "click");
        assertThat(p.chosen).isEmpty();
        tester.clickLink("refresh");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNotNull();
        assertThat(tester.getTagByWicketId("items").getAttribute("disabled")).isNotNull();
        assertThat(tester.getLastResponseAsString()).doesNotContain(">Discard<");
        p.draft.setEnabled(true); tester.clickLink("refresh");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNull();
    }
    @Test void disabledPanelRejectsPrimaryAndMenuCallbacks() {
        var p = tester.startPage(ActionsPage.class); p.split.setEnabled(false); tester.clickLink("refresh");
        assertThat(tester.getTagByWicketId("primary").getAttribute("disabled")).isNotNull();
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNotNull();
        assertThatThrownBy(() -> tester.executeBehavior(p.split.getPrimaryButton().getBehaviors(AjaxEventBehavior.class).get(0)))
                .isInstanceOf(ListenerInvocationNotAllowedException.class);
        assertThatThrownBy(() -> tester.executeBehavior(p.split.get("menu:items:0").getBehaviors(AjaxEventBehavior.class).get(0)))
                .isInstanceOf(ListenerInvocationNotAllowedException.class);
        assertThat(p.chosen).isEmpty();
    }
    @Test void emptyMenuKeepsPrimaryUsableAndCanGainActionsDuringAjaxReplacement() {
        var split = new SwSplitButton("split", "Save", target -> {});
        tester.startComponentInPage(split);
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNotNull();
        assertThat(tester.getTagByWicketId("primary").getAttribute("disabled")).isNull();
        split.addAction("New", target -> {}); tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNull();
    }
    @ParameterizedTest @CsvSource({"en-US,More actions", "de-AT,Weitere Aktionen", "es,Más acciones", "fr,Plus d’actions",
            "it,Altre azioni", "ja,その他の操作", "nl,Meer acties", "pt-BR,Mais ações", "hr,Više radnji", "fi,More actions"})
    void triggerUsesLocalizedResources(String language, String expected) {
        tester.getSession().setLocale(Locale.forLanguageTag(language)); tester.startPage(ActionsPage.class);
        assertThat(tester.getTagByWicketId("trigger").getAttribute("aria-label")).isEqualTo(expected);
    }
    @Test void detachedHiddenActionModelsReloadWithCurrentValues() {
        int[] detached = {0};
        IModel<String> label = new IModel<>() {
            @Override public String getObject() { return "Hidden"; }
            @Override public void detach() { detached[0]++; }
        };
        var split = new SwSplitButton("split", "Save", target -> {});
        split.addAction(label, target -> {}).setVisible(false); tester.startComponentInPage(split);
        assertThat(detached[0]).isPositive();
    }
    @Test void serializationRetainsActionConfigurationAndCallbacks() {
        var p = tester.startPage(ActionsPage.class); p.draft.setEnabled(false);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (ActionsPage) serializer.deserialize(serializer.serialize(p));
        tester.startPage(restored); tester.executeAjaxEvent("split:menu:items:1", "click");
        assertThat(restored.chosen).containsExactly("discard");
        restored.label.setObject("Publish"); tester.clickLink("refresh");
        assertThat(tester.getTagByWicketId("primary").getValue()).isEqualTo("Publish");
        assertThat(tester.getTagByWicketId("items").getAttribute("disabled")).isNotNull();
    }
    @Test void repeatedGroupAcceptsNativeMarkupAndCallerOwnedButtons() {
        var group = Sw.Components.buttonGroup("group", Model.ofList(List.of("First", "Hidden", "Last")), (item, value) -> {
            item.setVisible(!value.equals("Hidden"));
            item.add(SwButton.onClick("button", value, target -> {}).setVariant(ButtonBehavior.Variant.OUTLINE));
        }).setLabel("Record actions").setOrientation(ButtonGroupBehavior.Orientation.VERTICAL);
        tester.startComponentInPage(group, Markup.of("<menu wicket:id='group'><li wicket:id='items'><button wicket:id='button'></button></li></menu>"));
        assertThat(tester.getTagByWicketId("group").getAttribute("role")).isEqualTo("group");
        assertThat(tester.getTagByWicketId("group").getAttribute("aria-label")).isEqualTo("Record actions");
        assertThat(tester.getTagByWicketId("group").getAttribute("data-orientation")).isEqualTo("vertical");
        assertThat(tester.getTagByWicketId("items").getAttribute("role")).isEqualTo("presentation");
        assertThat(tester.getLastResponseAsString()).contains("sw-button-group-item", ">First<", ">Last<").doesNotContain(">Hidden<");
    }
    @Test void behaviorPreservesCallerLabelsClassesAndNativeTabOrder() {
        var group = new WebMarkupContainer("group"); group.add(Sw.Behaviors.buttonGroup());
        tester.startComponentInPage(group, Markup.of("<div wicket:id='group' class='host' aria-label='Tools'><button type='button'>One</button><a href='/'>Two</a></div>"));
        var tag = tester.getTagByWicketId("group");
        assertThat(tag.getAttribute("class")).contains("host", "sw-button-group");
        assertThat(tag.getAttribute("aria-label")).isEqualTo("Tools");
        assertThat(tester.getLastResponseAsString()).doesNotContain("tabindex=");
    }

    @Test void groupListAndLabelModelsDetachAndRebuildChangedActions() {
        int[] detached = {0, 0}; var chosen = new ArrayList<String>();
        var values = new ListModel<String>(new ArrayList<>(List.of("One", "Two"))) {
            @Override public void detach() { detached[0]++; }
        };
        var label = new Model<>("Tools") { @Override public void detach() { detached[1]++; } };
        var group = SwButtonGroup.of("group", values, (item, value) ->
                item.add(SwButton.onClick("button", value, target -> chosen.add(value)))).setLabel(label);
        tester.startComponentInPage(group, Markup.of("<div wicket:id='group'><span wicket:id='items'><button wicket:id='button'></button></span></div>"));
        assertThat(detached[0]).isPositive(); assertThat(detached[1]).isPositive();
        values.setObject(new ArrayList<>(List.of("Three"))); label.setObject("New tools");
        tester.startPage(tester.getLastRenderedPage()); tester.executeAjaxEvent("group:items:0:button", "click");
        assertThat(chosen).containsExactly("Three");
    }
}
