package dev.jbaby.shadwick.components;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import java.text.NumberFormat;
import java.util.Objects;

/** Locale-formatted range or page summary, independent of the navigation's placement. Not a live region. */
public class SwPaginationSummary extends Label {
    public enum Format { RANGE, PAGE }
    private final IPageableItems pageable;
    private Format format = Format.RANGE;
    public SwPaginationSummary(String id, IPageableItems pageable) {
        super(id); this.pageable = Objects.requireNonNull(pageable);
        add(new dev.jbaby.shadwick.behaviors.StyledBehavior("sw-pagination-summary"));
        setDefaultModel((IModel<String>) this::summary);
    }
    public SwPaginationSummary setFormat(Format format) { this.format = Objects.requireNonNull(format); return this; }
    private String summary() {
        var numbers = NumberFormat.getIntegerInstance(getLocale());
        long count = Math.max(0, pageable.getItemCount()), pages = Math.max(1, pageable.getPageCount());
        long page = Math.max(0, Math.min(pageable.getCurrentPage(), pages - 1));
        if (format == Format.PAGE) return message("page", numbers.format(page + 1), numbers.format(pages));
        if (count == 0) return message("empty");
        long size = Math.max(1, pageable.getItemsPerPage());
        long offset = Math.min(count - 1, page > Long.MAX_VALUE / size ? Long.MAX_VALUE : page * size);
        long end = offset + Math.min(size, count - offset);
        return message("range", numbers.format(offset + 1), numbers.format(end), numbers.format(count));
    }
    private String message(String key, Object... values) {
        return new StringResourceModel("SwPaginationSummary." + key, this).setParameters(values).getObject();
    }
}
