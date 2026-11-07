package dev.jbaby.shadwick.examples;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Application data for the interactive demo. Never shared across visitors. */
public final class StudioWorkspace implements Serializable {
    private static final MetaDataKey<StudioWorkspace> KEY = new MetaDataKey<>() {};
    public final UUID generation = UUID.randomUUID();
    public final LocalDate today;
    private final List<Project> projects = new ArrayList<>();
    private final List<Invoice> invoices = new ArrayList<>();
    private final List<Activity> activity = new ArrayList<>();
    private long nextId = 100;

    public record Activity(String key, List<String> arguments) implements Serializable {
        public Activity { arguments = List.copyOf(arguments); }
    }
    public interface MessageFailure { String key(); }
    public static final class InvalidDetails extends IllegalArgumentException implements MessageFailure {
        private final String key;
        InvalidDetails(String key, String message) { super(message); this.key = key; }
        @Override public String key() { return key; }
    }
    public static final class Conflict extends IllegalStateException implements MessageFailure {
        private final String key;
        Conflict(String key, String message) { super(message); this.key = key; }
        Conflict(String key, String message, Throwable cause) { super(message, cause); this.key = key; }
        @Override public String key() { return key; }
    }
    public record Task(long id, String title, String owner, int hours, boolean done) implements Serializable {}
    public record Milestone(String title, LocalDate date) implements Serializable {}
    public record Project(long id, long revision, String name, String client, String category, LocalDate due,
                          long budgetCents, List<Task> tasks, boolean handedOff) implements Serializable {
        public Project { tasks = List.copyOf(tasks); }
        public long completed() { return tasks.stream().filter(Task::done).count(); }
        public int progress() { return tasks.isEmpty() ? 0 : (int) (100 * completed() / tasks.size()); }
        public int hours() { return tasks.stream().mapToInt(Task::hours).sum(); }
        public String status() { return handedOff ? "Delivered" : progress() == 100 ? "Ready for handoff" : "In progress"; }
        public List<Milestone> milestones() {
            return List.of(new Milestone("Creative direction", due.minusDays(12)), new Milestone("Design review", due.minusDays(5)), new Milestone("Client handoff", due));
        }
    }
    public record Invoice(long id, long projectId, String number, String description, long cents,
                          LocalDate due, LocalDate paidOn) implements Serializable {
        public boolean paid() { return paidOn != null; }
        public String status(LocalDate today) { return paid() ? "Paid" : due.isBefore(today) ? "Overdue" : "Awaiting payment"; }
    }

    public static StudioWorkspace get() {
        Session session = Session.get();
        StudioWorkspace workspace = session.getMetaData(KEY);
        if (workspace == null) { session.bind(); workspace = new StudioWorkspace(LocalDate.now()); session.setMetaData(KEY, workspace); }
        return workspace;
    }
    public static void reset() { Session.get().setMetaData(KEY, new StudioWorkspace(LocalDate.now())); }
    public static void changed() { Session.get().dirty(); }

    public StudioWorkspace(LocalDate today) {
        this.today = today;
        projects.add(new Project(1, 0, "A little closer to nature.", "Fern & Field", "Brand & website", today.plusDays(7), 1240000,
                List.of(new Task(11, "Define the visual direction", "Aoife", 12, true), new Task(12, "Design the core pages", "Cian", 24, true),
                        new Task(13, "Review the mobile experience", "Aoife", 8, false), new Task(14, "Prepare the brand guidelines", "Nora", 6, false)), false));
        projects.add(new Project(2, 0, "A fresh perspective.", "Forma Architects", "Digital experience", today.plusDays(14), 860000,
                List.of(new Task(21, "Map the visitor journey", "Nora", 10, true), new Task(22, "Explore three directions", "Cian", 16, false),
                        new Task(23, "Build the design system", "Aoife", 20, false)), false));
        projects.add(new Project(3, 0, "Made for slow mornings.", "Sunday Coffee", "Brand identity", today.plusDays(21), 480000,
                List.of(new Task(31, "Run the discovery workshop", "Aoife", 8, true), new Task(32, "Explore the identity", "Nora", 18, false),
                        new Task(33, "Design the packaging", "Cian", 12, false), new Task(34, "Document the guidelines", "Nora", 6, false)), false));
        projects.add(new Project(4, 0, "Good things, together.", "Common Ground", "Brand identity", today.minusDays(4), 620000,
                List.of(new Task(41, "Create the identity", "Aoife", 22, true), new Task(42, "Prepare the final artwork", "Nora", 10, true)), true));
        invoices.add(new Invoice(1, 1, "CLV-2401", "Discovery & design deposit", 620000, today.minusDays(20), today.minusDays(22)));
        invoices.add(new Invoice(2, 1, "CLV-2402", "Final design & client handoff", 620000, today.plusDays(14), null));
        invoices.add(new Invoice(3, 2, "CLV-2403", "Digital experience deposit", 430000, today.minusDays(3), null));
        invoices.add(new Invoice(4, 3, "CLV-2404", "Brand identity deposit", 240000, today.plusDays(10), null));
        invoices.add(new Invoice(5, 4, "CLV-2405", "Identity & final artwork", 620000, today.minusDays(1), null));
        activity.addAll(List.of(new Activity("activity.seedArtwork", List.of()), new Activity("activity.seedPages", List.of()), new Activity("activity.seedDiscovery", List.of())));
    }

