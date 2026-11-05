package dev.jbaby.shadwick.components.table;

import dev.jbaby.shadwick.behaviors.BadgeBehavior;
import dev.jbaby.shadwick.components.SwBadge;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import java.util.Objects;

/** An escaped badge cell whose appearance follows the row. */
public class SwBadgeColumn<T, S> extends LambdaColumn<T, S> {
    private final SerializableFunction<T, BadgeBehavior.Variant> variant;
    public SwBadgeColumn(IModel<String> title, SerializableFunction<T, ?> value, SerializableFunction<T, BadgeBehavior.Variant> variant) {
        this(title, null, value, variant);
    }
    public SwBadgeColumn(IModel<String> title, S sort, SerializableFunction<T, ?> value, SerializableFunction<T, BadgeBehavior.Variant> variant) {
        super(title, sort, value::apply); this.variant = Objects.requireNonNull(variant);
    }
    @Override public void populateItem(Item<ICellPopulator<T>> item, String id, IModel<T> row) {
        item.add(new SwBadge(id, getDataModel(row), (IModel<BadgeBehavior.Variant>) () -> variant.apply(row.getObject())));
    }
}
