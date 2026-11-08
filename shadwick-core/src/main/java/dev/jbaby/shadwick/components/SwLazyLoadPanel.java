package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.SkeletonBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Shows decorative skeletons, then creates content through Wicket's shared Ajax lazy-load timer.
 * Override isContentReady to wait for application-owned background work. The component creates no threads.
 * A native noscript link checks readiness and loads content without Ajax. Expected service failures and retry
 * content belong to the application; retry by replacing this panel. Unexpected exceptions follow Wicket's handling.
 * @param <T> loaded component type
 */
public abstract class SwLazyLoadPanel<T extends Component> extends AjaxLazyLoadPanel<T> {
    public static final String CONTENT_ID = "content";
    private SkeletonBehavior.Shape placeholderShape = SkeletonBehavior.Shape.LINE;
    private int placeholderCount = 3;
    private IModel<String> loadingLabel;
    private boolean contentLoaded;
    private transient boolean labelRead;
    private transient String labelSnapshot;

    public SwLazyLoadPanel(String id) { this(id, null); }
    public SwLazyLoadPanel(String id, IModel<?> model) {
        super(id, model); setOutputMarkupPlaceholderTag(true); add(new StyledBehavior("sw-lazy-load"));
    }
    public SwLazyLoadPanel<T> setPlaceholder(SkeletonBehavior.Shape shape, int count) {
        Objects.requireNonNull(shape);
        if (count < 1) throw new IllegalArgumentException("Placeholder count must be at least 1");
        placeholderShape = shape; placeholderCount = count; return this;
    }
    public SwLazyLoadPanel<T> setLoadingLabel(String label) { return setLoadingLabel(label == null ? null : Model.of(label)); }
    /** Null restores the localized library default. The panel owns and detaches this model. */
    public SwLazyLoadPanel<T> setLoadingLabel(IModel<String> label) { loadingLabel = label; return this; }

    private String loadingText() {
        if (!labelRead) {
            labelSnapshot = loadingLabel == null ? null : loadingLabel.getObject();
            if (labelSnapshot == null || labelSnapshot.isBlank())
                labelSnapshot = getString("SwLazyLoadPanel.loading", null, "Loading content…");
            labelRead = true;
        }
        return labelSnapshot;
    }
    @Override protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        tag.put("aria-busy", Boolean.toString(!contentLoaded));
    }
    @Override public Component getLoadingComponent(String id) {
        var placeholder = new Placeholder(id, placeholderShape, placeholderCount, (IModel<String>) this::loadingText);
        var load = new Link<Void>("load") {
            @Override public void onClick() { SwLazyLoadPanel.this.isLoaded(); }
        };
        load.add(new Label("label", new StringResourceModel("SwLazyLoadPanel.load", this).setDefaultValue("Load content")));
        placeholder.add(load); return placeholder;
    }
    /** Calls to this hook must include super so the surrounding busy state clears. */
    @Override protected void onContentLoaded(T content, Optional<AjaxRequestTarget> target) {
        requireContentId(content, CONTENT_ID); contentLoaded = true; super.onContentLoaded(content, target);
    }
    @Override protected void initTimer() {
        Duration interval = getUpdateInterval();
        if (interval == null || interval.compareTo(Duration.ofMillis(1)) < 0 || interval.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0)
            throw new IllegalArgumentException("Lazy-load update interval must be between 1ms and 2147483647ms");
        if (getPage().getBehaviors(AjaxLazyLoadTimer.class).isEmpty()) {
            var timer = new FirstLoadTimer(); getPage().add(timer);
            getRequestCycle().find(AjaxRequestTarget.class).ifPresent(timer::restart);
        }
    }
    @Override protected void onDetach() {
        try { if (loadingLabel != null) loadingLabel.detach(); }
        finally { labelSnapshot = null; labelRead = false; super.onDetach(); }
    }
    private static void requireContentId(Component content, String id) {
        if (content == null || !id.equals(content.getId()))
            throw new IllegalArgumentException("Lazy content must use the supplied id (\"" + id + "\")");
    }
    static final class FirstLoadTimer extends AjaxLazyLoadTimer {
        FirstLoadTimer() { setUpdateInterval(Duration.ofMillis(100)); }
    }
    static final class Placeholder extends Panel {
        Placeholder(String id, SkeletonBehavior.Shape shape, int count, IModel<String> label) {
            super(id); add(new Label("status", label));
            var skeletons = new RepeatingView("skeleton");
            for (int i = 0; i < count; i++) skeletons.add(new SwSkeleton(skeletons.newChildId(), shape));
            add(skeletons);
        }
    }
    public static SwLazyLoadPanel<Component> of(String id, SerializableFunction<String, ? extends Component> content) {
        Objects.requireNonNull(content);
        return new SwLazyLoadPanel<>(id) {
            @Override public Component getLazyLoadComponent(String contentId) {
                Component result = content.apply(contentId); requireContentId(result, contentId); return result;
            }
        };
    }
}
