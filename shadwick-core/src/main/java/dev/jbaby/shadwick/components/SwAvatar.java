package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A picture with initials as its fallback while loading or after a failure. Models are read once per request.
 * A String constructor supplies initials; an IModel constructor supplies a picture URL, as in Wicket Oat.
 * Use setLabel for the person's full name. Both skins support SMALL, DEFAULT and LARGE.
 */
public class SwAvatar extends Panel {

    // Any space, such as the ideographic space in Japanese names
    private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}]+");

    public enum Size {
        SMALL("sm"),
        DEFAULT(null),
        LARGE("lg");

        private final String className;
        Size(String className) { this.className = className; }
        public String getClassName() { return className; }
    }

    private final IModel<String> imageUrl;
    private final IModel<String> initials;
    private final WebMarkupContainer img;
    private final Label label;
    private IModel<String> accessibleLabel;

    /** A picture; {@code imageUrl} is its URL. */
    public SwAvatar(String id, IModel<String> imageUrl) {
        this(id, imageUrl, null, Size.DEFAULT);
    }

    public SwAvatar(String id, String initials) {
        this(id, null, Model.of(initials), Size.DEFAULT);
    }

    /**
     * @param imageUrl the picture's URL, or {@code null} (or a model of {@code null} or a
     *                 blank URL) for none
     * @param initials the initials, shown without a picture or under it; or {@code null}
     */
    public SwAvatar(String id, IModel<String> imageUrl, IModel<String> initials, Size size) {
        super(id);
        this.imageUrl = OncePerRequestModel.of(imageUrl);
        this.initials = OncePerRequestModel.of(initials);

        WebMarkupContainer container = new WebMarkupContainer("container");
        add(container);

        if (size != Size.DEFAULT) {
            container.add(AttributeModifier.replace("data-size", size.getClassName()));
        }

        img = new WebMarkupContainer("img");
        img.add(AttributeModifier.replace("src", (IModel<String>) this::getImageUrl));
        // CSS exposes the picture once loaded, and the fallback while loading or failed.
        // Give either representation the same accessible name.
        img.add(AttributeModifier.replace("alt", (IModel<String>) () -> accessibleLabel != null ? accessibleLabel.getObject() : (hasInitials() ? getInitials() : "")));
        container.add(img);

        label = new Label("initials", (IModel<String>) this::getInitials);
        label.add(AttributeModifier.replace("title", (IModel<String>) this::getInitials));
        label.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> accessibleLabel != null ? accessibleLabel.getObject() : null));
        label.add(AttributeModifier.replace("role", (IModel<String>) () -> accessibleLabel != null && accessibleLabel.getObject() != null ? "img" : null));
        container.add(label);
    }

    /** A full name for the image and fallback; defaults to the supplied initials, or decorative for a picture only. */
    public SwAvatar setLabel(String label) { return setLabel(Model.of(label)); }

    public SwAvatar setLabel(IModel<String> label) {
        accessibleLabel = label;
        return this;
    }

    /** Initials from a model, e.g. {@code SwAvatar.initials(id, person.map(Person::initials))}. */
    public static SwAvatar initials(String id, IModel<String> initials) {
        return new SwAvatar(id, null, initials, Size.DEFAULT);
    }

    /** Initials for a name: "Jordan Lee" gives "JL", "ada" gives "A", no name "?". */
    public static String initialsOf(String name) {
        List<String> words = SPACES.splitAsStream(name != null ? name : "").filter(word -> !word.isEmpty()).toList();
        if (words.isEmpty()) {
            return "?";
        }
        String first = firstCharacter(words.get(0));
        String last = words.size() > 1 ? firstCharacter(words.get(words.size() - 1)) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    // Whole, also when it is outside the Basic Multilingual Plane (two chars)
    private static String firstCharacter(String word) {
        return word.substring(0, word.offsetByCodePoints(0, 1));
    }

    private String getImageUrl() {
        String url = imageUrl != null ? imageUrl.getObject() : null;
        return url == null || url.isBlank() ? null : url;
    }

    private String getInitials() {
        return initials != null ? initials.getObject() : null;
    }

    private boolean hasInitials() {
        return getInitials() != null;
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        boolean hasImage = getImageUrl() != null;
        img.setVisible(hasImage);
        label.setVisible(!hasImage || hasInitials());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
        if (getImageUrl() != null) {
            // Hides a picture that can't be loaded: the initials, or nothing, instead of a broken image
            response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (accessibleLabel != null) { accessibleLabel.detach(); }
        if (imageUrl != null) {
            imageUrl.detach();
        }
        if (initials != null) {
            initials.detach();
        }
    }
}
