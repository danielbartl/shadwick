package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.components.SwAvatarGroup;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.Objects;

/** Overlapping avatars on caller-owned markup. Direct members use {@code sw-avatar-group-item}. */
public class AvatarGroupBehavior extends StyledBehavior {
    private SwAvatarGroup.Size size = SwAvatarGroup.Size.DEFAULT;
    private IModel<String> label;

    public AvatarGroupBehavior() { super("sw-avatar-group"); }
    public AvatarGroupBehavior setSize(SwAvatarGroup.Size size) { this.size = Objects.requireNonNull(size); return this; }
    public AvatarGroupBehavior setLabel(String label) { return setLabel(Model.of(label)); }
    public AvatarGroupBehavior setLabel(IModel<String> label) { this.label = label; return this; }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        // Explicit list semantics survive browsers that remove them when list-style is none.
        if (!tag.getAttributes().containsKey("role"))
            tag.put("role", tag.getName().equalsIgnoreCase("ul") || tag.getName().equalsIgnoreCase("ol") ? "list" : "group");
        if (size == SwAvatarGroup.Size.DEFAULT) tag.remove("data-size"); else tag.put("data-size", size.getClassName());
        if (label != null) {
            String value = label.getObject();
            if (value == null || value.isBlank()) tag.remove("aria-label"); else tag.put("aria-label", value);
        }
    }
    @Override public void detach(Component component) {
        if (label != null) label.detach();
        super.detach(component);
    }
}
