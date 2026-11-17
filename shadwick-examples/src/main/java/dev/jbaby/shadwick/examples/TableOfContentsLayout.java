package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;
import dev.jbaby.shadwick.components.MenuItem;
import dev.jbaby.shadwick.components.SwSidebar;
import dev.jbaby.shadwick.components.SwSidebarTrigger;
import dev.jbaby.shadwick.components.SwSiteHeader;
import dev.jbaby.shadwick.components.SwSkipLink;
import dev.jbaby.shadwick.components.SwThemeToggle;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import java.io.Serializable;
import java.util.List;

/**
 * Layout recipe "On the end, with a table of contents": the page's sections in a sidebar at the end of the line (the
 * right in a left-to-right page), with its trigger at the end of the header. Composed from the building blocks on a
 * plain WebPage, for the frames {@code SwAppLayout} doesn't shape.
 */
public abstract class TableOfContentsLayout extends WebPage {

    /** One section of the page: its heading and the id of the element it starts with. */
    protected record Section(String anchor, String title) implements Serializable {}

    protected TableOfContentsLayout(PageParameters parameters) {
        super(parameters);
    }

    /** The page's title. */
    protected abstract String title();

    /** The page's sections, in order; their elements carry the anchors as ids. */
    protected abstract List<Section> sections();

    @Override
    protected void onInitialize() {
        super.onInitialize();
        TransparentWebMarkupContainer html = new TransparentWebMarkupContainer("html");
        html.add(AttributeModifier.replace("lang", (IModel<String>) () -> getLocale().toLanguageTag()));
        html.add(new ThemeBehavior());
        add(html, new Label("documentTitle", title() + " · Field guide"));

        TransparentWebMarkupContainer main = new TransparentWebMarkupContainer("main");
        main.setMarkupId("main");
        add(new SwSkipLink("skip", main));
        TransparentWebMarkupContainer provider = new TransparentWebMarkupContainer("provider");
        provider.add(Sw.Behaviors.sidebarProvider().setCookieName("app-contents"));
        add(provider);

        SwSidebar contents = newContents();
        provider.add(contents);
        TransparentWebMarkupContainer inset = new TransparentWebMarkupContainer("inset");
        inset.add(Sw.Behaviors.sidebarInset());
        provider.add(inset);
        SwSiteHeader header = new SwSiteHeader("header");
        header.add(new Label("name", "Field guide"), new SwThemeToggle("theme"), new SwSidebarTrigger("trigger", contents));
        inset.add(header, main);
        main.add(new Label("title", title()));
    }

    private SwSidebar newContents() {
        List<MenuItem> entries = sections().stream()
                .map(section -> MenuItem.of(section.title(), getClass(), new PageParameters().set("section", section.anchor())))
                .toList();
        SwSidebar contents = new SwSidebar("contents", Model.ofList(List.of(MenuItem.group(Model.of("On this page"), entries)))) {
            // Each entry is a link to its section on this page, not to another page
            @Override
            protected AbstractLink newMenuLink(String id, MenuItem item) {
                return new ExternalLink(id, "#" + item.parameters().get("section").toString());
            }

            @Override
            protected Component newMenuIcon(String id, MenuItem item) {
                return super.newMenuIcon(id, item).setVisible(false);
            }
        };
        contents.setSide(SidebarBehavior.Side.END).setCollapsible(SidebarBehavior.Collapsible.OFFCANVAS).setGroupsOpen(true);
        contents.setLabel("On this page");
        return contents;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(TableOfContentsLayout.class, "TableOfContentsLayout.css")));
    }
}
