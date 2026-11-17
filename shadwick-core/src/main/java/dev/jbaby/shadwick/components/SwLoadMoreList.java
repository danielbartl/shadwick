package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.util.ComponentIds;
import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.AjaxBusyBehavior;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.util.SerializableBiFunction;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

/**
 * A long list - an activity feed, search results, a product catalogue - that shows its first items and appends more
 * as the user asks for them, instead of pages. It follows Wicket Oat's {@code OatLoadMoreList}:
 * <pre>{@code
 * add(new SwLoadMoreList<>("orders", ordersProvider, 20, (id, order) -> new OrderCard(id, order)));
 * }</pre>
 * on {@code <div wicket:id="orders"></div>}. The items come from a Wicket {@link IDataProvider}, a batch at a time,
 * each on a {@code <div role="listitem">}. "Load more" appends the next batch over Ajax without re-rendering the items
 * already shown, and moves the keyboard focus to the first new one. Without JavaScript it is an ordinary link: the
 * page renders again with the next batch and the browser scrolls to it. {@link #setLoadOnScroll(boolean)
 * setLoadOnScroll(true)} also loads the next batch when the button scrolls into view; the button stays as the
 * keyboard's way in.
 * <p>
 * Loaded items stay in the page, so it suits hundreds of items; for more, or when users jump around, page with
 * {@link SwDataTable} or {@link SwPagingNavigator} instead. Items added or removed between loads shift the batches,
 * as with any offset paging.
 *
 * @param <T> the item type
 */
public class SwLoadMoreList<T> extends Panel {

    private final IDataProvider<T> provider;
    private final long batchSize;
    private final SerializableBiFunction<String, IModel<T>, ? extends Component> item;
    private boolean loadOnScroll;
    private IModel<String> label;

    private WebMarkupContainer list;
    private RepeatingView batches;
    private WebMarkupContainer footer;
    private Label status;
    private long shown;
    private int batchCount;
    private boolean exhausted;
    // Read once per request: a provider may count with a database query
    private transient Long size;
    // What the status says after a load; empty on an ordinary render so a page load announces nothing
    private String announcement = "";

