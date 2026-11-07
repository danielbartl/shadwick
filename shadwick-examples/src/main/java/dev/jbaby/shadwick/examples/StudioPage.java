package dev.jbaby.shadwick.examples;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.*;
import dev.jbaby.shadwick.behaviors.*;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.chart.*;
import dev.jbaby.shadwick.components.form.*;
import dev.jbaby.shadwick.components.table.*;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.*;
import org.apache.wicket.ajax.*;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.*;
import org.apache.wicket.markup.html.list.*;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.request.handler.resource.ResourceRequestHandler;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.*;
import org.apache.wicket.validation.validator.StringValidator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import static dev.jbaby.shadwick.examples.StudioWorkspace.*;

/** Visitor-owned sample application. Commands update current models and changed DOM regions over Ajax. */
public class StudioPage extends SwAppLayout {
    private record Cash(String label, long euros) implements java.io.Serializable {}
    private static final CssResourceReference CSS = new CssResourceReference(StudioPage.class, "studio.css");
    private static final JavaScriptResourceReference JS = new JavaScriptResourceReference(StudioPage.class, "studio.js");
    private UUID generation;
    private String view;
    private long projectId;
    private String renderedTitle;
    private SwConfirmDialog confirmation;
    private SwDialog projectEditor, taskEditor, invoiceDialog;
    private final Model<String> projectName = Model.of(""), client = Model.of(""), category = Model.of("Brand & website");
    private final Model<LocalDate> due = Model.of();
    private final Model<BigDecimal> budget = Model.of(new BigDecimal("5000.00"));
    private final Model<String> taskTitle = Model.of(""), owner = Model.of("Aoife");
    private final Model<Integer> hours = Model.of(4);
    private Long editingId, invoiceId, taskProjectId;
    private long editingRevision;
    private final CommandContext projectCommand = new CommandContext(), taskCommand = new CommandContext(), invoiceCommand = new CommandContext();

    /** Bind each dialog submission to the instance opened by the visitor, including after view changes. */
    private final class CommandContext implements java.io.Serializable {
        private String expected;
        private final Model<String> submitted = Model.of();
        void start() { expected = UUID.randomUUID().toString(); submitted.setObject(expected); }
        void consume() { expected = null; }
        boolean valid(Form<?> form) {
            if (expected != null && expected.equals(submitted.getObject())) return true;
            consume(); submitted.setObject(null); form.error(t("feedback.actionChanged")); return false;
        }
    }

