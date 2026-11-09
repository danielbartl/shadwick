package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.ShadwickSettings;
import dev.jbaby.shadwick.Skin;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import java.util.List;
import java.util.Locale;

/**
 * The showcase's layout: a header with the pages, skin, light/dark and language controls.
 * Preferences are page parameters ({@code ?skin=lyra}, {@code ?mode=dark}, {@code ?lang=de}), kept
 * in the session once picked, so every page can be opened in any skin and mode by its URL. Two
 * windows of one browser share the session: compare skins side by side in two browsers (or a
 * private window).
 */
public abstract class BasePage extends WebPage {

    private static final CssResourceReference EXAMPLES_CSS =
            new CssResourceReference(BasePage.class, "examples.css");
    private static final List<Locale> LANGUAGES =
            List.of("en", "de", "es", "fr", "it", "ja", "nl", "pt", "hr").stream()
                    .map(Locale::forLanguageTag)
                    .toList();

    protected BasePage(PageParameters parameters) {
        super(parameters);
        pick(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        ThemeBehavior theme = newThemeBehavior(getPageParameters());
        addDocument(theme);
        addNavigation();
        addExampleInformation();
        addAppearanceControls(theme);
        add(languageForm());
    }

    private static void pick(PageParameters parameters) {
        String language = parameters.get("lang").toOptionalString();
        if (language != null && !language.isBlank()) {
            Locale locale = Locale.forLanguageTag(language);
            if (!locale.getLanguage().isEmpty()) org.apache.wicket.Session.get().setLocale(locale);
        }
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

    private void addDocument(ThemeBehavior theme) {
        add(
                new org.apache.wicket.markup.html.WebMarkupContainer("favicon")
                        .add(
                                AttributeModifier.replace(
                                        "href",
                                        (IModel<String>)
                                                () ->
                                                        urlFor(
                                                                        new org.apache.wicket
                                                                                .request.resource
                                                                                .PackageResourceReference(
                                                                                BasePage.class,
                                                                                "clover.svg"),
                                                                        null)
                                                                .toString())));
        // Render the theme on <html>, so the page never flashes light first.
        TransparentWebMarkupContainer html = new TransparentWebMarkupContainer("html");
        html.add(
                AttributeModifier.replace(
                        "lang", (IModel<String>) () -> getLocale().toLanguageTag()));
        if (theme != null) {
            html.add(theme);
        } else {
            html.add(
                    AttributeModifier.replace(
                            "class", (IModel<String>) () -> Appearance.isDark() ? "dark" : null));
        }
        add(html);
        TransparentWebMarkupContainer main = new TransparentWebMarkupContainer("main");
        main.setMarkupId("main");
        add(main, new dev.jbaby.shadwick.components.SwSkipLink("skip", main));
        add(new TransparentWebMarkupContainer("siteHeader").add(Sw.Behaviors.siteHeader()));
        add(new Label("title", (IModel<String>) () -> getTitle() + " · Shadwick"));
    }

    private void addNavigation() {
        var brand = navLink("catalogHome", HomePage.class);
        brand.add(
                new org.apache.wicket.markup.html.image.Image(
                        "brandLogo",
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                BasePage.class, "clover.svg")));
        add(brand, navLink("studioHome", StudioPage.class));
        add(new CatalogNavigation("catalogNavigation", getClass(), getPageParameters()));
    }

    private void addExampleInformation() {
        var example = CatalogPages.find(getClass());
        add(
                new Label("pageGroup", example == null ? "Getting started" : example.group()),
                new Label("pageTitle", (IModel<String>) this::getTitle));
        add(
                new org.apache.wicket.markup.html.link.ExternalLink(
                        "pageGuide", CatalogPages.guide(getClass())));
        add(
                new org.apache.wicket.markup.html.WebMarkupContainer("viewCode")
                        .setVisible(getClass() != HomePage.class));
        add(
                new ExampleSourcePanel("exampleSource", getClass())
                        .setVisible(getClass() != HomePage.class));
    }

    private void addAppearanceControls(ThemeBehavior theme) {
        for (Skin skin : Skin.values()) {
            add(switchLink(skin.id(), "skin", skin.id(), () -> currentSkin() == skin));
        }
        add(new TransparentWebMarkupContainer("modeLinks").setVisible(theme == null));
        add(switchLink("light", "mode", "light", () -> !Appearance.isDark()));
        add(switchLink("dark", "mode", "dark", Appearance::isDark));
    }

    private Form<Void> languageForm() {
        Model<Locale> language =
                Model.of(
                        LANGUAGES.stream()
                                .filter(
                                        locale ->
                                                locale.getLanguage()
                                                        .equals(getLocale().getLanguage()))
                                .findFirst()
                                .orElse(Locale.ENGLISH));
        Form<Void> form =
                new Form<>("languageForm") {

                    @Override
                    protected void onSubmit() {
                        getSession().setLocale(language.getObject());
                        setResponsePage(
                                BasePage.this.getClass(),
                                new PageParameters(getPageParameters())
                                        .set("lang", language.getObject().toLanguageTag()));
                    }
                };
        form.add(
                languageChoice(language),
                new Label("languageLabel", new StringResourceModel("BasePage.language", this)),
                new org.apache.wicket.markup.html.form.Button(
                                "applyLanguage", new StringResourceModel("BasePage.apply", this))
                        .add(
                                Sw.Behaviors.button(ButtonBehavior.Variant.GHOST)
                                        .setSize(ButtonBehavior.Size.SMALL)));
        return form;
    }

    private BookmarkablePageLink<Void> navLink(String id, Class<? extends Page> page) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, page);
        boolean current = page == getClass();
        link.add(
                Sw.Behaviors.button(
                                current
                                        ? ButtonBehavior.Variant.SECONDARY
                                        : ButtonBehavior.Variant.GHOST)
                        .setSize(ButtonBehavior.Size.SMALL));
        if (current) {
            link.add(AttributeModifier.replace("aria-current", "page"));
        }
        return link;
    }

    /** A link to this page with another skin or mode; the current one looks pressed. */
    private BookmarkablePageLink<Void> switchLink(
            String id, String parameter, String value, IModel<Boolean> current) {
        BookmarkablePageLink<Void> link =
                new BookmarkablePageLink<>(
                        id,
                        getClass(),
                        new PageParameters(getPageParameters()).set(parameter, value));
        link.add(
                Sw.Behaviors.button()
                        .setSize(ButtonBehavior.Size.SMALL)
                        .setVariant(
                                (IModel<ButtonBehavior.Variant>)
                                        () ->
                                                current.getObject()
                                                        ? ButtonBehavior.Variant.SECONDARY
                                                        : ButtonBehavior.Variant.GHOST));
        link.add(
                AttributeModifier.replace(
                        "aria-current",
                        (IModel<String>) () -> current.getObject() ? "true" : null));
        return link;
    }

    private static Skin currentSkin() {
        return ShadwickSettings.get().getCurrentSkin();
    }

    private DropDownChoice<Locale> languageChoice(IModel<Locale> language) {
        DropDownChoice<Locale> choice =
                new DropDownChoice<>(
                        "language",
                        language,
                        LANGUAGES,
                        new IChoiceRenderer<>() {

                            @Override
                            public Object getDisplayValue(Locale locale) {
                                return locale.getDisplayLanguage(locale);
                            }

                            @Override
                            public String getIdValue(Locale locale, int index) {
                                return locale.toLanguageTag();
                            }
                        });
        choice.setMarkupId("showcase-language");
        choice.setLabel(new StringResourceModel("BasePage.language", this));
        return choice;
    }

    /** Opt in to the browser preference demo; other showcases keep server reference modes. */
    protected ThemeBehavior newThemeBehavior(PageParameters parameters) {
        return null;
    }

    /** The page's name, for its {@code <title>}. */
    protected abstract String getTitle();

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(EXAMPLES_CSS));
        response.render(
                org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference(
                        ExampleSourcePanel.SCRIPT));
    }
}
