package dev.jbaby.shadwick.components.table;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.SwIcon;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.List;
import java.util.Optional;

/** Sortable native headers. Ajax has ordinary fallback links; an optional URL hook supplies bookmarkable links. */
public class SwHeadersToolbar<S> extends AbstractToolbar {
    public <T> SwHeadersToolbar(DataTable<T, S> table, ISortStateLocator<S> locator) {
        super(table); table.setOutputMarkupId(true);
        add(new ListView<IColumn<T, S>>("headers", (IModel<List<IColumn<T, S>>>) () -> List.copyOf(table.getColumns())) {
            @Override protected void populateItem(ListItem<IColumn<T, S>> item) {
                var column = item.getModelObject();
                configureHeader(item, item.getIndex());
                item.add(AttributeModifier.replace("scope", column.getHeaderColspan() > 1 ? "colgroup" : "col"));
                item.add(AttributeModifier.append("class", "sw-table-head"));
                if (column instanceof IStyledColumn<?, ?> styled && styled.getCssClass() != null) {
                    item.add(AttributeModifier.append("class", styled.getCssClass()));
                    var classes = java.util.Arrays.asList(styled.getCssClass().split("\\s+"));
                    if (classes.contains("sw-table-align-end")) item.add(AttributeModifier.replace("data-align", "end"));
                    else if (classes.contains("sw-table-align-center")) item.add(AttributeModifier.replace("data-align", "center"));
                }
                if (column.getHeaderColspan() > 1) item.add(AttributeModifier.replace("colspan", column.getHeaderColspan()));
                if (column.getHeaderRowspan() > 1) item.add(AttributeModifier.replace("rowspan", column.getHeaderRowspan()));
                if (column.isSortable()) {
                    S property = column.getSortProperty();
                    item.add(AttributeModifier.replace("aria-sort", (IModel<String>) () -> {
                        var order = locator.getSortState().getPropertySortOrder(property);
                        return order == SortOrder.ASCENDING ? "ascending" : order == SortOrder.DESCENDING ? "descending" : null;
                    }));
                    item.add(new SortHeader<>("label", column, locator));
                } else item.add(column.getHeader("label"));
            }
        });
    }
    /** Custom per-column header attributes, e.g. visibility. */
    protected void configureHeader(org.apache.wicket.Component header, int index) {}
    /** Return a real page URL to use native navigation, or null for an Ajax fallback callback. */
    protected CharSequence sortUrl(S property, SortOrder next) { return null; }
    protected void onSortChanged(Optional<AjaxRequestTarget> target) { target.ifPresent(t -> t.add(getTable())); }
    private class SortHeader<T> extends Panel {
        SortHeader(String id, IColumn<T, S> column, ISortStateLocator<S> locator) {
            super(id); setRenderBodyOnly(true); S property = column.getSortProperty();
            IModel<SortOrder> next = () -> locator.getSortState().getPropertySortOrder(property) == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;
            AbstractLink link;
            if (sortUrl(property, next.getObject()) != null) link = new ExternalLink("link", (IModel<String>) () -> sortUrl(property, next.getObject()).toString());
            else link = new AjaxFallbackLink<Void>("link") {
                @Override public void onClick(Optional<AjaxRequestTarget> target) {
                    locator.getSortState().setPropertySortOrder(property, next.getObject());
                    getTable().setCurrentPage(0); onSortChanged(target);
                    target.ifPresent(t -> t.focusComponent(this));
                }
            };
            link.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL));
            link.add(AttributeModifier.append("class", "sw-table-sort"));
            link.setMarkupId(getTable().getMarkupId() + "-sort-" + getParentIndex(column)); link.setOutputMarkupId(true);
            link.add(column.getHeader("label"));
            link.add(new SwIcon("icon", (IModel<String>) () -> {
                var order = locator.getSortState().getPropertySortOrder(property);
                return order == SortOrder.ASCENDING ? "chevron-up" : order == SortOrder.DESCENDING ? "chevron-down" : "chevrons-up-down";
            })); add(link);
        }
        private int getParentIndex(IColumn<T, S> column) { return getTable().getColumns().indexOf(column); }
    }
}
