package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.ShadwickSettings;
import dev.jbaby.shadwick.Skin;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

/**
 * The showcase's layout: a header with the pages, a skin switcher and a light/dark switch. Both are page parameters
 * ({@code ?skin=lyra}, {@code ?mode=dark}), kept in the session once picked, so every page can be opened in any
 * skin and mode by its URL. Two windows of one browser share the session: compare skins side by side in two browsers
 * (or a private window).
 */
public abstract class BasePage extends WebPage {

    private static final CssResourceReference EXAMPLES_CSS = new CssResourceReference(BasePage.class, "examples.css");

    protected BasePage(PageParameters parameters) {
        super(parameters);
        pick(parameters);

        // class="dark" on <html>, rendered on the server, so the page never flashes light first
        TransparentWebMarkupContainer html = new TransparentWebMarkupContainer("html");
        html.add(AttributeModifier.replace("class", (IModel<String>) () -> Appearance.isDark() ? "dark" : null));
        add(html);
        add(new Label("title", (IModel<String>) () -> getTitle() + " · Shadwick"));

        add(navLink("navHome", HomePage.class));
        add(navLink("navButtons", ButtonPage.class));
        add(navLink("navForm", FormPage.class));
        add(navLink("navDialog", DialogPage.class));

        for (Skin skin : Skin.values()) {
            add(switchLink(skin.id(), "skin", skin.id(), () -> currentSkin() == skin));
        }
        add(switchLink("light", "mode", "light", () -> !Appearance.isDark()));
        add(switchLink("dark", "mode", "dark", Appearance::isDark));
    }

    /** The page's name, for its {@code <title>}. */
    protected abstract String getTitle();

    private static void pick(PageParameters parameters) {
        String skin = parameters.get("skin").toOptionalString();
        for (Skin candidate : Skin.values()) {
            if (candidate.id().equals(skin)) {
                ShadwickSettings.get().setCurrentSkin(candidate);
            }
        }
        String mode = parameters.get("mode").toOptionalString();
        if ("dark".equals(mode) || "light".equals(mode)) {
            Appearance.setDark("dark".equals(mode));
        }
    }

    private static Skin currentSkin() {
        return ShadwickSettings.get().getCurrentSkin();
    }

    private BookmarkablePageLink<Void> navLink(String id, Class<? extends Page> page) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, page);
        boolean current = page == getClass();
        link.add(Sw.Behaviors.button(current ? ButtonBehavior.Variant.SECONDARY : ButtonBehavior.Variant.GHOST)
                .setSize(ButtonBehavior.Size.SMALL));
        if (current) {
            link.add(AttributeModifier.replace("aria-current", "page"));
        }
        return link;
    }

    /** A link to this page with another skin or mode; the current one looks pressed. */
    private BookmarkablePageLink<Void> switchLink(String id, String parameter, String value, IModel<Boolean> current) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, getClass(), new PageParameters().add(parameter, value));
        link.add(Sw.Behaviors.button().setSize(ButtonBehavior.Size.SMALL)
                .setVariant((IModel<ButtonBehavior.Variant>) () -> current.getObject() ? ButtonBehavior.Variant.SECONDARY : ButtonBehavior.Variant.GHOST));
        link.add(AttributeModifier.replace("aria-current", (IModel<String>) () -> current.getObject() ? "true" : null));
        return link;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(EXAMPLES_CSS));
    }
}
