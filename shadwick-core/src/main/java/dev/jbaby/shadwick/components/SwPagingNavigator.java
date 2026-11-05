package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.PaginationLinkBehavior;
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
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Native page navigation, or Ajax links with ordinary Wicket fallback URLs. Empty results have one page. */
public class SwPagingNavigator extends Panel {
    private record Entry(long page, String kind) implements Serializable {}
    private final IPageable pageable;
    private final SwPagination<Entry> navigation;
    private int siblings = 1;
    private SerializableFunction<Long, ? extends CharSequence> pageUrl;
    public SwPagingNavigator(String id, IPageable pageable) {
        super(id); this.pageable = Objects.requireNonNull(pageable); setOutputMarkupId(true);
        navigation = new SwPagination<>("navigation", OncePerRequestModel.of((IModel<List<Entry>>) this::entries)) {
            @Override protected void populateItem(ListItem<Entry> item) {
                Entry entry = item.getModelObject(); boolean ellipsis = entry.kind.equals("ellipsis");
                AbstractLink link;
                if (pageUrl != null) link = new ExternalLink("link", (IModel<String>) () -> pageUrl.apply(entry.page).toString());
                else link = new AjaxFallbackLink<Void>("link") {
                    @Override public void onClick(Optional<AjaxRequestTarget> target) {
                        SwPagingNavigator.this.pageable.setCurrentPage(entry.page);
                        target.ifPresent(t -> { onAjaxEvent(t); t.focusComponent(this); });
                    }
                };
                boolean previous = entry.kind.equals("previous"), next = entry.kind.equals("next");
                link.setEnabled(!ellipsis && (!previous || current() > 0) && (!next || current() < last()));
                link.setVisible(!ellipsis);
                String suffix = previous || next ? entry.kind : "page-" + (entry.page + 1);
                link.setMarkupId(SwPagingNavigator.this.getMarkupId() + "-" + suffix); link.setOutputMarkupId(true);
                var appearance = new PaginationLinkBehavior().setCurrent((IModel<Boolean>) () -> !previous && !next && entry.page == current());
                if (previous || next) {
                    appearance.setSize(ButtonBehavior.Size.DEFAULT);
                    link.add(AttributeModifier.append("class", "sw-pagination-" + entry.kind));
                    link.add(AttributeModifier.replace("aria-label", getString("SwPagingNavigator." + entry.kind + "Label")));
                } else link.add(AttributeModifier.replace("aria-label", getString("SwPagingNavigator.page", null, "Page") + " " + (entry.page + 1)));
                link.add(appearance);
                link.add(new SwIcon("previous", "chevron-left").setVisible(previous));
                link.add(new SwIcon("next", "chevron-right").setVisible(next));
                var text = new Label("label", previous || next ? getString("SwPagingNavigator." + entry.kind) : Long.toString(entry.page + 1));
                if (previous || next) text.add(AttributeModifier.append("class", "sw-pagination-" + entry.kind + "-text"));
                link.add(text); item.add(link);
                var gap = new WebMarkupContainer("ellipsis"); gap.setVisible(ellipsis);
                gap.add(new SwIcon("icon", "ellipsis"), new Label("label", getString("SwPagingNavigator.more"))); item.add(gap);
            }
        };
        navigation.setOutputMarkupId(true); add(navigation);
    }
    private long last() { return Math.max(1, pageable.getPageCount()) - 1; }
    private long current() { return Math.max(0, Math.min(pageable.getCurrentPage(), last())); }
    private List<Entry> entries() {
        var entries = new ArrayList<Entry>(); entries.add(new Entry(Math.max(0, current() - 1), "previous"));
        for (long page : PageWindow.pages(current(), pageable.getPageCount(), siblings)) entries.add(new Entry(page, page < 0 ? "ellipsis" : "page"));
        entries.add(new Entry(current() < last() ? current() + 1 : last(), "next")); return entries;
    }
    public SwPagingNavigator setSiblings(int siblings) {
        PageWindow.pages(0, 1, siblings); this.siblings = siblings; return this;
    }
    public SwPagingNavigator setLabel(String label) { navigation.setLabel(label); return this; }
    public SwPagingNavigator setLabel(IModel<String> label) { navigation.setLabel(label); return this; }
    /** When set, links navigate to real bookmarkable URLs instead of running Ajax callbacks. */
    public SwPagingNavigator setPageUrl(SerializableFunction<Long, ? extends CharSequence> pageUrl) { this.pageUrl = pageUrl; return this; }
    protected void onAjaxEvent(AjaxRequestTarget target) {
        if (pageable instanceof Component component) {
            if (!component.getOutputMarkupId()) throw new IllegalStateException("Give the pageable an output markup id before using Ajax pagination");
            target.add(component);
        }
        target.add(this);
    }
}
