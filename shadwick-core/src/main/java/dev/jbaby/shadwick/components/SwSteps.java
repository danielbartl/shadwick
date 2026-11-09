package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.util.SerializableBiConsumer;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An ordered process indicator. Indices before the zero-based current index are completed; later
 * indices are upcoming. Null/negative current indices mean not started, and an index past the end
 * means all completed. Optional selection acts only on completed steps and rechecks eligibility.
 * The application owns validation, navigation and updating the models/Ajax target. Without script,
 * actions become plain status text. Titles, state captions and accessible labels are escaped.
 */
public class SwSteps extends Panel {

    private final IModel<? extends List<String>> titles;
    private final IModel<Integer> current;
    private final LoadableDetachableModel<List<String>> loadedTitles;
    private final LoadableDetachableModel<Integer> loadedCurrent;
    private IModel<String> label;
    private SerializableBiConsumer<AjaxRequestTarget, Integer> onSelect;

    public SwSteps(String id, List<String> titles, IModel<Integer> current) {
        this(id, Model.ofList(titles), current);
    }

    public SwSteps(String id, IModel<? extends List<String>> titles, IModel<Integer> current) {
        super(id);
        this.titles = Objects.requireNonNull(titles);
        this.current = Objects.requireNonNull(current);
        setOutputMarkupId(true);
        loadedTitles =
                new LoadableDetachableModel<>() {

                    @Override
                    protected List<String> load() {
                        List<String> values = SwSteps.this.titles.getObject();
                        return values == null ? List.of() : new ArrayList<>(values);
                    }
                };
        loadedCurrent = LoadableDetachableModel.of(current::getObject);
        addSteps();
    }

    private void addSteps() {
        var list = new WebMarkupContainer("list");
        list.add(
                AttributeModifier.replace(
                        "aria-label",
                        (IModel<String>)
                                () -> {
                                    String value = label == null ? null : label.getObject();
                                    return value == null || value.isBlank()
                                            ? getString("SwSteps.label")
                                            : value;
                                }));
        add(list);
        list.add(
                new ListView<String>("items", loadedTitles) {

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        populateStep(item);
                    }
                });
    }

    private void populateStep(ListItem<String> item) {
        int index = item.getIndex();
        String title = item.getModelObject();
        item.add(AttributeModifier.replace("data-state", (IModel<String>) () -> state(index)));
        item.add(
                AttributeModifier.replace(
                        "aria-current",
                        (IModel<String>) () -> index == currentStep() ? "step" : null));
        AjaxLink<Void> link = stepAction(index, title);
        item.add(link);
        WebMarkupContainer fallback = stepFallback(index, title);
        item.add(fallback);
    }

    private String state(int index) {
        int now = currentStep();
        return index < now ? "done" : index == now ? "current" : "upcoming";
    }

    private int currentStep() {
        Integer value = loadedCurrent.getObject();
        return value == null ? -1 : value;
    }

    private AjaxLink<Void> stepAction(int index, String title) {
        var link =
                new AjaxLink<Void>("link") {

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        setEnabled(selectable(index, title));
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (!isEnabledInHierarchy()) tag.setName("span");
                        else tag.append("class", "sw-step-action", " ");
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // A previously rendered URL must not select a renamed, removed or
                        // now-future step.
                        if (isEnabledInHierarchy() && selectable(index, title)) {
                            try {
                                onSelect.accept(target, index);
                            } finally {
                                clearSnapshots();
                            }
                        }
                    }
                };
        addParts(link, index, title);
        return link;
    }

    private WebMarkupContainer stepFallback(int index, String title) {
        var fallback =
                new WebMarkupContainer("fallback") {

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        setVisible(SwSteps.this.isEnabledInHierarchy() && selectable(index, title));
                    }
                };
        addParts(fallback, index, title);
        return fallback;
    }

    private boolean selectable(int index, String renderedTitle) {
        List<String> values = loadedTitles.getObject();
        return onSelect != null
                && index < currentStep()
                && index < values.size()
                && Objects.equals(renderedTitle, values.get(index));
    }

    private void clearSnapshots() {
        loadedTitles.detach();
        loadedCurrent.detach();
    }

    private void addParts(WebMarkupContainer parent, int index, String title) {
        parent.add(new Label("number", Integer.toString(index + 1)));
        parent.add(new Label("title", title));
        parent.add(
                new Label("status", (IModel<String>) () -> getString("SwSteps." + state(index))));
    }

    /** Null/blank labels use the localized default, "Steps". */
    public SwSteps setLabel(String label) {
        return setLabel(Model.of(label));
    }

    public SwSteps setLabel(IModel<String> label) {
        this.label = label;
        return this;
    }

    /**
     * Enables Ajax selection of completed steps; null removes the action. Current/future steps
     * remain text.
     */
    public SwSteps setOnSelect(SerializableBiConsumer<AjaxRequestTarget, Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
    }

    @Override
    protected void onDetach() {
        try {
            titles.detach();
            current.detach();
            if (label != null) label.detach();
        } finally {
            clearSnapshots();
            super.onDetach();
        }
    }
}
