package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import java.util.Objects;

/** Styles native table elements. Wrap wide tables in a sw-table-container. */
public class TableBehavior extends StyledBehavior {
    public enum Size { DEFAULT, SMALL }
    private Size size = Size.DEFAULT;
    private boolean stickyFirstColumn;
    public TableBehavior() { super("sw-table"); }
    public TableBehavior setSize(Size size) { this.size = Objects.requireNonNull(size); return this; }
    public TableBehavior setStickyFirstColumn(boolean sticky) { stickyFirstColumn = sticky; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!"table".equals(tag.getName())) throw new IllegalArgumentException("TableBehavior requires <table> markup");
        super.onComponentTag(component, tag);
        if (size == Size.SMALL) tag.put("data-size", "sm"); else tag.remove("data-size");
        if (stickyFirstColumn) tag.put("data-sticky", "first-column"); else tag.remove("data-sticky");
    }
}
