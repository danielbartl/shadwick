package dev.jbaby.shadwick.components;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.util.SerializableBiFunction;
import dev.jbaby.shadwick.util.SerializableFunction;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.Objects;
import java.util.Optional;

/**
 * A list of items next to the details of the one selected - customers and a customer, tickets and a ticket, messages
 * and a message. It follows Wicket Oat's {@code OatMasterDetail}. Where there is room for one pane only (it is narrower
 * than 40rem), it shows one at a time: choosing an item shows its details, with a Back button to the list.
 * <pre>{@code
 * SwMasterDetail<Customer> customers = new SwMasterDetail<>("customers", new Model<>());
 * customers.setMaster(id -> new CustomerList(id, customers));      // its items call customers.select(target, c)
 * customers.setDetail((id, customer) -> new CustomerPanel(id, customer));
 * add(customers);
 * }</pre>
 * on {@code <div wicket:id="customers"></div>}. The list is yours: its items call {@link #select(AjaxRequestTarget,
 * Object)} from an Ajax link, or {@link #select(Object)} from an ordinary link, which works without JavaScript too.
 * Showing the details doesn't re-render the list, so it keeps its scroll position; mark the selected item yourself
 * (with {@link #isSelected}) and re-render the list if it shows the mark. When only one pane shows, the keyboard
 * focus moves to the details, and Back returns it to the item it came from.
 *
 * @param <T> the type of the items
 */
public class SwMasterDetail<T> extends Panel {

    /** The id the master's factory is given. */
    public static final String MASTER_ID = "master";
    /** The id the detail's factory is given. */
    public static final String DETAIL_ID = "detail";

    private final IModel<T> selection;
    private SerializableBiFunction<String, IModel<T>, ? extends Component> detail;
    private WebMarkupContainer detailArea;
    private boolean showingDetail;
    // The selection the detail was made for, so a change made through the model directly also shows
    private transient Object shownSelection;
    private boolean detailBuilt;

    /** @param selection the selected item; {@code null} for none */
    public SwMasterDetail(String id, IModel<T> selection) {
        super(id);
        this.selection = Objects.requireNonNull(selection);
        setOutputMarkupId(true);
    }

    /** The list of items; the factory receives the id to use ({@link #MASTER_ID}). */
    public SwMasterDetail<T> setMaster(SerializableFunction<String, ? extends Component> master) {
        Component component = master.apply(MASTER_ID);
        check(component, MASTER_ID, "master");
        addOrReplace(component);
        return this;
    }

    /**
     * The details of the selected item: the factory receives the id to use ({@link #DETAIL_ID}) and the selection
     * model, and is called whenever the selection changes.
     */
    public SwMasterDetail<T> setDetail(SerializableBiFunction<String, IModel<T>, ? extends Component> detail) {
        this.detail = Objects.requireNonNull(detail);
        if (selection.getObject() != null) check(detail.apply(DETAIL_ID, selection), DETAIL_ID, "detail");
        detailBuilt = false;
        return this;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (get(MASTER_ID) == null) add(new WebMarkupContainer(MASTER_ID).setVisible(false));
        detailArea = new WebMarkupContainer("detailArea");
        detailArea.setOutputMarkupId(true);
        add(detailArea);
        AjaxFallbackLink<Void> back = new AjaxFallbackLink<>("back") {
            @Override
            public void onClick(Optional<AjaxRequestTarget> target) {
                if (target.isPresent()) showList(target.get());
                else showList();
            }
        };
        back.setBody(new StringResourceModel("SwMasterDetail.back", this));
        back.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL));
        detailArea.add(back);
        detailArea.add(newEmptyState(DETAIL_ID));
    }

    /**
     * Selects an item and shows its details (where only one pane shows, instead of the list). Re-renders the detail;
     * re-render the master yourself if it marks the selection.
     */
    public SwMasterDetail<T> select(AjaxRequestTarget target, T item) {
        select(item);
        updateDetail();
        target.add(detailArea);
        switchTo(target);
        return this;
    }

    /** Selects an item from an ordinary (non-Ajax) request: the page then renders with its details. */
    public SwMasterDetail<T> select(T item) {
        selection.setObject(item);
        showingDetail = item != null;
        detailBuilt = false;
        return this;
    }

    /** Where only one pane shows, goes back from the details to the list; the selection stays. */
    public SwMasterDetail<T> showList(AjaxRequestTarget target) {
        showList();
        switchTo(target);
        return this;
    }

    /** Goes back to the list from an ordinary (non-Ajax) request. */
    public SwMasterDetail<T> showList() {
        showingDetail = false;
        return this;
    }

    /** Whether this item is the selected one, e.g. to mark it in the list. */
    public boolean isSelected(T item) {
        return item != null && item.equals(selection.getObject());
    }

    /** The selected item. */
    public IModel<T> getSelection() {
        return selection;
    }

    /** The empty state shown while nothing is selected; override to change it. */
    protected Component newEmptyState(String id) {
        return new SwEmptyState(id, new StringResourceModel("SwMasterDetail.empty", this), new Model<>()).setBorder(false);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        // Also a selection set through the model, e.g. by the master's own links, gets its details
        if (!detailBuilt || !Objects.equals(shownSelection, selection.getObject())) updateDetail();
    }

    private void updateDetail() {
        T item = selection.getObject();
        Component component = item == null || detail == null ? newEmptyState(DETAIL_ID) : detail.apply(DETAIL_ID, selection);
        check(component, DETAIL_ID, "detail");
        detailArea.addOrReplace(component);
        shownSelection = item;
        detailBuilt = true;
    }

    private static void check(Component component, String id, String part) {
        if (component == null || !id.equals(component.getId())) {
            throw new IllegalArgumentException("The " + part + " must use the id passed to the factory (\"" + id
                    + "\"), but was " + (component == null ? "null" : "\"" + component.getId() + "\""));
        }
    }

    /** Switches which pane shows where only one does, without re-rendering the list (keeping its scroll position). */
    private void switchTo(AjaxRequestTarget target) {
        target.appendJavaScript("Shadwick.masterDetail(" + JSONObject.quote(getMarkupId()) + ","
                + JSONObject.quote(showingDetail ? "detail" : "master") + ")");
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        tag.append("class", "sw-master-detail", " ");
        tag.put("data-showing", showingDetail ? "detail" : "master");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
        response.render(org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }

    @Override
    protected void onDetach() {
        selection.detach();
        super.onDetach();
    }
}
