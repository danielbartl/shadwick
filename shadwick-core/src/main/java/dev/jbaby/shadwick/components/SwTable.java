package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.TableBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;

/** A caller-owned native table. Place inside a sw-table-container for horizontal scrolling. */
public class SwTable extends WebMarkupContainer {
    private final TableBehavior appearance = new TableBehavior();
    public SwTable(String id) { super(id); add(appearance); }
    public SwTable setSize(TableBehavior.Size size) { appearance.setSize(size); return this; }
    public SwTable setStickyFirstColumn(boolean sticky) { appearance.setStickyFirstColumn(sticky); return this; }
}
