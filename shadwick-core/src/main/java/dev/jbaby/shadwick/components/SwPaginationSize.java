package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** A native popover menu of page sizes. Includes the current size even when absent from the choices. */
public class SwPaginationSize extends Panel {
    private final IPageableItems pageable;
    private List<Long> choices = List.of(10L, 20L, 50L);
    private SerializableFunction<Long, ? extends CharSequence> sizeUrl;
    private final WebMarkupContainer trigger;
    public SwPaginationSize(String id, IPageableItems pageable) {
        super(id); this.pageable = Objects.requireNonNull(pageable); setOutputMarkupId(true);
        add(new StyledBehavior("sw-pagination-size"));
        add(new Label("label", (IModel<String>) () -> getString("SwPaginationSize.label")));
        trigger = new WebMarkupContainer("trigger"); trigger.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE));
        trigger.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> getString("SwPaginationSize.label") + " " + pageable.getItemsPerPage()));
        trigger.add(new Label("current", (IModel<Long>) pageable::getItemsPerPage), new SwIcon("icon", "chevron-down")); add(trigger);
        var menu = new WebMarkupContainer("menu"); menu.add(new DropdownMenuBehavior(trigger)); add(menu);
        menu.add(new ListView<Long>("sizes", (IModel<List<Long>>) () -> Stream.concat(choices.stream(), Stream.of(pageable.getItemsPerPage())).distinct().sorted().toList()) {
            @Override protected void populateItem(ListItem<Long> item) {
                long size = item.getModelObject(); item.setRenderBodyOnly(true);
                AbstractLink link;
                if (sizeUrl != null) link = new ExternalLink("link", (IModel<String>) () -> sizeUrl.apply(size).toString());
                else link = new AjaxFallbackLink<Void>("link") {
                    @Override public void onClick(Optional<AjaxRequestTarget> target) {
                        SwPaginationSize.this.pageable.setItemsPerPage(size); SwPaginationSize.this.pageable.setCurrentPage(0);
                        target.ifPresent(t -> { onAjaxEvent(t); t.focusComponent(trigger); });
                    }
                };
                link.add(DropdownMenuItemBehavior.radio((IModel<Boolean>) () -> size == SwPaginationSize.this.pageable.getItemsPerPage()));
                link.add(new Label("text", size)); item.add(link);
            }
        });
    }
    public SwPaginationSize setSizes(long... sizes) {
        if (sizes.length == 0 || Arrays.stream(sizes).anyMatch(size -> size <= 0)) throw new IllegalArgumentException("Supply positive page sizes");
        choices = Arrays.stream(sizes).boxed().distinct().sorted().toList(); return this;
    }
    public SwPaginationSize setSizeUrl(SerializableFunction<Long, ? extends CharSequence> sizeUrl) { this.sizeUrl = sizeUrl; return this; }
    public WebMarkupContainer getTrigger() { return trigger; }
    protected void onAjaxEvent(AjaxRequestTarget target) {
        if (pageable instanceof Component component) {
            if (!component.getOutputMarkupId()) throw new IllegalStateException("Give the pageable an output markup id before changing page size over Ajax");
            target.add(component);
        }
        target.add(this);
    }
}
