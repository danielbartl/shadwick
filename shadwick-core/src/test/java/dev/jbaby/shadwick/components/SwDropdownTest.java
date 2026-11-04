package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.core.request.handler.ListenerInvocationNotAllowedException;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SwDropdownTest {
    private WicketTester tester;
    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() { tester.destroy(); }
    private String html() { return tester.getLastResponseAsString(); }

    public static class MenuPage extends WebPage implements IMarkupResourceStreamProvider {
        final List<String> chosen = new ArrayList<>();
        final IModel<List<String>> choices = Model.ofList(new ArrayList<>(List.of("Edit <item>", "Export")));
        final SwDropdown<String> dropdown = SwDropdown.of("dropdown", "Actions", choices, value -> value,
                (target, value) -> chosen.add(value));
        public MenuPage() {
            add(dropdown);
            add(SwButton.onClick("refresh", "Refresh", target -> target.add(dropdown)));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='dropdown'></div><button wicket:id='refresh'></button></body></html>");
        }
    }

    @Test void nativeMenuAndTriggerAreNamedAndConnectedWithoutSubmittingAForm() {
        MenuPage page = tester.startPage(MenuPage.class);
        TagTester trigger = tester.getTagByWicketId("trigger"), menu = tester.getTagByWicketId("menu");
        assertThat(trigger.getAttribute("type")).isEqualTo("button");
        assertThat(trigger.getAttribute("aria-haspopup")).isEqualTo("menu");
        assertThat(trigger.getAttribute("aria-expanded")).isEqualTo("false");
        assertThat(trigger.getAttribute("popovertarget")).isEqualTo(menu.getAttribute("id"));
        assertThat(trigger.getAttribute("aria-controls")).isEqualTo(menu.getAttribute("id"));
        assertThat(menu.getAttribute("aria-labelledby")).isEqualTo(trigger.getAttribute("id"));
        assertThat(menu.getAttribute("role")).isEqualTo("menu");
        assertThat(menu.getAttribute("popover")).isEqualTo("auto");
        assertThat(menu.getAttribute("data-side")).isEqualTo("bottom");
        assertThat(menu.getAttribute("data-align")).isEqualTo("start");
        assertThat(page.dropdown.getOutputMarkupId()).isTrue();
        tester.assertLabel("dropdown:menu:items:0:label", "Edit &lt;item&gt;");
        assertThat(tester.getTagByWicketId("items").getAttribute("role")).isEqualTo("menuitem");
        assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }

    @Test void factoryActionsRunWithTheCurrentItemAndCloseTheBrowserMenu() {
        MenuPage page = tester.startPage(MenuPage.class);
        tester.executeAjaxEvent("dropdown:menu:items:1", "click");
        assertThat(page.chosen).containsExactly("Export");
        assertThat(html()).contains("Shadwick.closePopover(\"" + page.dropdown.getMenu().getMarkupId() + "\")");
    }

    @Test void customPopulationSupportsDisabledAndDestructiveItems() {
        var menu = SwDropdown.of("dropdown", Model.of("Actions"), Model.ofList(List.of("Delete", "Disabled")), (item, value) -> {
            item.add(new Label("label", value));
            if ("Disabled".equals(value)) item.setEnabled(false);
            else item.getBehaviors(DropdownMenuItemBehavior.class).get(0).setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
        });
        tester.startComponentInPage(menu);
        assertThat(TagTester.createTagsByAttribute(html(), "role", "menuitem", false)).hasSize(2);
        var items = TagTester.createTagsByAttribute(html(), "role", "menuitem", false);
        assertThat(items.get(0).getAttribute("data-variant")).isEqualTo("destructive");
        assertThat(items.get(1).getAttribute("disabled")).isNotNull();
        assertThat(items.get(1).getAttribute("aria-disabled")).isEqualTo("true");
    }

    @Test void aChangedListRebuildsItemsOnAjaxReplacement() {
        MenuPage page = tester.startPage(MenuPage.class);
        page.choices.setObject(new ArrayList<>(List.of("Archive")));
        tester.clickLink("refresh");
        tester.assertComponentOnAjaxResponse(page.dropdown);
        tester.assertLabel("dropdown:menu:items:0:label", "Archive");
        assertThat(TagTester.createTagsByAttribute(html(), "role", "menuitem", false)).hasSize(1);
        tester.executeAjaxEvent("dropdown:menu:items:0", "click");
        assertThat(page.chosen).containsExactly("Archive");
    }

    @Test void theServerRejectsAnActionDisabledSinceItWasRendered() {
        MenuPage page = tester.startPage(MenuPage.class);
        page.dropdown.get("menu:items:1").setEnabled(false);
        assertThatExceptionOfType(ListenerInvocationNotAllowedException.class)
                .isThrownBy(() -> tester.executeBehavior(page.dropdown.get("menu:items:1").getBehaviors(AjaxEventBehavior.class).get(0)));
        assertThat(page.chosen).isEmpty();
    }

    @Test void emptyActionModelsDisableTheTriggerAndCanBeRepopulated() {
        MenuPage page = tester.startPage(MenuPage.class);
        page.choices.setObject(new ArrayList<>());
        tester.clickLink("refresh");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNotNull();
        page.choices.setObject(new ArrayList<>(List.of("Available")));
        tester.clickLink("refresh");
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNull();
    }

    @ParameterizedTest @EnumSource(DropdownMenuBehavior.Side.class)
    void placementUsesLogicalInlineSides(DropdownMenuBehavior.Side side) {
        var menu = SwDropdown.of("dropdown", "Actions", Model.ofList(List.of("One")), x -> x, (target, value) -> {}).setSide(side);
        tester.startComponentInPage(menu);
        String expected = switch (side) { case START -> "left"; case END -> "right"; default -> side.name().toLowerCase(java.util.Locale.ROOT); };
        assertThat(tester.getTagByWicketId("menu").getAttribute("data-side")).isEqualTo(expected);
    }

    @Test void modelsDetachAndDisabledMenusDisableTheirTrigger() {
        int[] detached = {0, 0};
        IModel<List<String>> items = new IModel<>() {
            @Override public List<String> getObject() { return List.of("One"); }
            @Override public void detach() { detached[0]++; }
        };
        Model<String> label = new Model<>("Actions") { @Override public void detach() { detached[1]++; } };
        var menu = SwDropdown.of("dropdown", label, items, x -> x, (target, value) -> {});
        menu.setEnabled(false);
        tester.startComponentInPage(menu);
        assertThat(detached[0]).isPositive(); assertThat(detached[1]).isPositive();
        assertThat(tester.getTagByWicketId("trigger").getAttribute("disabled")).isNotNull();
    }

    public static class ComposedPage extends WebPage implements IMarkupResourceStreamProvider {
        final WebMarkupContainer trigger = new WebMarkupContainer("trigger");
        final WebMarkupContainer menu = new WebMarkupContainer("menu");
        final Model<Boolean> checked = Model.of(false);
        final DropdownMenuBehavior behavior = Sw.Behaviors.dropdownMenu(trigger).setAlign(DropdownMenuBehavior.Align.END);
        public ComposedPage() {
            add(trigger); menu.add(behavior); add(menu);
            menu.add(new Label("heading", "Display").add(Sw.Behaviors.dropdownMenuLabel()));
            menu.add(new WebMarkupContainer("separator").add(Sw.Behaviors.dropdownMenuSeparator()));
            menu.add(new ExternalLink("link", "https://example.org/", "Documentation").add(Sw.Behaviors.dropdownMenuItem()).setEnabled(false));
            WebMarkupContainer selected = new WebMarkupContainer("checked");
            selected.add(Sw.Behaviors.dropdownMenuCheckboxItem(checked));
            selected.add(AjaxEventBehavior.onEvent("click", target -> { checked.setObject(!checked.getObject()); target.add(selected); }));
            selected.setOutputMarkupId(true); menu.add(selected);
            menu.add(new WebMarkupContainer("radio").add(Sw.Behaviors.dropdownMenuRadioItem(Model.of(true))));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><button wicket:id='trigger' type='button'>View</button><div wicket:id='menu'>"
                    + "<div wicket:id='heading'></div><div wicket:id='separator'></div><a wicket:id='link'></a>"
                    + "<button wicket:id='checked' type='button'>Compact</button><button wicket:id='radio' type='button'>Name</button></div></body></html>");
        }
    }

    @Test void checkedItemsReadServerModelsAndDisabledLinksLoseTheirHref() {
        ComposedPage page = tester.startPage(ComposedPage.class);
        assertThat(tester.getTagByWicketId("link").getAttribute("href")).isNull();
        assertThat(tester.getTagByWicketId("link").getAttribute("aria-disabled")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("checked").getAttribute("role")).isEqualTo("menuitemcheckbox");
        assertThat(tester.getTagByWicketId("checked").getAttribute("aria-checked")).isEqualTo("false");
        assertThat(tester.getTagByWicketId("radio").getAttribute("role")).isEqualTo("menuitemradio");
        assertThat(tester.getTagByWicketId("radio").getAttribute("aria-checked")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("separator").getAttribute("role")).isEqualTo("separator");
        tester.executeAjaxEvent("menu:checked", "click");
        assertThat(page.checked.getObject()).isTrue();
        assertThat(tester.getTagByWicketId("checked").getAttribute("aria-checked")).isEqualTo("true");
    }

    @Test void removingTheMenuBehaviorDisconnectsOnlyItsOwnTriggerAttributes() {
        ComposedPage page = tester.startPage(ComposedPage.class);
        page.menu.remove(page.behavior);
        tester.startPage(page);
        assertThat(tester.getTagByWicketId("trigger").getAttribute("popovertarget")).isNull();
        assertThat(tester.getTagByWicketId("trigger").getAttribute("type")).isEqualTo("button");
    }
}
