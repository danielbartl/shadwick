package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.SwButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;
import java.util.Locale;

public class ButtonPage extends BasePage {

    private final Label status = new Label("status", Model.of(""));

    public ButtonPage(PageParameters parameters) {
        super(parameters);
        status.setOutputMarkupId(true);
        add(status);

        // SwButton.onClick on <button>, one per variant
        add(new ListView<>("buttons", List.of(ButtonBehavior.Variant.values())) {
            @Override
            protected void populateItem(ListItem<ButtonBehavior.Variant> item) {
                ButtonBehavior.Variant variant = item.getModelObject();
                item.add(SwButton.onClick("button", name(variant), target -> {
                    status.setDefaultModelObject("Clicked " + name(variant) + ".");
                    target.add(status);
                }).setVariant(variant));
            }
        });

        // Sw.Behaviors.button() on any link, one per variant
        add(new ListView<>("links", List.of(ButtonBehavior.Variant.values())) {
            @Override
            protected void populateItem(ListItem<ButtonBehavior.Variant> item) {
                ButtonBehavior.Variant variant = item.getModelObject();
                item.add(new BookmarkablePageLink<Void>("link", HomePage.class)
                        .setBody(Model.of(name(variant)))
                        .add(Sw.Behaviors.button(variant)));
            }
        });

        // Sizes
        add(SwButton.onClick("small", "Small", target -> {
        }).setSize(ButtonBehavior.Size.SMALL));
        add(SwButton.onClick("default", "Default", target -> {
        }));
        add(SwButton.onClick("large", "Large", target -> {
        }).setSize(ButtonBehavior.Size.LARGE));
        // An icon button needs an accessible name: its aria-label is in the markup
        add(new SwButton("icon") {
            @Override
            public void onClick(org.apache.wicket.ajax.AjaxRequestTarget target) {
                status.setDefaultModelObject("Clicked Add.");
                target.add(status);
            }
        }.setSize(ButtonBehavior.Size.ICON).setVariant(ButtonBehavior.Variant.OUTLINE));

        // Disabled: a <button> gets disabled, a link loses its href and gets aria-disabled
        add(SwButton.onClick("disabledButton", "Disabled button", target -> {
        }).setEnabled(false));
        add(new Link<Void>("disabledLink") {
            @Override
            public void onClick() {
            }
        }.setBody(Model.of("Disabled link")).add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)).setEnabled(false));
    }

    private static String name(ButtonBehavior.Variant variant) {
        String name = variant.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    protected String getTitle() {
        return "Buttons";
    }
}
