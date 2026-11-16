package dev.jbaby.shadwick.components;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.components.form.NotShownInlineFilter;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.visit.IVisit;

import java.util.Comparator;
import java.util.Objects;

/**
 * The errors of one form, shown together as a destructive alert with a title, following Shadleaf's form errors and
 * the GOV.UK error summary. By default ({@link Show#GLOBAL}) it lists the errors no field shows next to itself: those of
 * the form as a whole, such as two passwords that don't match or a service that refused the request
 * ({@code form.error(...)}). With {@link Show#ALL} it lists every field's error too, after those, while each field still
 * shows its own. A message from a field becomes a link to its control. It shows nothing while there are no errors.
 *
 * <pre>
 * form.add(new SwFormErrors("errors", form).setShow(SwFormErrors.Show.ALL).setAutofocus(true));
 * </pre>
 *
 * It is not announced as an alert: after a full page load such an announcement is unreliable, and it would talk over
 * the page. {@link #setAutofocus(boolean)} moves the focus to it when the form comes back with errors (or, when it has
 * nothing to show, to the first control with an error), which also has a screen reader read it. As a
 * {@link SwFeedbackPanel} with the form as its fence, it takes these messages from other feedback panels and toasts,
 * and adds itself to every Ajax response that changes what it shows.
 */
public class SwFormErrors extends SwFeedbackPanel {

    /** Which errors to list. */
    public enum Show {
        /** The errors no Shadwick field shows next to itself: the form's own, and those of other components. */
        GLOBAL,
        /** Every error in the form: the form's own first, then each field's, in the order they were reported. */
        ALL
    }

    private final Form<?> form;
    private final MarkupContainer alert;
    private final Label title;
    private IModel<String> titleModel;
    private Show show = Show.GLOBAL;
    private Integer headingLevel;
    private boolean autofocus;

    public SwFormErrors(String id, Form<?> form) {
        super(id, Objects.requireNonNull(form, "form"), new NotShownInlineFilter());
        this.form = form;
        // Errors of the form as a whole come first; the others keep their order (the sort is stable)
        setSortingComparator(Comparator.comparingInt(message -> message.getReporter() instanceof FormComponent ? 1 : 0));
        alert = (MarkupContainer) get("feedbackul");
        alert.setOutputMarkupId(true);
        alert.add(AttributeModifier.replace("autofocus", (IModel<String>) () -> autofocus ? "autofocus" : null));
        title = new Label("title", (IModel<String>) this::titleText);
        title.add(AttributeModifier.replace("role", (IModel<String>) () -> headingLevel != null ? "heading" : null));
        title.add(AttributeModifier.replace("aria-level", (IModel<String>) () -> headingLevel != null ? headingLevel.toString() : null));
        alert.add(title);
    }

    /** Which errors to list; {@link Show#GLOBAL} unless set. */
    public SwFormErrors setShow(Show show) {
        this.show = Objects.requireNonNull(show, "show");
        setFilter(show == Show.ALL ? null : new NotShownInlineFilter());
        return this;
    }

    public Show getShow() { return show; }

    /** The title above the messages; by default the resource {@code SwFormErrors.title}, "There is a problem". */
    public SwFormErrors setTitle(String title) { return setTitle(Model.of(title)); }

    public SwFormErrors setTitle(IModel<String> title) {
        this.titleModel = title;
        return this;
    }

    /**
     * Makes the title a heading of this level (1 to 6), which screen reader users can jump to; {@code null}, the
     * default, keeps it plain text.
     */
    public SwFormErrors setHeadingLevel(Integer level) {
        if (level != null && (level < 1 || level > 6)) {
            throw new IllegalArgumentException("A heading level is 1 to 6, not " + level);
        }
        this.headingLevel = level;
        return this;
    }

    /**
     * Moves the focus to the summary when the form comes back with errors, or to the first control with an error when
     * the summary has nothing to show. Off by default: moving the focus on load skips past the page's heading, so it
     * suits a form that is the page's only task, or one submitted over Ajax.
     */
    public SwFormErrors setAutofocus(boolean autofocus) {
        this.autofocus = autofocus;
        return this;
    }

    private String titleText() {
        if (titleModel != null && titleModel.getObject() != null && !titleModel.getObject().isBlank()) {
            return titleModel.getObject();
        }
        return new StringResourceModel("SwFormErrors.title", this).setDefaultValue("There is a problem").getString();
    }

    @Override
    protected ListItem<FeedbackMessage> newMessageItem(int index, IModel<FeedbackMessage> itemModel) {
        // A plain list item: the alert is the summary itself, not each message as in SwFeedbackPanel
        return new ListItem<>(index, itemModel);
    }

    @Override
    protected String getCSSClass(FeedbackMessage message) {
        return "sw-form-errors-item";
    }

    @Override
    protected Component newMessageDisplayComponent(String id, FeedbackMessage message) {
        String text = Strings.escapeMarkup(String.valueOf(message.getMessage())).toString();
        Component reporter = message.getReporter();
        String html = reporter instanceof FormComponent<?> control && control.getOutputMarkupId()
                ? "<a href=\"#" + Strings.escapeMarkup(control.getMarkupId()) + "\">" + text + "</a>"
                : text;
        return new Label(id, html).setEscapeModelStrings(false);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // Nothing to show here, but the form has invalid controls: the first of them takes the focus
        if (autofocus && !anyMessage()) {
            FormComponent<?> control = firstInvalidControl();
            if (control != null) {
                response.render(OnDomReadyHeaderItem.forScript("var c=document.getElementById("
                        + JSONObject.quote(control.getMarkupId()) + ");if(c)c.focus();"));
            }
        }
    }

    @Override
    public void onEvent(IEvent<?> event) {
        super.onEvent(event);
        if (autofocus && event.getPayload() instanceof AjaxRequestTarget target && form.isSubmitted() && isVisibleInHierarchy()) {
            if (anyMessage()) {
                target.focusComponent(alert);
            } else {
                FormComponent<?> control = firstInvalidControl();
                if (control != null) target.focusComponent(control);
            }
        }
    }

    private FormComponent<?> firstInvalidControl() {
        return form.visitChildren(FormComponent.class, (FormComponent<?> control, IVisit<FormComponent<?>> visit) -> {
            if (!control.isValid() && control.isVisibleInHierarchy() && control.getOutputMarkupId()) {
                visit.stop(control);
            }
        });
    }
}
