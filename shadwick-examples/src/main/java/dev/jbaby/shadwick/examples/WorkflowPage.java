package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.SwBadge;
import dev.jbaby.shadwick.components.SwFilter;
import dev.jbaby.shadwick.components.SwMasterDetail;
import dev.jbaby.shadwick.components.SwWizard;
import dev.jbaby.shadwick.components.form.SwEmailField;
import dev.jbaby.shadwick.components.form.SwTextArea;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/** A multi-step sign-up, and a support inbox: a status filter above tickets beside the selected one. */
public class WorkflowPage extends BasePage {

    /** What the sign-up collects, shared by all its steps. */
    public static class Signup implements Serializable {
        public String email, name, team, about;
    }

    public enum Status { OPEN, WAITING, CLOSED }

    public record Ticket(int number, String subject, String customer, Status status, String body) implements Serializable {}

    private static final List<Ticket> TICKETS = List.of(
            new Ticket(1042, "Invoice shows the wrong address", "Fern & Field", Status.OPEN,
                    "The March invoice still has our old address. Could you send a corrected one?"),
            new Ticket(1041, "Add a second admin", "Northwind", Status.WAITING,
                    "Please give Jordan admin rights as well. Waiting for the customer to confirm the email."),
            new Ticket(1039, "Export to CSV", "Lantern", Status.OPEN,
                    "Is there a way to export the project list with its budgets?"),
            new Ticket(1035, "Login page in German", "Acme Studio", Status.CLOSED,
                    "Resolved: the browser language now picks the German sign-in page."));

    private final Signup signup = new Signup();
    private final Model<Status> status = Model.of();

    public WorkflowPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addSignup();
        addInbox();
    }

    private void addSignup() {
        Label welcome = new Label("welcome", Model.of(""));
        welcome.setOutputMarkupId(true);
        add(welcome);
        SwWizard wizard = new SwWizard("signup") {
            @Override
            protected void onNext(int step, AjaxRequestTarget target) {
                // A check only the server can make: keeps the user on the step, with the error in the summary
                if (step == 0 && "taken@example.com".equalsIgnoreCase(signup.email)) {
                    getForm().error("An account with that email already exists. Sign in instead, or use another email.");
                }
            }

            @Override
            protected void onFinish(AjaxRequestTarget target) {
                welcome.setDefaultModelObject("Welcome, " + signup.name + "! Your workspace for " + signup.team + " is ready.");
                if (target != null) target.add(welcome);
            }
        };
        wizard.addStep("Account", id -> new SwEmailField(id, "Work email", new PropertyModel<>(signup, "email"))
                        .setDescription("Try taken@example.com to see a check only the server can make.").setRequired(true))
                .addStep("Profile", this::profileStep)
                .addStep("Review", this::reviewStep);
        add(wizard);
    }

    private Fragment profileStep(String id) {
        Fragment step = new Fragment(id, "profileStep", this);
        step.add(new SwTextField<>("name", "Your name", new PropertyModel<String>(signup, "name")).setRequired(true));
        step.add(new SwTextField<>("team", "Team name", new PropertyModel<String>(signup, "team")).setRequired(true));
        step.add(new SwTextArea<>("about", "What will you use it for?", new PropertyModel<String>(signup, "about")));
        return step;
    }

    private Fragment reviewStep(String id) {
        Fragment step = new Fragment(id, "reviewStep", this);
        step.add(new Label("email", (IModel<String>) () -> signup.email));
        step.add(new Label("name", (IModel<String>) () -> signup.name));
        step.add(new Label("team", (IModel<String>) () -> signup.team));
        return step;
    }

    private void addInbox() {
        SwMasterDetail<Ticket> inbox = new SwMasterDetail<>("inbox", new Model<>());
        WebMarkupContainer[] list = new WebMarkupContainer[1];
        inbox.setMaster(id -> {
            Fragment master = new Fragment(id, "ticketList", this);
            master.setOutputMarkupId(true);
            // Reused items keep their ids when the list re-renders to mark the selection, so the focus stays on the link
            master.add(new ListView<>("tickets", (IModel<List<Ticket>>) () -> TICKETS.stream()
                    .filter(ticket -> status.getObject() == null || ticket.status() == status.getObject()).toList()) {
                {
                    setReuseItems(true);
                }

                // A reused item isn't populated again: everything reads the ticket through the item's model
                @Override
                protected void populateItem(ListItem<Ticket> item) {
                    AjaxFallbackLink<Void> link = new AjaxFallbackLink<>("link") {
                        @Override
                        public void onClick(Optional<AjaxRequestTarget> target) {
                            if (target.isPresent()) {
                                inbox.select(target.get(), item.getModelObject());
                                target.get().add(list[0]); // marks the selection
                            } else {
                                inbox.select(item.getModelObject());
                            }
                        }
                    };
                    link.setOutputMarkupId(true);
                    link.add(AttributeModifier.replace("aria-current",
                            (IModel<String>) () -> inbox.isSelected(item.getModelObject()) ? "true" : null));
                    link.add(new Label("subject", item.getModel().map(Ticket::subject)),
                            new Label("customer", item.getModel().map(Ticket::customer)));
                    item.add(link);
                }
            });
            list[0] = master;
            return master;
        });
        inbox.setDetail((id, selected) -> {
            Fragment detail = new Fragment(id, "ticketDetail", this);
            Ticket ticket = selected.getObject();
            detail.add(new Label("subject", ticket.subject()), new Label("number", "#" + ticket.number() + " · " + ticket.customer()),
                    new SwBadge("status", ticket.status().name().charAt(0) + ticket.status().name().substring(1).toLowerCase()),
                    new Label("body", ticket.body()));
            return detail;
        });
        add(SwFilter.onChange("status", status, Model.ofList(List.of(Status.values())), target -> target.add(list[0]))
                .setChoiceRenderer(new EnumChoiceRenderer<>(this))
                .setNullValid(true)
                .setLabel("Ticket status"));
        add(inbox);
    }

    @Override
    protected String getTitle() {
        return "Wizard, filter & master–detail";
    }
}
