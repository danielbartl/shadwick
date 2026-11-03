package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.*;
import dev.jbaby.shadwick.components.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/** The Phase 2 display components in the current skin and mode. */
public class DisplayPage extends BasePage {
    public DisplayPage(PageParameters parameters) {
        super(parameters);
        add(new SwCard("card"));
        add(new SwCard("smallCard").setSize(CardBehavior.Size.SMALL));
        add(new ListView<>("badges", List.of(BadgeBehavior.Variant.values())) {
            @Override
            protected void populateItem(ListItem<BadgeBehavior.Variant> item) {
                BadgeBehavior.Variant variant = item.getModelObject();
                item.add(new SwBadge("badge", variant.name().toLowerCase(java.util.Locale.ROOT), variant));
            }
        });
        add(new SwAlert("alert", "Your changes have been saved."));
        add(new SwAlert("destructive", "The project could not be saved.", AlertBehavior.Variant.DESTRUCTIVE));
        WebMarkupContainer composed = new WebMarkupContainer("composed");
        composed.add(Sw.Behaviors.alert());
        composed.add(new SwIcon("icon", "info"));
        add(composed);

        String imageUrl = urlFor(new PackageResourceReference(DisplayPage.class, "avatar.svg"), null).toString();
        SwAvatar avatar = new SwAvatar("avatar", Model.of(imageUrl), Model.of("AL"), SwAvatar.Size.DEFAULT).setLabel("Ada Lovelace");
        avatar.setOutputMarkupId(true);
        add(avatar);
        add(new SwAvatar("initials", "GH").setLabel("Grace Hopper"));
        add(new SwAvatar("smallAvatar", null, Model.of("AL"), SwAvatar.Size.SMALL));
        add(new SwAvatar("largeAvatar", null, Model.of("GH"), SwAvatar.Size.LARGE));
        add(SwButton.onClick("refreshAvatar", "Refresh avatar", target -> target.add(avatar)));

        add(new SwSeparator("separator").setDecorative(false));
        add(new SwSeparator("vertical").setOrientation(SeparatorBehavior.Orientation.VERTICAL));
        add(new SwKbd("modifier", "Ctrl"));
        add(new SwKbd("key", "K"));
        add(new SwSkeleton("line"));
        add(new SwSkeleton("circle", SkeletonBehavior.Shape.CIRCLE));
        add(new SwSkeleton("box", SkeletonBehavior.Shape.BOX));
        add(new SwSpinner("spinner").setLabel("Loading projects"));
        add(new SwSpinner("smallSpinner", SpinnerBehavior.Size.SMALL));
        add(new SwSpinner("largeSpinner", SpinnerBehavior.Size.LARGE));
        add(new SwIcon("search", "search").setLabel("Search"));
        add(new SwIcon("calendar", "calendar").setLabel("Calendar"));
        add(new SwIcon("download", "download").setLabel("Download"));
        add(new SwEmptyState("empty", "No projects yet", "Create a project to get started.").setIcon("folder"));
    }

    @Override
    protected String getTitle() { return "Display"; }
}
