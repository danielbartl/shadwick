package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.DescriptionListBehavior;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Native read-only record details. Attach to a dl; each row contains a dt label and dd value.
 * Null/blank values show an em dash. Numbers and dates follow the component's locale.
 * Custom value factories receive VALUE_ID and render on a span; use a Panel for composed value markup.
 */
public class SwDescriptionList extends Panel {
    public static final String VALUE_ID = "value";
    private final RepeatingView items = new RepeatingView("items");
    private ZoneId timeZone = ZoneOffset.UTC;

    public SwDescriptionList(String id) { this(id, null); }
    public SwDescriptionList(String id, IModel<?> model) {
        super(id, model); setOutputMarkupId(true); add(new DescriptionListBehavior()); add(items);
    }
    public SwDescriptionList addItem(String label, IModel<?> value) { return addItem(Model.of(label), value); }
    public SwDescriptionList addItem(IModel<String> label, IModel<?> value) {
        Objects.requireNonNull(value);
        IModel<Object> display = new IModel<>() {
            private transient boolean loaded;
            private transient Object snapshot;
            @Override public Object getObject() {
                if (!loaded) {
                    Object object = value.getObject();
                    snapshot = object == null || object.toString().isBlank() ? "—" : object;
                    loaded = true;
                }
                return snapshot;
            }
            @Override public void detach() {
                try { value.detach(); } finally { snapshot = null; loaded = false; }
            }
        };
        return addItem(label, id -> new DisplayValueLabel(id, display, () -> timeZone));
    }
    /** Add a badge or a composed value; its component must use the supplied VALUE_ID. */
    public SwDescriptionList addItem(IModel<String> label, SerializableFunction<String, ? extends Component> value) {
        Objects.requireNonNull(label); Objects.requireNonNull(value);
        Component component = value.apply(VALUE_ID);
        if (component == null || !VALUE_ID.equals(component.getId()))
            throw new IllegalArgumentException("The value component must use the supplied id (\"" + VALUE_ID + "\")");
        var item = new WebMarkupContainer(items.newChildId());
        item.add(new Label("term", label), component); items.add(item); return this;
    }
    /** Read a nested property from this component's current model; the property key also looks up its label. */
    public SwDescriptionList addProperty(String property) {
        Objects.requireNonNull(property);
        return addItem(new StringResourceModel(property, this).setDefaultValue(property),
                new PropertyModel<>(this, "defaultModelObject." + property));
    }
    /** Zone for instant-bearing values; local calendar and wall-clock values are unchanged. Defaults to UTC. */
    public SwDescriptionList setTimeZone(ZoneId timeZone) { this.timeZone = Objects.requireNonNull(timeZone); return this; }
}
