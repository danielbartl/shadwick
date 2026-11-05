package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.TableBehavior;
import dev.jbaby.shadwick.components.SwTable;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** Ordinary native cells, grouped columns, compact density and independent nested tables. */
public class TablePage extends BasePage {
    public TablePage(PageParameters parameters) {
        super(parameters); add(new SwTable("normal"), new SwTable("compact").setSize(TableBehavior.Size.SMALL), new SwTable("grouped").setStickyFirstColumn(true), new SwTable("nested").setSize(TableBehavior.Size.SMALL));
    }
    @Override protected String getTitle() { return "Tables"; }
}
