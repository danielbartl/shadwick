package dev.jbaby.shadwick.components;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import java.io.Serializable;
import java.util.Objects;

/** Immutable, decoded bookmarkable table query parameters. Page numbers are zero-based. */
public final class TableQuery implements Serializable {
    private final PageParameters parameters;
    private final String pageName, sizeName, sortName;
    public TableQuery(PageParameters parameters) { this(parameters, "page", "size", "sort"); }
    public TableQuery(PageParameters parameters, String pageName, String sizeName, String sortName) {
        this.parameters = new PageParameters(Objects.requireNonNull(parameters));
        this.pageName = requireName(pageName); this.sizeName = requireName(sizeName); this.sortName = requireName(sortName);
        if (pageName.equals(sizeName) || pageName.equals(sortName) || sizeName.equals(sortName))
            throw new IllegalArgumentException("Table page, size and sort parameters must have distinct names");
    }
    public TableQuery qualified(String qualifier) {
        String prefix = requireName(qualifier) + "_";
        return new TableQuery(parameters, prefix + "page", prefix + "size", prefix + "sort");
    }
    private static String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("A query parameter name must not be blank");
        return name;
    }
    public String getPageParameter() { return pageName; }
    public String getSizeParameter() { return sizeName; }
    public String getSortParameter() { return sortName; }
    public PageParameters getParameters() { return new PageParameters(parameters); }
    public long getPage() { return Math.max(0, number(pageName, 0)); }
    public long getSize(long fallback, long maximum) {
        if (fallback <= 0 || maximum <= 0) throw new IllegalArgumentException("Page sizes must be positive");
        long requested = number(sizeName, fallback);
        return Math.min(maximum, requested > 0 ? requested : fallback);
    }
    public String getSortProperty() {
        String value = parameters.get(sortName).toOptionalString();
        if (value == null || value.isBlank()) return null;
        String[] parts = value.split(",", -1);
        return parts.length <= 2 && !parts[0].isBlank() && (parts.length == 1 || parts[1].equals("asc") || parts[1].equals("desc")) ? parts[0] : null;
    }
    public boolean isAscending() {
        String value = parameters.get(sortName).toOptionalString(); return value == null || !value.endsWith(",desc");
    }
    private long number(String name, long fallback) {
        try { return Long.parseLong(parameters.get(name).toString(Long.toString(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }
    public PageParameters page(long page) {
        if (page < 0) throw new IllegalArgumentException("Page numbers must not be negative");
        return getParameters().set(pageName, page);
    }
    public PageParameters size(long size) {
        if (size <= 0) throw new IllegalArgumentException("Page sizes must be positive");
        var result = getParameters(); result.remove(pageName); return result.set(sizeName, size);
    }
    public PageParameters sort(String property, boolean ascending) {
        requireName(property);
        var result = getParameters(); result.remove(pageName); return result.set(sortName, property + (ascending ? ",asc" : ",desc"));
    }
    /** A search form keeps everything except its own fields and this table's page parameter. */
    public PageParameters forSearch(String... ownFields) {
        var result = getParameters(); result.remove(pageName);
        for (String field : ownFields) result.remove(field);
        return result;
    }
}