    /**
     * @param provider the items
     * @param batchSize how many items each batch has, at least 1
     * @param item creates an item's component with the id and model it is given
     */
    public SwLoadMoreList(String id, IDataProvider<T> provider, long batchSize,
                          SerializableBiFunction<String, IModel<T>, ? extends Component> item) {
        super(id);
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1: " + batchSize);
        }
        this.provider = Objects.requireNonNull(provider);
        this.batchSize = batchSize;
        this.item = Objects.requireNonNull(item);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        list = new WebMarkupContainer("list");
        list.setOutputMarkupId(true);
        list.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> label == null ? null : label.getObject()));
        add(list);
        batches = new RepeatingView("batch");
        list.add(batches);
        add(newFooter());
        status = new Label("status", (IModel<String>) () -> announcement) {
            @Override
            protected void onAfterRender() {
                super.onAfterRender();
                // Said once: a later render doesn't repeat it
                announcement = "";
            }
        };
        status.setOutputMarkupId(true);
        add(status);
    }

    private WebMarkupContainer newFooter() {
        footer = new WebMarkupContainer("footer") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(hasMore());
            }
        };
        footer.setOutputMarkupPlaceholderTag(true);
        Label count = new Label("count", new StringResourceModel("SwLoadMoreList.count", this)
                .setParameters((IModel<Long>) () -> shown, (IModel<Long>) this::total));
        count.setOutputMarkupId(true);
        footer.add(count);
        AjaxFallbackLink<Void> more = new AjaxFallbackLink<>("more") {
            @Override
            public void onClick(Optional<AjaxRequestTarget> target) {
                if (target.isPresent()) loadMore(target.get());
                else loadMore();
            }

            // Without JavaScript the page renders again; the address scrolls to the batch the click adds
            @Override
            protected CharSequence appendAnchor(ComponentTag tag, CharSequence url) {
                return url + "#" + batchId(batchCount);
            }
        };
        more.setBody(new StringResourceModel("SwLoadMoreList.more", this));
        more.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE));
        more.add(new AjaxBusyBehavior());
        more.add(AttributeModifier.replace("aria-describedby", count.getMarkupId()));
        more.add(AttributeModifier.replace("data-sw-load-on-scroll", (IModel<String>) () -> loadOnScroll ? "" : null));
        footer.add(more);
        return footer;
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        // The first batch, once the provider can be read (not in the constructor)
        if (batchCount == 0) addBatch();
    }

    /** Also loads the next batch when the button scrolls into view, as in an endless feed. */
    public SwLoadMoreList<T> setLoadOnScroll(boolean loadOnScroll) {
        this.loadOnScroll = loadOnScroll;
        return this;
    }

    /** The list's name for screen readers, e.g. "Recent activity". */
    public SwLoadMoreList<T> setLabel(String label) {
        return setLabel(Model.of(label));
    }

    public SwLoadMoreList<T> setLabel(IModel<String> label) {
        this.label = label;
        return this;
    }

    /** Whether more items are shown with {@link #setLoadOnScroll} as the button scrolls into view. */
    public boolean isLoadOnScroll() {
        return loadOnScroll;
    }

    /** How many items are shown. */
    public long getShownCount() {
        return shown;
    }

    /**
     * Shows the next batch over Ajax, appending only it, and announces how many are shown; does nothing once all items
     * are shown.
     */
    public SwLoadMoreList<T> loadMore(AjaxRequestTarget target) {
        if (!hasMore()) {
            return this;
        }
        Component batch = addBatch();
        // Make room for the new batch at the end of the list, then render only it
        target.prependJavaScript("Shadwick.loadMoreSlot(" + JSONObject.quote(list.getMarkupId()) + ","
                + JSONObject.quote(batch.getMarkupId()) + ")");
        target.add(batch, footer, status);
        target.appendJavaScript("Shadwick.loadMoreFocus(" + JSONObject.quote(batch.getMarkupId()) + ")");
        return this;
    }

    /** Shows the next batch from an ordinary (non-Ajax) request: the page then renders with it. */
    public SwLoadMoreList<T> loadMore() {
        if (hasMore()) addBatch();
        return this;
    }

    /** Starts again from the first batch, e.g. after the provider's filter changed. */
    public SwLoadMoreList<T> reset(AjaxRequestTarget target) {
        reset();
        target.add(this);
        return this;
    }

    /** Starts again from the first batch in an ordinary request. */
    public SwLoadMoreList<T> reset() {
        if (batches != null) batches.removeAll();
        shown = 0;
        batchCount = 0;
        size = null;
        exhausted = false;
        announcement = "";
        return this;
    }

    private Component addBatch() {
        long total = total();
        Batch batch = new Batch(batches.newChildId(), batchCount, shown, Math.max(0, Math.min(batchSize, total - shown)));
        batches.add(batch);
        shown += batch.count;
        batchCount++;
        // The provider gave fewer items than it counted, e.g. some were deleted meanwhile: there are no more
        if (batch.count < batch.requested) exhausted = true;
        if (batchCount > 1) {
            announcement = new StringResourceModel("SwLoadMoreList.status", this)
                    .setParameters(shown, total).getObject();
        }
        return batch;
    }

    private String batchId(int index) {
        return getMarkupId() + "-batch-" + index;
    }

    private boolean hasMore() {
        return !exhausted && shown < total();
    }

    private long total() {
        if (size == null) size = provider.size();
        return size;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }

    @Override
    protected void onDetach() {
        size = null;
        provider.detach();
        if (label != null) label.detach();
        super.onDetach();
    }

    /** One batch of items, rendered once when loaded. */
    private final class Batch extends WebMarkupContainer {

        private final long count;
        private final long requested;

        Batch(String id, int index, long first, long count) {
            super(id);
            this.requested = count;
            setMarkupId(batchId(index));
            setOutputMarkupId(true);
            RepeatingView items = new RepeatingView("item");
            add(items);
            long added = 0;
            if (count > 0) {
                Iterator<? extends T> rows = provider.iterator(first, count);
                while (rows.hasNext()) {
                    String childId = items.newChildId();
                    items.add(ComponentIds.fromFactory(item.apply(childId, provider.model(rows.next())), childId, "An item"));
                    added++;
                }
            }
            this.count = added;
        }
    }
}
