package dev.jbaby.shadwick.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import java.util.List;

/** Hidden inputs for a native GET filter form. Pass query.forSearch("q", "status"). */
public class SwQueryParameters extends Panel {
    public SwQueryParameters(String id, PageParameters parameters) { this(id, Model.of(parameters)); }
    public SwQueryParameters(String id, IModel<PageParameters> parameters) {
        super(id); setRenderBodyOnly(true);
        add(new ListView<PageParameters.NamedPair>("parameters", parameters.map(p -> p == null ? List.of() : p.getAllNamed())) {
            @Override protected void populateItem(ListItem<PageParameters.NamedPair> item) {
                item.setRenderBodyOnly(true);
                var input = new WebMarkupContainer("input");
                input.add(AttributeModifier.replace("name", item.getModelObject().getKey()));
                input.add(AttributeModifier.replace("value", item.getModelObject().getValue())); item.add(input);
            }
        });
    }
}
