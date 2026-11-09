package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.ThemeMode;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwThemeToggle;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 * The document preference, independent of the showcase's other pages' reference light/dark
 * controls.
 */
public class ThemePage extends BasePage {

    private final String storageKey;

    public ThemePage(PageParameters parameters) {
        super(parameters);
        storageKey = "shadwick-example-theme";
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        PageParameters parameters = getPageParameters();
        addThemeControls();
        add(
                new BookmarkablePageLink<Void>(
                        "next",
                        ThemePage.class,
                        new PageParameters(parameters)
                                .set(
                                        "view",
                                        parameters.get("view").toString("first").equals("first")
                                                ? "second"
                                                : "first")));
        add(new Label("view", parameters.get("view").toString("first")));
    }

    private void addThemeControls() {
        SwThemeToggle primary = new SwThemeToggle("primary").setLabel("Choose appearance");
        primary.setMarkupId("theme-primary");
        SwThemeToggle secondary =
                new SwThemeToggle("secondary")
                        .setLabel("Footer appearance")
                        .setVariant(ButtonBehavior.Variant.GHOST);
        secondary.setMarkupId("theme-secondary");
        SwThemeToggle button =
                new SwThemeToggle("button").setPresentation(SwThemeToggle.Presentation.BUTTON);
        button.setMarkupId("theme-button");
        add(primary, secondary, button);
        add(
                SwButton.onClick(
                                "refresh",
                                "Refresh theme controls",
                                target -> target.add(primary, secondary, button))
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
    }

    @Override
    protected ThemeBehavior newThemeBehavior(PageParameters parameters) {
        ThemeBehavior behavior = new ThemeBehavior().setStorageKey(storageKey);
        String mode = parameters.get("mode").toString("");
        if (mode.equals("dark")) behavior.setDefaultMode(ThemeMode.DARK);
        else if (mode.equals("light")) behavior.setDefaultMode(ThemeMode.LIGHT);
        return behavior;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(
                JavaScriptHeaderItem.forReference(
                        new JavaScriptResourceReference(ThemePage.class, "theme-demo.js")));
    }

    @Override
    protected String getTitle() {
        return "Theme & preferences";
    }
}
