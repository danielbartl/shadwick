package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.ThemeMode;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;
import java.util.Objects;

/**
 * Light/Dark/System preference menu, or a light/dark button. Uses the existing native menu and button styling.
 * Browser-owned state keeps pages stateless and all controls for the same theme root in sync.
 * Add ThemeBehavior to html for the remembered/system preference to apply before first paint.
 */
public class SwThemeToggle extends Panel {
    public enum Presentation { MENU, BUTTON }
    private Presentation presentation = Presentation.MENU;
    private final WebMarkupContainer trigger;
    private final WebMarkupContainer button;
    private final WebMarkupContainer menu;
    private final DropdownMenuBehavior appearance;
    private final ButtonBehavior triggerStyle = new ButtonBehavior(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.ICON);
    private final ButtonBehavior buttonStyle = new ButtonBehavior(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.ICON);
    private IModel<String> label;
    private Component target;

    public SwThemeToggle(String id) {
        super(id);
        setOutputMarkupId(true);
        add(AttributeModifier.append("class", "sw-theme-toggle"));
        add(AttributeModifier.replace("data-sw-theme-target", (IModel<String>) () -> target != null ? target.getMarkupId() : null));
        WebMarkupContainer dropdown = new WebMarkupContainer("dropdown") {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(presentation == Presentation.MENU); }
        };
        add(dropdown);
        trigger = control("trigger", false); trigger.add(triggerStyle); dropdown.add(trigger);
        button = control("button", true); button.add(buttonStyle); add(button);
        menu = new WebMarkupContainer("menu");
        appearance = new DropdownMenuBehavior(trigger).setAlign(DropdownMenuBehavior.Align.END);
        menu.add(appearance);
        menu.add(new ListView<>("modes", Model.ofList(List.of(ThemeMode.values()))) {
            @Override protected void populateItem(ListItem<ThemeMode> item) {
                ThemeMode mode = item.getModelObject();
                item.add(DropdownMenuItemBehavior.radio(Model.of(false)));
                item.add(AttributeModifier.replace("data-sw-theme-choice", mode.id()));
                item.add(new Label("label", new StringResourceModel("SwThemeToggle." + mode.id(), SwThemeToggle.this)));
                item.add(new SwIcon("icon", switch (mode) { case LIGHT -> "sun"; case DARK -> "moon"; case SYSTEM -> "monitor"; }));
            }
        });
        dropdown.add(menu);
    }

    private WebMarkupContainer control(String id, boolean single) {
        WebMarkupContainer control = new WebMarkupContainer(id) {
            @Override protected void onConfigure() {
                super.onConfigure();
                if (single) setVisible(presentation == Presentation.BUTTON);
            }
        };
        control.add(AttributeModifier.replace("disabled", (IModel<String>) () -> control.isEnabledInHierarchy() ? null : "disabled"));
        control.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> label != null ? label.getObject()
                : getString(single ? "SwThemeToggle.darkMode" : "SwThemeToggle.label")));
        control.add(new SwIcon("sun", "sun"), new SwIcon("moon", "moon"));
        return control;
    }

    public SwThemeToggle setPresentation(Presentation presentation) { this.presentation = Objects.requireNonNull(presentation); return this; }
    public SwThemeToggle setVariant(ButtonBehavior.Variant variant) { triggerStyle.setVariant(variant); buttonStyle.setVariant(variant); return this; }
    public SwThemeToggle setSide(DropdownMenuBehavior.Side side) { appearance.setSide(side); return this; }
    public SwThemeToggle setAlign(DropdownMenuBehavior.Align align) { appearance.setAlign(align); return this; }
    public SwThemeToggle setLabel(String label) { return setLabel(Model.of(label)); }
    public SwThemeToggle setLabel(IModel<String> label) { this.label = OncePerRequestModel.of(label); return this; }
    /** Targets an independent preview root; normally omitted for the document theme. */
    public SwThemeToggle setTarget(Component target) { this.target = Objects.requireNonNull(target); target.setOutputMarkupId(true); return this; }
    public WebMarkupContainer getTrigger() { return presentation == Presentation.BUTTON ? button : trigger; }
    public WebMarkupContainer getMenu() { return menu; }

    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderThemeScript(response);
    }
    @Override protected void onDetach() { super.onDetach(); if (label != null) label.detach(); }
}
