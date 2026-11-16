package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An icon, drawn as inline SVG in the current text color and sized to the text (1em),
 * from a set of common business icons from <a href="https://lucide.dev">Lucide</a>
 * (ISC license, see {@code META-INF/NOTICE}): {@code plus}, {@code pencil},
 * {@code trash-2}, {@code search}, {@code download}, {@code calendar}, {@code user},
 * {@code settings}, {@code chart-line}, ... - see {@link #names()}.
 * <pre>{@code
 * <a wicket:id="export" class="button"><svg wicket:id="icon"></svg> Export</a>
 *
 * link.add(new SwIcon("icon", "download"));
 * }</pre>
 * Put it on an {@code <svg>} tag, or on any other tag to get the SVG inside it. An icon
 * next to text is decorative and hidden from screen readers; an icon on its own (an
 * icon-only button) needs a {@link #setLabel label}. Register icons of your own, with
 * the same 24×24 stroke style, with {@link #register}. No image requests, no icon font,
 * and nothing the Content Security Policy needs to allow.
 * <p>
 * {@link #setSize(Integer)} gives the icon a fixed size in pixels instead of 1em, also inside components that size
 * their icons (a button's 16 pixels); {@link #setStrokeWidth(Number)} replaces Lucide's stroke width of 2.
 */
public class SwIcon extends WebComponent {

    private static final Map<String, String> ICONS = new ConcurrentHashMap<>(load());

    private IModel<String> label;
    private Integer size;
    private String strokeWidth;

    /** @param name an icon's name, e.g. {@code "pencil"} */
    public SwIcon(String id, String name) {
        this(id, Model.of(name));
    }

    /** @param name an icon's name, read on every render, e.g. to switch between icons */
    public SwIcon(String id, IModel<String> name) {
        super(id, name);
        requireKnown(name.getObject());
    }

    /**
     * An icon, or for a {@code null} name nothing: a hidden component in its place, so
     * the markup can always have the icon's tag.
     */
    public static Component ofNullable(String id, String name) {
        return name != null ? new SwIcon(id, name) : new WebMarkupContainer(id).setVisible(false);
    }

    // The constructors' check, for components that keep an icon's name for later
    static String requireKnown(String name) {
        if (name != null && !ICONS.containsKey(name)) {
            throw new IllegalArgumentException(noIcon(name));
        }
        return name;
    }

    private static String noIcon(String name) {
        return "No icon \"" + name + "\"; the icons are " + names();
    }

    /** A name for screen readers, for an icon that stands on its own; {@code null} makes it decorative. */
    public SwIcon setLabel(IModel<String> label) {
        this.label = label;
        return this;
    }

    public SwIcon setLabel(String label) {
        return setLabel(Model.of(label));
    }

    /**
     * Width and height in pixels, from 1 to 1024; {@code null} restores the default, 1em or the size the surrounding
     * component gives its icons. Renders {@code width}, {@code height} and {@code data-icon-size}, which the skins'
     * icon sizes leave alone.
     */
    public SwIcon setSize(Integer pixels) {
        if (pixels != null && (pixels < 1 || pixels > 1024)) {
            throw new IllegalArgumentException("An icon's size must be between 1 and 1024 pixels, not " + pixels);
        }
        this.size = pixels;
        return this;
    }

    /** The stroke width in the icon's 24×24 units, more than 0 and at most 24; {@code null} restores 2. */
    public SwIcon setStrokeWidth(Number width) {
        if (width == null) {
            this.strokeWidth = null;
            return this;
        }
        double value = width.doubleValue();
        if (!Double.isFinite(value) || value <= 0 || value > 24) {
            throw new IllegalArgumentException("An icon's stroke width must be more than 0 and at most 24, not " + width);
        }
        java.math.BigDecimal exact = width instanceof java.math.BigDecimal decimal ? decimal : java.math.BigDecimal.valueOf(value);
        this.strokeWidth = exact.stripTrailingZeros().toPlainString();
        return this;
    }

    private String stroke() {
        return strokeWidth != null ? strokeWidth : "2";
    }

    /** The current accessible name, or null for a decorative icon. */
    protected String accessibleLabel() {
        return label != null ? label.getObject() : null;
    }

    /** The names of all icons, built-in and registered. */
    public static Set<String> names() {
        return Collections.unmodifiableSet(new TreeSet<>(ICONS.keySet()));
    }

    /**
     * Adds an icon or replaces one: {@code shapes} is the SVG content of a 24×24 icon
     * drawn with strokes, e.g. {@code <path d="M5 12h14"/>}. It is written into the page
     * as it is, so only register shapes you trust.
     */
    public static void register(String name, String shapes) {
        ICONS.put(name, shapes);
    }

    /** Decorative SVG for native option bodies, from the same trusted icon registry. */
    public static String decorativeMarkup(String name) {
        java.util.Objects.requireNonNull(name, "name");
        requireKnown(name);
        return "<svg class=\"sw-icon\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\""
                + " fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\""
                + " aria-hidden=\"true\" focusable=\"false\">" + ICONS.get(name) + "</svg>";
    }

    private String shapes() {
        String name = getDefaultModelObjectAsString();
        String shapes = ICONS.get(name);
        if (shapes == null) {
            throw new IllegalStateException(noIcon(name));
        }
        return shapes;
    }

    private String svgAttributes() {
        String text = label != null ? label.getObject() : null;
        StringBuilder attributes = new StringBuilder(" class=\"sw-icon\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\""
                + " fill=\"none\" stroke=\"currentColor\" stroke-width=\"" + stroke() + "\" stroke-linecap=\"round\" stroke-linejoin=\"round\"");
        if (size != null) {
            attributes.append(" width=\"").append(size).append("\" height=\"").append(size)
                    .append("\" data-icon-size=\"").append(size).append('"');
        }
        if (text != null) {
            attributes.append(" role=\"img\" aria-label=\"")
                    .append(org.apache.wicket.util.string.Strings.escapeMarkup(text)).append('"');
        } else {
            attributes.append(" aria-hidden=\"true\" focusable=\"false\"");
        }
        return attributes.toString();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        if ("svg".equalsIgnoreCase(tag.getName())) {
            for (String attribute : new String[] {"viewBox", "fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin"}) {
                tag.remove(attribute);
            }
            tag.append("class", "sw-icon", " ");
            tag.put("xmlns", "http://www.w3.org/2000/svg");
            tag.put("viewBox", "0 0 24 24");
            tag.put("fill", "none");
            tag.put("stroke", "currentColor");
            tag.put("stroke-width", stroke());
            if (size != null) {
                tag.put("width", size);
                tag.put("height", size);
                tag.put("data-icon-size", size);
            } else {
                tag.remove("data-icon-size");
            }
            tag.put("stroke-linecap", "round");
            tag.put("stroke-linejoin", "round");
            tag.remove("role");
            tag.remove("aria-label");
            tag.remove("aria-hidden");
            tag.remove("focusable");
            String text = label != null ? label.getObject() : null;
            if (text != null) {
                tag.put("role", "img");
                tag.put("aria-label", text);
            } else {
                tag.put("aria-hidden", "true");
                tag.put("focusable", "false");
            }
        }
        if (tag.isOpenClose()) {
            tag.setType(org.apache.wicket.markup.parser.XmlTag.TagType.OPEN);
        }
    }

    @Override
    public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        String body = "svg".equalsIgnoreCase(openTag.getName()) ? shapes() : "<svg" + svgAttributes() + ">" + shapes() + "</svg>";
        replaceComponentTagBody(markupStream, openTag, body);
    }

    @Override
    protected void onDetach() {
        if (label != null) {
            label.detach();
        }
        super.onDetach();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
    }

    private static Map<String, String> load() {
        Properties properties = new Properties();
        try (InputStream in = SwIcon.class.getResourceAsStream("icons/lucide.properties")) {
            if (in == null) {
                throw new IllegalStateException("icons/lucide.properties is missing");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> icons = new ConcurrentHashMap<>();
        properties.stringPropertyNames().forEach(name -> icons.put(name, properties.getProperty(name)));
        return icons;
    }
}