    public StudioPage(PageParameters parameters) {
        super(parameters);
        generation = workspace().generation;
        String requested = parameters.get("view").toString("overview");
        view = Set.of("overview", "projects", "project", "invoices").contains(requested) ? requested : "overview";
        projectId = parameters.get("project").toLong(1);
        for (Skin skin : Skin.values()) if (skin.id().equals(parameters.get("skin").toString())) ShadwickSettings.get().setCurrentSkin(skin);
        Locale locale = parameters.get("lang").isEmpty() ? getSession().getLocale() : Locale.forLanguageTag(parameters.get("lang").toString());
        getSession().setLocale(List.of("en", "de").contains(locale.getLanguage()) ? locale : Locale.ENGLISH);
        add(new SwFeedbackPanel("feedback"));
        add(bind("section", () -> view.equals("project") ? t("nav.projectWorkspace") : title()), bind("heading", this::heading),
                bind("intro", this::intro), bind("date", () -> date(workspace().today)));
        add(button("newProject", () -> t("action.newProject"), target -> openProjectEditor(target, null)));
        createDialogs();
        add(newContent());
        add(new AbstractDefaultAjaxBehavior() {
            @Override protected void respond(AjaxRequestTarget target) {
                var values = getRequest().getRequestParameters();
                PageParameters parameters = route(values.getParameterValue("studioView").toString("overview"), values.getParameterValue("studioProject").toLong(0));
                for (String key : List.of("q", "status", "sort")) parameters.set(key, values.getParameterValue("studio" + key).toString(""));
                navigate(target, parameters, false);
            }
            @Override public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript("CloverStudio.navigation(" + JSONObject.quote(getCallbackUrl().toString()) + ")"));
            }
        });
        var footer = new WebMarkupContainer("footer"); footer.setOutputMarkupId(true);
        footer.add(new BookmarkablePageLink<>("components", HomePage.class)); add(footer);
    }

    private void createDialogs() {
        projectCommand.consume(); taskCommand.consume(); invoiceCommand.consume();
        confirmation = new SwConfirmDialog("confirmation").setBusyIndicator(true); addOrReplace(confirmation);
        projectEditor = new SwDialog("projectEditor", t("dialog.newProject")) {
            @Override protected void onClose(AjaxRequestTarget target) { projectCommand.consume(); super.onClose(target); }
            @Override protected void onConfirm(AjaxRequestTarget target) {
                if (!current(target) || !projectCommand.valid(getForm())) return;
                try {
                    boolean creating = editingId == null;
                    Project saved = workspace().saveProject(editingId, editingRevision, projectName.getObject(), client.getObject(), category.getObject(), due.getObject(), budget.getObject().movePointRight(2).longValueExact());
                    projectCommand.consume(); changed(); success(t("feedback.saved"));
                    if (creating) display(target, "project", saved.id());
                    refresh(target);
                } catch (IllegalArgumentException | IllegalStateException | ArithmeticException error) { getForm().error(problem(error)); }
            }
        };
        projectEditor.setDescription(t("dialog.projectDescription")).setConfirmLabel(t("action.saveProject")).setBusyIndicator(true)
                .setContent(new ProjectEditor(SwDialog.CONTENT_ID)); addOrReplace(projectEditor);
        taskEditor = new SwDialog("taskEditor", t("dialog.task")) {
            @Override protected void onClose(AjaxRequestTarget target) { taskCommand.consume(); super.onClose(target); }
            @Override protected void onConfirm(AjaxRequestTarget target) {
                if (!current(target) || !taskCommand.valid(getForm())) return;
                try { workspace().addTask(taskProjectId, taskTitle.getObject(), owner.getObject(), hours.getObject()); taskCommand.consume(); changed(); success(t("feedback.taskAdded")); refresh(target); }
                catch (IllegalArgumentException | IllegalStateException error) { getForm().error(problem(error)); }
            }
        };
        taskEditor.setDescription(t("dialog.taskDescription")).setConfirmLabel(t("action.addTask")).setBusyIndicator(true).setContent(new TaskEditor(SwDialog.CONTENT_ID)); addOrReplace(taskEditor);
        invoiceDialog = new SwDialog("invoiceDialog", t("field.invoice")) {
            @Override protected void onClose(AjaxRequestTarget target) { invoiceCommand.consume(); super.onClose(target); }
            @Override protected void onConfirm(AjaxRequestTarget target) {
                if (!current(target) || !invoiceCommand.valid(getForm())) return;
                if (invoiceId == null) { getForm().error(t("error.openInvoice")); return; }
                try { boolean paid = workspace().pay(invoiceId); invoiceCommand.consume(); changed(); success(t(paid ? "feedback.paid" : "feedback.alreadyPaid")); refresh(target); }
                catch (IllegalStateException error) { getForm().error(problem(error)); }
            }
        };
        invoiceDialog.setDescription(t("dialog.invoiceDescription")).setConfirmLabel(t("action.pay"))
                .setConfirmVariant(ButtonBehavior.Variant.DEFAULT).setBusyIndicator(true); addOrReplace(invoiceDialog);
    }

    private String title() { return t(switch (view) { case "projects", "project" -> "nav.projects"; case "invoices" -> "nav.invoices"; default -> "nav.overview"; }); }
    private String heading() {
        if (view.equals("project")) { Project p = workspace().project(projectId); return p == null ? t("heading.missing") : p.client(); }
        return t("heading." + view);
    }
    private String intro() { return t("intro." + view); }
    static PageParameters route(String view, long project) { PageParameters p = new PageParameters().set("view", view); if (project > 0) p.set("project", project); return p; }
    private Panel newContent() {
        Panel result = switch (view) {
            case "projects" -> new Projects("content");
            case "project" -> workspace().project(projectId) == null ? new Missing("content") : new ProjectView("content", projectId);
            case "invoices" -> new Invoices("content");
            default -> new Overview("content");
        };
        result.setOutputMarkupId(true); return result;
    }
    private void replaceContent(AjaxRequestTarget target) {
        Component previous = get("content"); Panel content = newContent();
        if (previous instanceof Invoices oldInvoices && content instanceof Invoices newInvoices) newInvoices.copyPresentation(oldInvoices);
        addOrReplace(content); target.add(content);
    }
    private void display(AjaxRequestTarget target, String nextView, long id) { navigate(target, route(nextView, id), false); }
    private void navigate(AjaxRequestTarget target, PageParameters parameters, boolean push) {
        String requested = parameters.get("view").toString("overview");
        String next = Set.of("overview", "projects", "project", "invoices").contains(requested) ? requested : "overview";
        long id = parameters.get("project").toLong(next.equals("project") ? 1 : 0);
        boolean sameView = view.equals(next) && (!next.equals("project") || projectId == id) && generation.equals(workspace().generation);
        String previousMenu = view.equals("project") ? "projects" : view;
        view = next; projectId = id;
        for (String key : List.of("view", "project", "q", "status", "sort")) {
            getPageParameters().remove(key);
            if (!parameters.get(key).isEmpty()) getPageParameters().set(key, parameters.get(key).toString());
        }
        getPageParameters().set("view", view);
        if (!sameView) replaceContent(target);
        else if (get("content") instanceof Projects projects) projects.apply(parameters);
        if (!previousMenu.equals(view.equals("project") ? "projects" : view)) target.add(getSidebar().getMenu());
        refresh(target); syncUrl(target, push); target.focusComponent(get("heading"));
    }
    private boolean current(AjaxRequestTarget target) {
        if (generation.equals(workspace().generation)) return true;
        generation = workspace().generation; warn(t("feedback.resetElsewhere")); replaceContent(target); refresh(target); return false;
    }
    private void requireCurrentDownload() {
        if (!generation.equals(workspace().generation)) throw new AbortWithHttpErrorCodeException(409, t("feedback.resetElsewhere"));
    }
    private void mutate(AjaxRequestTarget target, Runnable action, String message) {
        if (!current(target)) return;
        try { action.run(); changed(); success(message); }
        catch (IllegalArgumentException | IllegalStateException error) { warn(problem(error)); }
        refresh(target);
    }
    private void openProjectEditor(AjaxRequestTarget target, Project p) {
        if (!current(target)) return;
        projectCommand.start(); editingId = p == null ? null : p.id(); editingRevision = p == null ? 0 : p.revision();
        projectName.setObject(p == null ? "" : p.name()); client.setObject(p == null ? "" : p.client()); category.setObject(p == null ? "Brand & website" : p.category());
        due.setObject(p == null ? workspace().today.plusDays(21) : p.due()); budget.setObject(p == null ? new BigDecimal("5000.00") : BigDecimal.valueOf(p.budgetCents(), 2));
        projectEditor.setTitle(t(p == null ? "dialog.newProject" : "action.editProject")); projectEditor.getForm().clearInput(); projectEditor.open(target);
    }
    private void inspect(AjaxRequestTarget target, long id) {
        if (!current(target)) return;
        Invoice fresh = workspace().invoice(id);
        if (fresh == null) { warn(t("error.invoiceMissing")); refresh(target); return; }
        invoiceCommand.start(); invoiceId = id; invoiceDialog.setTitle(fresh.number()).setContent(new InvoicePreview(SwDialog.CONTENT_ID, id)).setFooterVisible(!fresh.paid()).open(target);
    }

    /** Each binding remembers only what it last rendered, not a stale domain-object snapshot. */
    private static final class ViewState extends Behavior {
        private final IModel<?> state;
        private Object rendered;
        ViewState(IModel<?> state) { this.state = state; rendered = state.getObject(); }
        boolean changed() { return !Objects.equals(rendered, state.getObject()); }
        void remember() { rendered = state.getObject(); }
        @Override public void beforeRender(Component component) { remember(); }
        @Override public void detach(Component component) { state.detach(); }
    }
    private <C extends Component> C watch(C component, IModel<?> state) {
        component.setOutputMarkupPlaceholderTag(true); component.add(new ViewState(state)); return component;
    }
    private Label bind(String id, IModel<?> value) { return watch(new Label(id, value), value); }
    private SwBadge badge(String id, IModel<String> value) { return watch(new SwBadge(id, value, BadgeBehavior.Variant.SECONDARY), value); }
    private SwButton button(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> action) {
        SwButton button = new SwButton(id, label) {
            @Override public void onClick(AjaxRequestTarget target) { action.accept(target); }
            @Override protected void updateAjaxAttributes(AjaxRequestAttributes attributes) { super.updateAjaxAttributes(attributes); attributes.setMethod(AjaxRequestAttributes.Method.POST); }
        };
        return watch(button.setBusyIndicator(true), label);
    }
    private <C extends Component> C visible(C component, IModel<Boolean> value) {
        component.add(new Behavior() { @Override public void onConfigure(Component c) { c.setVisible(value.getObject()); } });
        return watch(component, value);
    }
    private <C extends Component> C enabled(C component, IModel<Boolean> value) {
        component.add(new Behavior() { @Override public void onConfigure(Component c) { c.setEnabled(value.getObject()); } });
        return watch(component, value);
    }
    private void progress(MarkupContainer container, IModel<Project> project) {
        IModel<Integer> amount = project.map(Project::progress);
        container.add(watch(new SwProgress("progress", amount).setLabel(project.map(p -> t("project.progressLabel", p.client()))),
                (IModel<?>) () -> List.of(project.getObject().progress(), project.getObject().client())), bind("percent", project.map(p -> p.progress() + "%")));
    }
    private static boolean covered(Component component, Collection<? extends Component> targets) {
        for (Component c = component; c != null; c = c.getParent()) if (targets.contains(c)) return true;
        return false;
    }
    private void refresh(AjaxRequestTarget target) {
        // Parent replacements cover their descendants. Unchanged shell, forms and rows keep their DOM identity.
        visitChildren(Component.class, (component, visit) -> {
            if (!covered(component, target.getComponents()) && component.getBehaviors(ViewState.class).stream().anyMatch(ViewState::changed)) target.add(component);
        });
        String nextTitle = heading() + " · Clover Studio";
        if (!Objects.equals(renderedTitle, nextTitle)) { target.appendJavaScript("document.title=" + JSONObject.quote(nextTitle)); renderedTitle = nextTitle; }
    }
    private void syncUrl(AjaxRequestTarget target, boolean push) {
        String url = urlFor(StudioPage.class, new PageParameters(getPageParameters())).toString();
        target.appendJavaScript("CloverStudio.url(" + JSONObject.quote(url) + "," + push + ")");
    }
    private String t(String key, Object... arguments) {
        return new java.text.MessageFormat(getString("studio." + key), getLocale()).format(arguments);
    }
    private String problem(Exception error) {
        return t(error instanceof MessageFailure failure ? failure.key() : "error.details");
    }
    private String displayStatus(String status) {
        return t(switch (status) {
            case "In progress" -> "status.progress"; case "Ready for handoff" -> "status.ready"; case "Delivered" -> "status.delivered";
            case "Paid" -> "status.paid"; case "Overdue" -> "status.overdue"; default -> "status.awaiting";
        });
    }
    private String milestoneTitle(String title) {
        return t(switch (title) { case "Creative direction" -> "milestone.direction"; case "Design review" -> "milestone.review"; default -> "milestone.handoff"; });
    }
    private IChoiceRenderer<String> filterRenderer() {
        return new IChoiceRenderer<>() {
            @Override public String getIdValue(String value, int index) { return value; }
            @Override public Object getDisplayValue(String value) {
                return switch (value) {
                    case "All projects" -> t("filter.all"); case "Due date" -> t("sort.due");
                    case "Client A–Z" -> t("sort.client"); case "Budget high–low" -> t("sort.budget"); default -> displayStatus(value);
                };
            }
        };
    }
    private static StudioWorkspace workspace() { return StudioWorkspace.get(); }
    static String money(long cents) { return DataTablePage.money(cents, Session.get().getLocale()); }
    static String date(LocalDate value) { return value.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Session.get().getLocale())); }
    private <C extends Component> C navigation(C link, IModel<PageParameters> parameters) {
        link.add(new AjaxEventBehavior("click") {
            @Override protected void onEvent(AjaxRequestTarget target) { navigate(target, parameters.getObject(), true); }
            @Override protected void updateAjaxAttributes(AjaxRequestAttributes attributes) { super.updateAjaxAttributes(attributes); attributes.setPreventDefault(true); }
        }); return link;
    }
    private BookmarkablePageLink<Void> link(String id, String view, long project) {
        return navigation(new BookmarkablePageLink<>(id, StudioPage.class, route(view, project)), () -> route(view, project));
    }
    @Override protected AbstractLink newMenuLink(String id, MenuItem item) {
        AbstractLink link = super.newMenuLink(id, item);
        return item.pageClass() == StudioPage.class ? navigation(link, item::parameters) : link;
    }
    private static Label text(String id, Object value) { return new Label(id, value == null ? "" : value.toString()); }

    @Override protected IModel<String> appNameModel() { return () -> t("nav.context", title()); }
    @Override protected IModel<String> appTitleModel() { return () -> heading() + " · Clover Studio"; }
    @Override protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return () -> List.of(MenuItem.group(t("nav.workspace").toUpperCase(getLocale()), MenuItem.of(t("nav.overview"), StudioPage.class, route("overview", 0)).withIcon("house"),
                MenuItem.of(t("nav.projects"), StudioPage.class, route("projects", 0)).withIcon("folder"), MenuItem.of(t("nav.invoices"), StudioPage.class, route("invoices", 0)).withIcon("file-text")));
    }
    @Override protected boolean isCurrentPage(MenuItem item) { return item.parameters() != null && item.parameters().get("view").toString("").equals(view.equals("project") ? "projects" : view); }
    @Override protected SidebarBehavior.Collapsible sidebarCollapsible() { return SidebarBehavior.Collapsible.NONE; }
    @Override protected ThemeBehavior newThemeBehavior() {
        ThemeBehavior theme = new ThemeBehavior().setStorageKey("clover-studio-theme");
        String mode = getPageParameters().get("mode").toString("");
        if (mode.equals("light") || mode.equals("dark")) theme.setDefaultMode(mode.equals("dark") ? ThemeMode.DARK : ThemeMode.LIGHT).setRememberChoice(false);
        return theme;
    }
    @Override protected SidebarProviderBehavior newSidebarProviderBehavior() { return new SidebarProviderBehavior().setCookieName("clover-studio-sidebar"); }
    @Override protected Component newSidebarHeader(String id) { return new Brand(id); }
    @Override protected Component newSidebarFooter(String id) { return new Team(id); }
    @Override protected Component newTopNavExtra(String id) { return new DemoTools(id); }
    @Override public void renderHead(IHeaderResponse response) { super.renderHead(response); response.render(CssHeaderItem.forReference(CSS)); response.render(JavaScriptHeaderItem.forReference(JS)); }
    @Override protected void onInitialize() {
        super.onInitialize(); getProvider().add(AttributeModifier.append("class", "studio-app"));
        watch(getTopNav().get("appName"), appNameModel()); get("skip").setOutputMarkupId(true); getTopNav().get("trigger").setOutputMarkupId(true);
    }
    @Override protected void onBeforeRender() { renderedTitle = heading() + " · Clover Studio"; super.onBeforeRender(); }
    public static class Brand extends Panel { public Brand(String id) { super(id); } }
    public static class Team extends Panel { public Team(String id) { super(id); } }
    public class DemoTools extends Panel {
        DemoTools(String id) {
            super(id); setOutputMarkupId(true); add(new SwThemeToggle("theme"));
            Model<Skin> skin = Model.of(ShadwickSettings.get().getCurrentSkin());
            Model<String> language = Model.of(getLocale().getLanguage());
            Form<Void> form = new Form<>("settings") {
                @Override protected void onSubmit() {
                    // No-script fallback for the explicit global preference change.
                    if (getRequestCycle().find(AjaxRequestTarget.class).isEmpty()) {
                        applyPreferences(); setResponsePage(StudioPage.class, new PageParameters(getPageParameters()));
                    }
                }
                private void applyPreferences() {
                    ShadwickSettings.get().setCurrentSkin(skin.getObject()); getSession().setLocale(Locale.forLanguageTag(language.getObject()));
                    getPageParameters().set("skin", skin.getObject().id()).set("lang", language.getObject());
                }
            };
            form.add(new DropDownChoice<>("skin", skin, List.of(Skin.values()), new IChoiceRenderer<>() { @Override public Object getDisplayValue(Skin value) { return value.id(); } }),
                    new DropDownChoice<>("language", language, List.of("en", "de"), new IChoiceRenderer<>() {
                        @Override public Object getDisplayValue(String value) { return Locale.forLanguageTag(value).getDisplayLanguage(Locale.forLanguageTag(value)); }
                        @Override public String getIdValue(String value, int index) { return value; }
                    }));
            form.add(new AjaxFallbackButton("apply", form) {
                @Override protected void onSubmit(Optional<AjaxRequestTarget> ajax) {
                    if (ajax.isEmpty()) return;
                    AjaxRequestTarget target = ajax.get();
                    ShadwickSettings.get().setCurrentSkin(skin.getObject()); getSession().setLocale(Locale.forLanguageTag(language.getObject()));
                    getPageParameters().set("skin", skin.getObject().id()).set("lang", language.getObject());
                    // Component Ajax updates intentionally retain their original page skin. This explicit
                    // whole-interface preference action loads and activates the chosen bundle itself.
                    replaceContent(target); createDialogs(); target.add(projectEditor, taskEditor, invoiceDialog, confirmation);
                    var tools = new DemoTools("topNavExtra"); getTopNav().addOrReplace(tools); target.add(tools, getSidebar(), StudioPage.this.get("footer"), StudioPage.this.get("newProject"), getTopNav().get("trigger"), StudioPage.this.get("skip"));
                    refresh(target); syncUrl(target, false);
                    target.appendJavaScript("CloverStudio.locale(" + JSONObject.quote(language.getObject()) + "," + JSONObject.quote(skin.getObject().id()) + "," + JSONObject.quote(urlFor(Shadwick.css(skin.getObject(), ShadwickSettings.get().getBundle()), null).toString()) + ")");
                }
            }.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL)));
            add(form, button("reset", () -> t("action.reset"), target -> confirmation.setConfirmLabel(t("action.resetConfirm")).setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE)
                    .ask(target, t("dialog.reset"), t("dialog.resetDescription"), confirmed -> {
                        StudioWorkspace.reset(); generation = workspace().generation; success(t("feedback.reset")); display(confirmed, "overview", 0); refresh(confirmed);
                    })).setVariant(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL));
        }
    }
    public class ProjectEditor extends Panel {
        ProjectEditor(String id) {
            super(id); add(new SwFeedbackPanel("errors", projectEditor.getForm()), new HiddenField<>("context", projectCommand.submitted).setRequired(true));
            add(new SwTextField<>("name", t("field.projectName"), projectName).setRequired(true).addValidator(StringValidator.maximumLength(100)),
                    new SwTextField<>("client", t("field.client"), client).setRequired(true).addValidator(StringValidator.maximumLength(80)),
                    new SwTextField<>("category", t("field.scope"), category).setRequired(true).addValidator(StringValidator.maximumLength(80)),
                    new SwDateField("due", t("field.due"), due).setRequired(true),
                    new SwNumberField<>("budget", t("field.budgetEur"), budget).setType(BigDecimal.class).setMin(new BigDecimal("0.01")).setMax(new BigDecimal("1000000")).setStep(new BigDecimal("0.01")).setRequired(true));
        }
    }
    public class TaskEditor extends Panel {
        TaskEditor(String id) {
            super(id); add(new SwFeedbackPanel("errors", taskEditor.getForm()), new HiddenField<>("context", taskCommand.submitted).setRequired(true));
            add(new SwTextField<>("title", t("field.task"), taskTitle).setRequired(true).addValidator(StringValidator.maximumLength(120)),
                    new SwSelect<>("owner", t("field.owner"), owner, Model.ofList(List.of("Aoife", "Cian", "Nora"))).setRequired(true),
                    new SwNumberField<>("hours", t("field.hours"), hours).setType(Integer.class).setMin(1).setMax(160).setStep(1).setRequired(true));
        }
    }
    public class Overview extends Panel {
        private Project featured() { return workspace().projects().stream().filter(p -> !p.handedOff()).min(Comparator.comparing(Project::due)).orElse(workspace().projects().get(0)); }
        private List<Project> upcoming() { return workspace().projects().stream().filter(p -> !p.handedOff()).sorted(Comparator.comparing(Project::due)).limit(3).toList(); }
        private List<Activity> events() { return workspace().activity().stream().limit(4).toList(); }
        Overview(String id) {
            super(id);
            add(bind("active", () -> workspace().active()), bind("outstanding", () -> money(workspace().outstanding())), bind("collected", () -> money(workspace().collected())));
            add(bind("featuredClient", () -> featured().client()), bind("featuredName", () -> featured().name()), bind("featuredScope", () -> featured().category()), bind("featuredDue", () -> date(featured().due())), link("allProjects", "projects", 0));
            var featuredLink = new BookmarkablePageLink<Void>("featuredLink", StudioPage.class, route("project", featured().id())) {
                @Override protected void onConfigure() { super.onConfigure(); getPageParameters().set("project", featured().id()); }
            };
            add(watch(navigation(featuredLink, () -> route("project", featured().id())), (IModel<Long>) () -> featured().id()));
            var artwork = new WebMarkupContainer("featuredArt"); artwork.add(AttributeModifier.append("class", (IModel<String>) () -> "studio-palette-" + featured().id() % 4));
            artwork.add(bind("wordmark", () -> featured().id() == 1 ? "fern & field" : featured().client()), visible(new WebMarkupContainer("botanical"), () -> featured().id() == 1));
            add(watch(artwork, (IModel<?>) () -> List.of(featured().id(), featured().client()))); progress(this, this::featured);
            var schedule = watch(new WebMarkupContainer("schedule"), (IModel<?>) () -> upcoming().stream().map(p -> List.of(p.id(), p.client(), p.category(), p.due())).toList());
            schedule.add(new ListView<Project>("upcoming", (IModel<List<Project>>) this::upcoming) {
                @Override protected void populateItem(ListItem<Project> item) { Project p = item.getModelObject(); var open = link("open", "project", p.id()); open.add(text("client", p.client()), text("name", p.category())); item.add(open, text("day", p.due().getDayOfMonth()), text("month", p.due().format(DateTimeFormatter.ofPattern("MMM", getLocale())))); }
            }); add(schedule);
            var feed = watch(new WebMarkupContainer("feed"), (IModel<List<Activity>>) this::events);
            feed.add(new ListView<Activity>("activity", (IModel<List<Activity>>) this::events) {
                @Override protected void populateItem(ListItem<Activity> item) { Activity event = item.getModelObject(); item.add(text("event", t(event.key(), event.arguments().toArray()))); }
            }); add(feed);
            IModel<List<Cash>> cash = () -> List.of(new Cash(t("chart.collected"), workspace().collected() / 100), new Cash(t("chart.outstanding"), workspace().outstanding() / 100));
            add(watch(new SwChart<>("cash", cash, Cash::label).setTitle(t("chart.title")).setCategoryHeader(t("chart.category")).setCurrency(Currency.getInstance("EUR"))
                    .addSeries(ChartSeries.bar(t("chart.fees"), Cash::euros)).setShowLegend(false), cash), link("invoices", "invoices", 0));
        }
    }
    public class Projects extends Panel {
        private final Model<String> query = Model.of(getPageParameters().get("q").toString(""));
        private final Model<String> status = Model.of(getPageParameters().get("status").toString("All projects"));
        private final Model<String> sort = Model.of(getPageParameters().get("sort").toString("Due date"));
        private void apply(PageParameters parameters) {
            query.setObject(parameters.get("q").toString(""));
            String state = parameters.get("status").toString("All projects"), order = parameters.get("sort").toString("Due date");
            status.setObject(List.of("All projects", "In progress", "Ready for handoff", "Delivered").contains(state) ? state : "All projects");
            sort.setObject(List.of("Due date", "Client A–Z", "Budget high–low").contains(order) ? order : "Due date");
        }
        private List<Project> matching() {
            Comparator<Project> order = switch (sort.getObject()) { case "Client A–Z" -> Comparator.comparing(Project::client, String.CASE_INSENSITIVE_ORDER); case "Budget high–low" -> Comparator.comparingLong(Project::budgetCents).reversed(); default -> Comparator.comparing(Project::due); };
            String q = query.getObject().toLowerCase(Locale.ROOT).strip();
            return workspace().projects().stream().filter(p -> (p.name() + " " + p.client() + " " + p.category()).toLowerCase(Locale.ROOT).contains(q))
                    .filter(p -> status.getObject().equals("All projects") || p.status().equals(status.getObject())).sorted(order.thenComparingLong(Project::id)).toList();
        }
        private void search(AjaxRequestTarget target, boolean push) {
            var parameters = getRequest().getRequestParameters();
            query.setObject(parameters.getParameterValue("studioQ").toString(""));
            String state = parameters.getParameterValue("studioStatus").toString("All projects"), order = parameters.getParameterValue("studioSort").toString("Due date");
            status.setObject(List.of("All projects", "In progress", "Ready for handoff", "Delivered").contains(state) ? state : "All projects");
            sort.setObject(List.of("Due date", "Client A–Z", "Budget high–low").contains(order) ? order : "Due date");
            getPageParameters().set("q", query.getObject()).set("status", status.getObject()).set("sort", sort.getObject());
            if (push) {
                // The browser already holds the submitted values. Preserve these input nodes and focus.
                for (String field : List.of("query", "status", "sort")) get("filters:" + field).getBehaviors(ViewState.class).forEach(ViewState::remember);
            }
            refreshResults(target); syncUrl(target, push);
        }
        private void refreshResults(AjaxRequestTarget target) {
            // Every explicit search returns freshly rendered results, even for identical matches.
            target.add(get("results"), get("count"), get("empty"));
            refresh(target);
        }
        Projects(String id) {
            super(id);
            var history = new AbstractDefaultAjaxBehavior() { @Override protected void respond(AjaxRequestTarget target) { search(target, false); } };
            add(history);
            WebMarkupContainer filters = new WebMarkupContainer("filters"); filters.setOutputMarkupId(true);
            filters.add(AttributeModifier.replace("action", (IModel<String>) () -> urlFor(StudioPage.class, new PageParameters()).toString()));
            filters.add(new AjaxEventBehavior("submit") {
                @Override protected void onEvent(AjaxRequestTarget target) { search(target, true); }
                @Override protected void updateAjaxAttributes(AjaxRequestAttributes attributes) { super.updateAjaxAttributes(attributes); attributes.setPreventDefault(true); attributes.setMethod(AjaxRequestAttributes.Method.GET);
                    attributes.getDynamicExtraParameters().add("return CloverStudio.parameters(" + JSONObject.quote(filters.getMarkupId()) + ")"); }
            });
            var queryField = new SwTextField<>("query", t("filter.search"), query).addValidator(StringValidator.maximumLength(100)).addToInput(AttributeModifier.replace("name", "q"));
            var statusField = new SwSelect<>("status", t("field.status"), status, Model.ofList(List.of("All projects", "In progress", "Ready for handoff", "Delivered")), filterRenderer()).addToInput(AttributeModifier.replace("name", "status"));
            var sortField = new SwSelect<>("sort", t("filter.sort"), sort, Model.ofList(List.of("Due date", "Client A–Z", "Budget high–low")), filterRenderer()).addToInput(AttributeModifier.replace("name", "sort"));
            filters.add(watch(queryField, query), watch(statusField, status), watch(sortField, sort), new WebMarkupContainer("apply").add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE))); add(filters);
            var clear = new BookmarkablePageLink<Void>("clear", StudioPage.class, route("projects", 0)); clear.add(new AjaxEventBehavior("click") {
                @Override protected void onEvent(AjaxRequestTarget target) { query.setObject(""); status.setObject("All projects"); sort.setObject("Due date"); for (String key : List.of("q", "status", "sort")) getPageParameters().remove(key); refreshResults(target); syncUrl(target, true); }
                @Override protected void updateAjaxAttributes(AjaxRequestAttributes attributes) { super.updateAjaxAttributes(attributes); attributes.setPreventDefault(true); }
            }); add(clear);
            add(bind("count", () -> t("project.count", matching().size())), visible(new WebMarkupContainer("empty"), () -> matching().isEmpty()));
            var results = watch(new WebMarkupContainer("results"), (IModel<List<Project>>) this::matching);
            results.add(new ListView<Project>("projects", (IModel<List<Project>>) this::matching) {
                @Override protected void populateItem(ListItem<Project> item) {
                    Project p = item.getModelObject(); item.add(AttributeModifier.append("class", "studio-palette-" + p.id() % 4));
                    var open = link("open", "project", p.id()); open.add(text("client", p.client()), text("name", p.name()));
                    item.add(open, text("scope", p.category()), new SwBadge("status", displayStatus(p.status()), BadgeBehavior.Variant.SECONDARY), text("budget", money(p.budgetCents())), text("due", date(p.due()))); progress(item, item.getModel());
                }
            }); add(results);
            add(new Behavior() { @Override public void renderHead(Component component, IHeaderResponse response) { response.render(OnDomReadyHeaderItem.forScript("CloverStudio.search(" + JSONObject.quote(history.getCallbackUrl().toString()) + ")")); } });
        }
    }
    public class ProjectView extends Panel {
        private final long id;
        private Project project() { return workspace().project(id); }
        ProjectView(String componentId, long id) {
            super(componentId); this.id = id; IModel<Project> p = this::project;
            add(bind("name", p.map(Project::name)), bind("scope", p.map(Project::category)), badge("status", p.map(value -> displayStatus(value.status()))),
                    bind("budget", p.map(value -> money(value.budgetCents()))), bind("due", p.map(value -> date(value.due()))), bind("hours", p.map(value -> value.hours() + "h")),
                    bind("taskCount", p.map(value -> t("project.completedCount", value.completed(), value.tasks().size()))));
            progress(this, p); add(link("back", "projects", 0));
            add(visible(button("edit", () -> t("action.editProject"), target -> openProjectEditor(target, project())).setVariant(ButtonBehavior.Variant.OUTLINE), () -> !project().handedOff()));
            add(visible(button("addTask", () -> t("action.addTask"), target -> {
                if (!current(target)) return; taskProjectId = id; taskCommand.start(); taskTitle.setObject(""); hours.setObject(4); taskEditor.getForm().clearInput(); taskEditor.open(target);
            }).setVariant(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL), () -> !project().handedOff()));
            add(visible(new WebMarkupContainer("emptyTasks"), () -> project().tasks().isEmpty()));
            var taskItems = watch(new WebMarkupContainer("taskItems"), p.map(value -> value.tasks().stream().map(Task::id).toList()));
            taskItems.add(new ListView<Task>("tasks", p.map(Project::tasks)) {
                @Override protected void populateItem(ListItem<Task> item) { item.setRenderBodyOnly(true); item.add(new TaskRow("row", id, item.getModelObject().id())); }
            }); add(taskItems);
            SwDialog brief = new SwDialog("briefPreview", t("file.scopeTitle")).setFooterVisible(false).setContent(new BriefPreview(SwDialog.CONTENT_ID, id));
            add(brief, new SwAttachment("brief", t("file.scopeName")).setDescription(t("file.scopeDescription")).setFullWidth(true)
                    .setPreview(brief.get("dialog"), t("file.scopePreview")).setActions(new ScopeActions(SwAttachment.ACTIONS_ID, id)));
            var milestones = watch(new WebMarkupContainer("milestoneItems"), p.map(Project::due));
            milestones.add(new ListView<Milestone>("milestones", p.map(Project::milestones)) {
                @Override protected void populateItem(ListItem<Milestone> item) { item.add(text("title", milestoneTitle(item.getModelObject().title())), text("date", date(item.getModelObject().date()))); }
            }); add(milestones);
            add(bind("handoffHint", p.map(value -> t(value.handedOff() ? "handoff.prepared" : value.progress() == 100 ? "handoff.ready" : "handoff.incomplete"))));
            var handoff = button("handoff", () -> t("action.handoff"), target -> {
                if (!current(target)) return;
                confirmation.setConfirmLabel(t("action.handoffConfirm")).setConfirmVariant(ButtonBehavior.Variant.DEFAULT).ask(target, t("dialog.handoff"), t("dialog.handoffDescription"), confirmed -> mutate(confirmed, () -> workspace().handoff(id), t("feedback.handoff")));
            });
            add(visible(enabled(handoff, () -> project().progress() == 100 && !project().tasks().isEmpty()), () -> !project().handedOff()));
            var download = new Link<Void>("download") {
                @Override public void onClick() { requireCurrentDownload(); getRequestCycle().scheduleRequestHandlerAfterCurrent(new ResourceRequestHandler(new ByteArrayResource("application/zip", workspace().handoffZip(id), "clover-project-" + id + "-handoff.zip"), null)); }
            }; download.add(new ButtonBehavior()); add(visible(download, () -> project().handedOff()));
            IModel<List<Invoice>> invoices = () -> workspace().invoices().stream().filter(i -> i.projectId() == id).toList();
            add(visible(new WebMarkupContainer("noInvoices"), () -> invoices.getObject().isEmpty()));
            add(new ListView<Invoice>("invoices", invoices) {
                @Override protected void populateItem(ListItem<Invoice> item) {
                    long key = item.getModelObject().id();
                    item.add(text("number", item.getModelObject().number()), text("amount", money(item.getModelObject().cents())), badge("status", () -> displayStatus(workspace().invoice(key).status(workspace().today))),
                            button("inspect", () -> t("action.viewInvoice"), target -> inspect(target, key)).setVariant(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL));
                }
            });
        }
    }
    public class TaskRow extends Panel {
        private final long project, task;
        private boolean renderedDone;
        private Task task() { return workspace().project(project).tasks().stream().filter(value -> value.id() == task).findFirst().orElseThrow(); }
        TaskRow(String id, long project, long task) {
            super(id); this.project = project; this.task = task; renderedDone = task().done();
            watch(this, (IModel<?>) () -> List.of(task(), workspace().project(project).handedOff()));
            add(AttributeModifier.replace("class", (IModel<String>) () -> task().done() ? "studio-task-done" : "studio-task-open"));
            add(bind("title", () -> task().title()), bind("owner", () -> task().owner()), bind("estimate", () -> task().hours() + "h"));
            add(visible(button("toggle", () -> t(task().done() ? "action.reopen" : "action.complete"), target -> {
                if (getRequest().getQueryParameters().getParameterValue("project").toLong(1) != project) { warn(t("feedback.actionChanged")); refresh(target); return; }
                boolean expected = renderedDone; mutate(target, () -> workspace().completeTask(project, task, expected), t(expected ? "feedback.reopened" : "feedback.completed"));
            }).setVariant(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL), () -> !workspace().project(project).handedOff()),
                    visible(new WebMarkupContainer("done"), () -> workspace().project(project).handedOff()));
        }
        @Override protected void onBeforeRender() { renderedDone = task().done(); super.onBeforeRender(); }
    }
    public class ScopeActions extends Panel {
        ScopeActions(String id, long project) {
            super(id); add(new Link<Void>("download") {
                @Override public void onClick() {
                    requireCurrentDownload(); Project p = workspace().project(project);
                    String text = "# " + p.client() + "\n\n" + p.name() + "\n\nScope: " + p.category() + "\nTarget handoff: " + p.due()
                            + "\nBudget (EUR): " + BigDecimal.valueOf(p.budgetCents(), 2) + "\n\n## Tasks\n\n"
                            + String.join("\n", p.tasks().stream().map(t -> "- [" + (t.done() ? "x" : " ") + "] " + t.title() + " · " + t.owner() + " · " + t.hours() + "h").toList());
                    getRequestCycle().scheduleRequestHandlerAfterCurrent(new ResourceRequestHandler(new ByteArrayResource("text/markdown", text.getBytes(StandardCharsets.UTF_8), "clover-project-" + project + "-scope.md"), null));
                }
            }.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL)));
        }
    }
    public class BriefPreview extends Panel {
        BriefPreview(String id, long project) {
            super(id); IModel<Project> p = () -> workspace().project(project);
            add(bind("client", p.map(Project::client)), bind("name", p.map(Project::name)), bind("scope", p.map(Project::category)), bind("budget", p.map(value -> money(value.budgetCents()))),
                    bind("due", p.map(value -> date(value.due()))), bind("hours", p.map(value -> t("project.estimatedHours", value.hours()))));
        }
    }
    public class Invoices extends Panel {
        private final InvoiceProvider provider = new InvoiceProvider();
        private final SwDataTable<Invoice, String> table;
        private void copyPresentation(Invoices previous) {
            var sort = previous.provider.getSort(); provider.setSort(sort.getProperty(), sort.isAscending() ? org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING : org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.DESCENDING);
            table.setItemsPerPage(previous.table.getItemsPerPage()); table.setCurrentPage(previous.table.getCurrentPage());
            for (int column = 0; column < 5; column++) if (table.isColumnVisible(column) != previous.table.isColumnVisible(column)) table.setColumnVisible(column, previous.table.isColumnVisible(column));
        }
        Invoices(String id) {
            super(id); add(bind("outstanding", () -> money(workspace().outstanding())), bind("collected", () -> money(workspace().collected())),
                    bind("overdue", () -> money(workspace().invoices().stream().filter(i -> !i.paid() && i.due().isBefore(workspace().today)).mapToLong(Invoice::cents).sum())));
            List<IColumn<Invoice, String>> columns = new ArrayList<>();
            columns.add(new SwLinkColumn<Invoice, String>(Model.of(t("field.client")), "client", row -> workspace().project(row.projectId()).client()) {
                @Override protected AbstractLink newLink(String id, IModel<Invoice> row) { return link(id, "project", row.getObject().projectId()); }
            });
            columns.add(new org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn<>(Model.of(t("field.invoice")), "number", Invoice::number));
            columns.add(new SwBadgeColumn<Invoice, String>(Model.of(t("field.status")), "status", row -> displayStatus(row.status(workspace().today)), row -> BadgeBehavior.Variant.SECONDARY) {
                @Override public void populateItem(Item<ICellPopulator<Invoice>> item, String componentId, IModel<Invoice> row) {
                    long key = row.getObject().id(); item.add(badge(componentId, () -> displayStatus(workspace().invoice(key).status(workspace().today))));
                }
            });
            columns.add(new SwNumberColumn<Invoice, String>(Model.of(t("field.amount")), "amount", row -> BigDecimal.valueOf(row.cents(), 2)).setCurrency(Currency.getInstance("EUR")));
            columns.add(new SwDateColumn<>(Model.of(t("sort.due")), "due", Invoice::due));
            var actions = new SwActionsColumn<Invoice, String>().setRowKey(Invoice::id).setRowLabel(Invoice::number);
            actions.addAction(t("action.viewInvoice"), (target, row) -> inspect(target, row.id())); columns.add(actions);
            table = new SwDataTable<>("table", columns, provider, 5).setLabel(t("table.label")).setCaption(t("table.caption"))
                    .setRowKey(Invoice::id).setPageSizes(5, 10).setColumnChooser(true).setCsvExport("clover-studio-invoices.csv");
            // A status change may reorder the table only when status sorting makes its row order change.
            add(watch(table, (IModel<?>) () -> provider.ordered().stream().map(Invoice::id).toList()));
        }
    }
    private class InvoiceProvider extends SortableDataProvider<Invoice, String> {
        InvoiceProvider() { setSort("number", org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.DESCENDING); }
        private List<Invoice> ordered() {
            Comparator<Invoice> comparator = switch (getSort().getProperty()) {
                case "client" -> Comparator.comparing(i -> workspace().project(i.projectId()).client());
                case "status" -> Comparator.comparing(i -> displayStatus(i.status(workspace().today)));
                case "amount" -> Comparator.comparingLong(Invoice::cents);
                case "due" -> Comparator.comparing(Invoice::due);
                default -> Comparator.comparing(Invoice::number);
            };
            if (!getSort().isAscending()) comparator = comparator.reversed(); return workspace().invoices().stream().sorted(comparator.thenComparingLong(Invoice::id)).toList();
        }
        @Override public Iterator<? extends Invoice> iterator(long first, long count) { return ordered().stream().skip(first).limit(count).iterator(); }
        @Override public long size() { return workspace().invoices().size(); }
        @Override public IModel<Invoice> model(Invoice row) { return LoadableDetachableModel.of(() -> workspace().invoice(row.id())); }
    }
    public class InvoicePreview extends Panel {
        InvoicePreview(String id, long key) {
            super(id); IModel<Invoice> invoice = () -> workspace().invoice(key); IModel<Project> p = () -> workspace().project(invoice.getObject().projectId());
            add(new SwFeedbackPanel("errors", invoiceDialog.getForm()), new HiddenField<>("context", invoiceCommand.submitted).setRequired(true), bind("client", p.map(Project::client)), bind("project", p.map(Project::name)), bind("description", invoice.map(Invoice::description)),
                    bind("amount", invoice.map(value -> money(value.cents()))), bind("due", invoice.map(value -> date(value.due()))),
                    bind("paid", invoice.map(value -> value.paid() ? t("invoice.paidOn", date(value.paidOn())) : t("invoice.noPayment"))), badge("status", invoice.map(value -> displayStatus(value.status(workspace().today)))));
        }
    }
    public class Missing extends Panel { Missing(String id) { super(id); add(link("back", "projects", 0)); } }
}
