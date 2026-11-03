package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.Objects;

/** Styles an icon or an existing element as a loading status. Its name defaults to the localized Loading label. */
public class SpinnerBehavior extends StyledBehavior {
    public enum Size { SMALL, DEFAULT, LARGE }
    private final IModel<Size> sizeModel;

    public SpinnerBehavior() { this(Size.DEFAULT); }
    public SpinnerBehavior(Size size) { this(Model.of(size)); }
    public SpinnerBehavior(IModel<Size> size) { super("sw-spinner"); sizeModel = Objects.requireNonNull(size); }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", "status");
        tag.remove("aria-hidden");
        if (tag.getAttribute("aria-label") == null) {
            tag.put("aria-label", component.getString("SwSpinner.loading", null, "Loading"));
        }
        Size size = sizeModel.getObject();
        if (size == null || size == Size.DEFAULT) { tag.remove("data-size"); }
        else { tag.put("data-size", size == Size.SMALL ? "sm" : "lg"); }
    }

    @Override
    public void detach(Component component) { sizeModel.detach(); super.detach(component); }
}
