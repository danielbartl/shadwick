package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.TimelineEventBehavior;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** A record's native description list and ordered history, with actual model and Ajax updates. */
public class RecordDisplayPage extends BasePage {

    public static class Customer implements Serializable {

        public String name = "Fern & Field";
        public String taxId;
        public LocalDate since = LocalDate.of(2019, 3, 1);
        public BigDecimal total = new BigDecimal("48250.1254");
        public String email = "jordan@example.com";
    }

    public record Event(
            String title, LocalDateTime at, String detail, TimelineEventBehavior.Variant variant)
            implements Serializable {}

    private final Model<Customer> record = Model.of(new Customer());
    private final IModel<List<Event>> events = Model.ofList(seed());
    private final Model<String> message =
            Model.of("Edit the customer or record a payment to update this view.");
    private int updates;

    public RecordDisplayPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        SwDescriptionList details = details();
        add(details);
        var compact =
                new SwDescriptionList("compact", record).addProperty("name").addProperty("total");
        add(compact);
        WebMarkupContainer activity = activity();
        add(activity);
        var receipt = new Label("receipt", message);
        receipt.setOutputMarkupId(true);
        add(receipt);
        addRecordActions(details, compact, activity, receipt);
    }

    private SwDescriptionList details() {
        var details =
                new SwDescriptionList("details", record)
                        .addProperty("name")
                        .addProperty("taxId")
                        .addProperty("since")
                        .addProperty("total")
                        .addItem(Model.of("Status"), id -> new SwBadge(id, "Active"))
                        .addItem(
                                Model.of("Contact"),
                                id -> new ContactLink(id, new PropertyModel<>(record, "email")));
        return details;
    }

    private WebMarkupContainer activity() {
        var activity = new WebMarkupContainer("activity");
        activity.setOutputMarkupId(true);
        activity.add(
                new SwTimeline<>("timeline", events, Event::title, Event::at)
                        .setDescription(Event::detail)
                        .setVariant(Event::variant));
        activity.add(
                new SwEmptyState(
                        "empty",
                        "No activity yet",
                        "Record a payment or edit the customer to add an event.") {

                    @Override
                    protected void onConfigure() {
                        super.onConfigure();
                        setVisible(events.getObject().isEmpty());
                    }
                });
        return activity;
    }

    private void addRecordActions(
            SwDescriptionList details,
            SwDescriptionList compact,
            WebMarkupContainer activity,
            Label receipt) {
        var form =
                new Form<Void>("form") {

                    @Override
                    protected void onSubmit() {
                        append(
                                "Customer updated",
                                "Name changed to " + record.getObject().name,
                                TimelineEventBehavior.Variant.OUTLINE);
                        message.setObject("Saved customer " + record.getObject().name + ".");
                    }
                };
        form.add(
                new SwTextField<>(
                                "name", "Customer name", new PropertyModel<String>(record, "name"))
                        .setRequired(true));
        form.add(new Button("normalSave").add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
        form.add(
                SwSubmitButton.onSubmit(
                        "save",
                        "Save with Ajax",
                        target -> target.add(details, compact, activity, receipt)));
        add(form);
        add(
                SwButton.onClick(
                        "payment",
                        "Record payment",
                        target -> {
                            record.getObject().total =
                                    record.getObject().total.add(new BigDecimal("100"));
                            append(
                                    "Payment received",
                                    "An additional 100 has been recorded.",
                                    TimelineEventBehavior.Variant.DEFAULT);
                            message.setObject("Recorded payment 100.");
                            target.add(details, compact, activity, receipt);
                        }));
        add(
                SwButton.onClick(
                                "clear",
                                "Clear history",
                                target -> {
                                    events.setObject(new ArrayList<>());
                                    message.setObject("History cleared.");
                                    target.add(activity, receipt);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
        add(
                SwButton.onClick(
                                "restore",
                                "Restore sample history",
                                target -> {
                                    events.setObject(seed());
                                    message.setObject("Sample history restored.");
                                    target.add(activity, receipt);
                                })
                        .setVariant(ButtonBehavior.Variant.OUTLINE));
    }

    private void append(String title, String detail, TimelineEventBehavior.Variant variant) {
        var current = new ArrayList<>(events.getObject());
        current.add(
                new Event(
                        title,
                        LocalDateTime.of(2026, 11, 8, 15, 0).plusMinutes(++updates),
                        detail,
                        variant));
        events.setObject(current);
    }

    private static ArrayList<Event> seed() {
        return new ArrayList<>(
                List.of(
                        new Event(
                                "Customer created",
                                LocalDateTime.of(2026, 11, 1, 9, 30),
                                "Jordan opened the account.",
                                TimelineEventBehavior.Variant.OUTLINE),
                        new Event(
                                "Proposal reviewed",
                                LocalDateTime.of(2026, 11, 3, 14, 5),
                                "Scope and pricing were approved.",
                                TimelineEventBehavior.Variant.SECONDARY),
                        new Event(
                                "Delivery failed",
                                LocalDateTime.of(2026, 11, 4, 11, 0),
                                "The first delivery address was unavailable.",
                                TimelineEventBehavior.Variant.DESTRUCTIVE),
                        new Event(
                                "Payment received",
                                LocalDateTime.of(2026, 11, 5, 10, 15),
                                "The deposit has been received.",
                                TimelineEventBehavior.Variant.DEFAULT),
                        new Event(
                                "Account note",
                                null,
                                "A note without a timestamp.",
                                TimelineEventBehavior.Variant.OUTLINE)));
    }

    /** A composed value can contain native links and application-owned markup. */
    public static class ContactLink extends Panel {

        public ContactLink(String id, IModel<String> email) {
            super(id, email);
            this.email = email;
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            add(
                    new ExternalLink(
                            "email", (IModel<String>) () -> "mailto:" + email.getObject(), email));
        }

        private final IModel<String> email;
    }

    @Override
    protected String getTitle() {
        return "Details & activity";
    }
}
