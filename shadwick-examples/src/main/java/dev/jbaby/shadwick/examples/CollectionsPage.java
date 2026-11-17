package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.SwCrud;
import dev.jbaby.shadwick.components.SwLoadMoreList;
import dev.jbaby.shadwick.components.form.SwEmailField;
import dev.jbaby.shadwick.components.form.SwSearchField;
import dev.jbaby.shadwick.components.form.SwSwitch;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/** An activity feed that loads more as you ask (or scroll), and a customer list to create, edit and delete in. */
public class CollectionsPage extends BasePage {

    public record Event(int number, String time, String text) implements Serializable {}

    private static final String[] WHAT = {
            "Fern & Field paid invoice CLV-24%02d", "Ada added a task to Northwind's launch",
            "Lantern's brief was approved", "Grace moved the Acme Studio review to Friday",
            "A new comment on the Harbor redesign", "Jordan uploaded %d files to the shared folder"};

    /** 134 made-up events, newest first. */
    static final List<Event> EVENTS = IntStream.rangeClosed(1, 134)
            .mapToObj(n -> new Event(n, String.format(Locale.ROOT, "%d:%02d", 17 - n / 12 % 9, 59 - n * 7 % 60),
                    String.format(Locale.ROOT, WHAT[n % WHAT.length], n % 40 + 2)))
            .toList();

    /** A customer; the editor gets a copy, so a cancelled or rejected edit changes nothing here. */
    public static class Customer implements Serializable {
        public int id;
        public String name, email, city;

        Customer(int id, String name, String email, String city) {
            this.id = id; this.name = name; this.email = email; this.city = city;
        }

        Customer copy() { return new Customer(id, name, email, city); }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getCity() { return city; }
    }

    private final List<Customer> customers = new ArrayList<>(List.of(
            new Customer(1, "Acme Studio", "hello@acme.example", "Vienna"),
            new Customer(2, "Fern & Field", "accounts@fernfield.example", "Graz"),
            new Customer(3, "Globex", "ops@globex.example", "Berlin"),
            new Customer(4, "Lantern", "team@lantern.example", "Lisbon"),
            new Customer(5, "Northwind", "billing@northwind.example", "Zagreb")));
    private final Model<String> query = Model.of("");
    private final Model<Boolean> loadOnScroll = Model.of(false);

    public CollectionsPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addFeed();
        addCustomers();
    }

    private void addFeed() {
        SwLoadMoreList<Event> feed = new SwLoadMoreList<>("feed", new ListDataProvider<>(EVENTS), 20,
                (id, event) -> {
                    Fragment row = new Fragment(id, "eventRow", this, event);
                    row.add(new Label("time", event.map(Event::time)), new Label("text", event.map(Event::text)));
                    return row;
                }).setLabel("Recent activity");
        add(feed);
        Form<Void> options = new Form<>("feedOptions");
        add(options);
        options.add(new SwSwitch("scroll", "Load as you scroll", loadOnScroll)
                .add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        feed.setLoadOnScroll(Boolean.TRUE.equals(loadOnScroll.getObject()));
                        feed.reset(target);
                    }
                }));
    }

    private void addCustomers() {
        List<IColumn<Customer, String>> columns = List.of(
                new LambdaColumn<>(Model.of("Name"), "name", Customer::getName),
                new LambdaColumn<>(Model.of("Email"), Customer::getEmail),
                new LambdaColumn<>(Model.of("City"), "city", Customer::getCity));
        SwCrud<Customer, String> crud = new SwCrud<>("customers", columns, new CustomerProvider(), 10)
                .setEditor(CustomerFields::new)
                .setNewItem(() -> new Customer(0, null, null, null))
                .setTitle(Customer::getName)
                .setRowKey(customer -> customer.id);
        crud.setOnSave((target, customer) -> save(crud, customer))
                .setOnDelete((target, customer) -> customers.removeIf(c -> c.id == customer.id))
                .setSearch(id -> search(id, crud));
        crud.getDataTable().setLabel("Customers");
        add(crud);
    }

    private void save(SwCrud<Customer, String> crud, Customer customer) {
        boolean taken = customers.stream().anyMatch(c -> c.id != customer.id && c.name.equalsIgnoreCase(customer.name));
        if (taken) {
            // A check only the server can make: shown at the field, and the dialog stays open
            var name = (SwTextField<?>) crud.getEditor().getForm().get("content:content:name");
            name.getInput().error("There is already a customer called " + customer.name + ".");
            return;
        }
        if (customer.id == 0) {
            customer.id = customers.stream().mapToInt(c -> c.id).max().orElse(0) + 1;
            customers.add(customer.copy());
        } else {
            customers.replaceAll(c -> c.id == customer.id ? customer.copy() : c);
        }
    }

    private Fragment search(String id, SwCrud<Customer, String> crud) {
        Fragment search = new Fragment(id, "searchForm", this);
        Form<Void> form = new Form<>("form");
        search.add(form);
        form.add(new SwSearchField("q", "Search customers", query).add(new AjaxFormComponentUpdatingBehavior("input") {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ofMillis(250), true));
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                crud.refresh(target);
            }
        }));
        return search;
    }

    @Override
    protected String getTitle() {
        return "Load more & CRUD";
    }

    /** The customers matching the search, sorted; each loaded by id as a copy for editing. */
    private class CustomerProvider extends SortableDataProvider<Customer, String> {
        CustomerProvider() {
            setSort("name", SortOrder.ASCENDING);
        }

        private List<Customer> matching() {
            String q = query.getObject() == null ? "" : query.getObject().trim().toLowerCase(Locale.ROOT);
            Comparator<Customer> order = Comparator.comparing(
                    c -> ("city".equals(getSort().getProperty()) ? c.city : c.name).toLowerCase(Locale.ROOT));
            return customers.stream()
                    .filter(c -> q.isEmpty() || (c.name + " " + c.email + " " + c.city).toLowerCase(Locale.ROOT).contains(q))
                    .sorted(getSort().isAscending() ? order : order.reversed())
                    .toList();
        }

        @Override
        public Iterator<? extends Customer> iterator(long first, long count) {
            return matching().stream().skip(first).limit(count).iterator();
        }

        @Override
        public long size() {
            return matching().size();
        }

        @Override
        public IModel<Customer> model(Customer customer) {
            int id = customer.id;
            return new LoadableDetachableModel<>() {
                @Override
                protected Customer load() {
                    return customers.stream().filter(c -> c.id == id).findFirst().map(Customer::copy).orElse(null);
                }
            };
        }
    }

    /** The editor: fields named after the customer's properties, on its CompoundPropertyModel. */
    public static class CustomerFields extends Panel {
        public CustomerFields(String id, IModel<Customer> customer) {
            super(id, customer);
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            add(new SwTextField<String>("name", "Name").setRequired(true));
            add(new SwEmailField("email", "Email"));
            add(new SwTextField<String>("city", "City"));
        }
    }
}
