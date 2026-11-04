package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.SwBreadcrumb;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSiteHeader;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.List;

/** Native shell composition, collapsed navigation and model-driven Ajax updates. */
public class ShellPage extends BasePage {
    private static final List<String> LEVELS = List.of("Home", "Workspace", "Projects", "Design system", "Components");
    public ShellPage(PageParameters parameters) {
        super(parameters);
        String page = parameters.get("page").toString("Overview");
        int index = LEVELS.indexOf(page);
        IModel<List<String>> levels = Model.ofList(new ArrayList<>(index >= 0 ? LEVELS.subList(0, index) : LEVELS));
        Model<String> current = Model.of(page);
        Label status = new Label("status", "Viewing: " + page + "."); status.setOutputMarkupId(true); add(status);

        SwSiteHeader header = new SwSiteHeader("header"); add(header);
        BookmarkablePageLink<Void> brand = new BookmarkablePageLink<>("brand", HomePage.class);
        brand.add(new Image("logo", new PackageResourceReference(ShellPage.class, "clover.svg"))); header.add(brand);
        WebMarkupContainer nav = new WebMarkupContainer("navigation"); header.add(nav);
        nav.add(new BookmarkablePageLink<>("overview", ShellPage.class).add(Sw.Behaviors.button(ButtonBehavior.Variant.GHOST)
                .setSize(ButtonBehavior.Size.SMALL)).add(AttributeModifier.replace("aria-current", "page")));
        nav.add(new BookmarkablePageLink<>("forms", FormPage.class).add(Sw.Behaviors.button(ButtonBehavior.Variant.GHOST)
                .setSize(ButtonBehavior.Size.SMALL)));
        WebMarkupContainer end = new WebMarkupContainer("end"); end.add(Sw.Behaviors.siteHeaderEnd()); header.add(end);
        end.add(SwButton.onClick("save", "Save view", target -> {
            status.setDefaultModelObject("View saved."); target.add(status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL));

        SwSiteHeader pageHeader = new SwSiteHeader("pageHeader").setInset(true); add(pageHeader);
        SwBreadcrumb<String> trail = trail("trail", levels).setCurrent(current).setMaxItems(3).setLabel("Workspace path");
        pageHeader.add(trail);
        WebMarkupContainer pageEnd = new WebMarkupContainer("end"); pageEnd.add(Sw.Behaviors.siteHeaderEnd()); pageHeader.add(pageEnd);
        pageEnd.add(new Label("badge", "Team workspace").add(Sw.Behaviors.badge()));
        add(trail("simple", Model.ofList(List.of("Home", "Components"))).setCurrent("Site header").setLabel("Simple path"));
        add(trail("lastLink", Model.ofList(List.of("Home", "Workspace", "Projects"))).setLabel("Current link path"));

        add(SwButton.onClick("refresh", "Refresh breadcrumb", target -> {
            current.setObject("Updated overview"); status.setDefaultModelObject("Breadcrumb refreshed."); target.add(trail, status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwButton.onClick("empty", "Clear breadcrumb", target -> {
            levels.setObject(new ArrayList<>()); status.setDefaultModelObject("Breadcrumb cleared."); target.add(trail, status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwButton.onClick("restore", "Restore breadcrumb", target -> {
            levels.setObject(new ArrayList<>(LEVELS)); current.setObject("Overview");
            status.setDefaultModelObject("Breadcrumb restored."); target.add(trail, status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwButton.onClick("long", "Use long labels", target -> {
            levels.setObject(new ArrayList<>(List.of("Home", "LongProjectName".repeat(18))));
            current.setObject("A page title that can wrap over several lines on a narrow screen");
            status.setDefaultModelObject("Long labels applied."); target.add(trail, status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
    }
    private SwBreadcrumb<String> trail(String id, IModel<List<String>> levels) {
        return SwBreadcrumb.of(id, levels, (item, value) -> item.add(new BookmarkablePageLink<Void>("link", ShellPage.class,
                new PageParameters().add("page", value)).add(new Label("label", item.getModel()))));
    }
    @Override protected String getTitle() { return "Headers & breadcrumbs"; }
}