    public synchronized List<Project> projects() { return List.copyOf(projects); }
    public synchronized List<Invoice> invoices() { return List.copyOf(invoices); }
    public synchronized List<Activity> activity() { return List.copyOf(activity); }
    public synchronized Project project(long id) { return projects.stream().filter(p -> p.id == id).findFirst().orElse(null); }
    public synchronized Invoice invoice(long id) { return invoices.stream().filter(i -> i.id == id).findFirst().orElse(null); }
    public synchronized long outstanding() { return invoices.stream().filter(i -> !i.paid()).mapToLong(Invoice::cents).sum(); }
    public synchronized long collected() { return invoices.stream().filter(Invoice::paid).mapToLong(Invoice::cents).sum(); }
    public synchronized long active() { return projects.stream().filter(p -> !p.handedOff).count(); }

    public synchronized Project saveProject(Long id, long revision, String name, String client, String category, LocalDate due, long budget) {
        if (name == null || name.isBlank() || name.length() > 100 || client == null || client.isBlank() || client.length() > 80
                || category == null || category.isBlank() || category.length() > 80 || due == null || budget <= 0 || budget > 100000000)
            throw new InvalidDetails("error.details", "Check the project details and enter a positive budget.");
        if (id == null && projects.size() >= 30) throw new Conflict("error.projectLimit", "This demo supports up to 30 projects. Reset sample data to start fresh.");
        Project old = id == null ? null : project(id);
        if (id != null && (old == null || old.revision != revision || old.handedOff)) throw new Conflict("error.projectChanged", "This project changed. Refresh before editing it.");
        long key = id == null ? nextId++ : id;
        Project saved = new Project(key, old == null ? 0 : old.revision + 1, name.strip(), client.strip(), category.strip(), due, budget,
                old == null ? List.of() : old.tasks, false);
        if (old != null) projects.remove(old);
        projects.add(saved);
        log(old == null ? "activity.created" : "activity.updated", saved.client, saved.name);
        return saved;
    }

    public synchronized void addTask(long projectId, String title, String owner, int hours) {
        Project p = editable(projectId);
        if (title == null || title.isBlank() || title.length() > 120 || owner == null || !List.of("Aoife", "Cian", "Nora").contains(owner) || hours < 1 || hours > 160)
            throw new InvalidDetails("error.taskDetails", "Enter a task, team member and an estimate from 1 to 160 hours.");
        if (p.tasks.size() >= 50) throw new Conflict("error.taskLimit", "This demo supports up to 50 tasks per project.");
        List<Task> tasks = new ArrayList<>(p.tasks); tasks.add(new Task(nextId++, title.strip(), owner, hours, false));
        replace(p, tasks, false); log("activity.addedTask", p.client);
    }

    public synchronized void completeTask(long projectId, long taskId, boolean expectedDone) {
        Project p = editable(projectId);
        Task old = p.tasks.stream().filter(t -> t.id == taskId).findFirst().orElseThrow(() -> new Conflict("error.taskMissing", "This task is no longer available."));
        if (old.done != expectedDone) throw new Conflict("error.taskChanged", "This task changed in another tab. The latest version is now shown.");
        replace(p, p.tasks.stream().map(t -> t.id == taskId ? new Task(t.id, t.title, t.owner, t.hours, !t.done) : t).toList(), false);
        log(old.done ? "activity.reopened" : "activity.completed", old.title, p.client);
    }

    public synchronized void handoff(long projectId) {
        Project p = editable(projectId);
        if (p.tasks.isEmpty() || p.progress() != 100) throw new Conflict("error.handoffIncomplete", "Complete every task before preparing the client handoff.");
        replace(p, p.tasks, true); log("activity.handoff", p.client);
    }

    public synchronized boolean pay(long invoiceId) {
        Invoice old = invoice(invoiceId);
        if (old == null) throw new Conflict("error.invoiceMissing", "This invoice is no longer available.");
        if (old.paid()) return false;
        invoices.set(invoices.indexOf(old), new Invoice(old.id, old.projectId, old.number, old.description, old.cents, old.due, today));
        log("activity.paid", old.number, project(old.projectId).client); return true;
    }

    /** A real ZIP of the current scope and completed checklist, without external file services. */
    public synchronized byte[] handoffZip(long projectId) {
        Project p = project(projectId);
        if (p == null || !p.handedOff) throw new Conflict("error.handoffRequired", "Prepare the client handoff before downloading it.");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                entry(zip, "README.md", "# " + p.client + "\n\n" + p.name + "\n\n" + p.category + " · " + p.hours() + " estimated hours\n\nPrepared in Clover Studio. This is a sample client handoff.\n");
                entry(zip, "completed-work.md", "# Completed work\n\n" + String.join("\n", p.tasks.stream().map(t -> "- [x] " + t.title + " · " + t.owner + " · " + t.hours + "h").toList()));
                entry(zip, "next-steps.md", "# Your next steps\n\nReview the completed scope with your team. Archive this handoff with your project records.\n");
            }
            return bytes.toByteArray();
        } catch (IOException e) { throw new Conflict("error.handoffDownload", "Could not prepare the handoff.", e); }
    }
    private static void entry(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); zip.write(text.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }
    private Project editable(long id) {
        Project p = project(id);
        if (p == null || p.handedOff) throw new Conflict("error.projectClosed", "This project is no longer open for changes.");
        return p;
    }
    private void replace(Project p, List<Task> tasks, boolean handedOff) {
        projects.set(projects.indexOf(p), new Project(p.id, p.revision + 1, p.name, p.client, p.category, p.due, p.budgetCents, tasks, handedOff));
    }
    private void log(String key, String... arguments) { activity.add(0, new Activity(key, List.of(arguments))); if (activity.size() > 12) activity.remove(activity.size() - 1); }
}
