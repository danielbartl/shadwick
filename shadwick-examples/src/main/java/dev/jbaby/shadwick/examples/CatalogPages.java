package dev.jbaby.shadwick.examples;

import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.List;

/** One inventory for routes, navigation, the catalog overview and guide links. */
final class CatalogPages {
    record Example(String route, String title, String group, String description,
                   Class<? extends Page> page, String guide) implements Serializable {}
    record Group(String title, List<Example> examples) implements Serializable {}
    static final List<Example> ALL = List.of(
        new Example("buttons", "Buttons", "Inputs & forms", "Variants, sizes, links and Ajax actions.", ButtonPage.class, "components/buttons"),
        new Example("form", "Forms & validation", "Inputs & forms", "Connect models, validation and form feedback.", FormPage.class, "components/forms"),
        new Example("inputs", "Text & number fields", "Inputs & forms", "Typed inputs, labels and validation states.", InputPage.class, "components/inputs"),
        new Example("choices", "Checkboxes & switches", "Inputs & forms", "Checkboxes, switches and radio groups.", ChoicePage.class, "components/choices"),
        new Example("selections", "Native selects", "Inputs & forms", "Single, multiple and grouped native choices.", SelectPage.class, "components/native-selects"),
        new Example("custom-select", "Custom select", "Inputs & forms", "Rich choices with a native foundation.", CustomSelectPage.class, "components/select"),
        new Example("combobox", "Combobox", "Inputs & forms", "Search, select and manage multiple choices.", ComboboxPage.class, "components/combobox"),
        new Example("dates", "Date & time fields", "Inputs & forms", "Native date, time, month and week inputs.", DateTimePage.class, "components/date-time"),
        new Example("groups", "Toggles & input groups", "Inputs & forms", "Compose toggles, addons and input actions.", GroupPage.class, "components/toggles-input-groups"),
        new Example("uploads", "File upload", "Inputs & forms", "Dropzones, selected files and multipart forms.", UploadPage.class, "components/file-upload"),
        new Example("dialog", "Dialogs", "Overlays & feedback", "Open content and validated forms in a dialog.", DialogPage.class, "components/dialog"),
        new Example("overlays", "Confirmations & sheets", "Overlays & feedback", "Confirm actions and reveal side panels.", OverlayPage.class, "components/sheet"),
        new Example("popups", "Popovers & tooltips", "Overlays & feedback", "Anchored content and contextual help.", PopupPage.class, "components/popover"),
        new Example("menus", "Dropdown menus", "Overlays & feedback", "Actions, navigation and checked choices.", MenuPage.class, "components/dropdown-menu"),
        new Example("toasts", "Toasts", "Overlays & feedback", "Timed notifications with keyboard access.", ToastPage.class, "components/toasts"),
        new Example("feedback", "Alerts & feedback", "Overlays & feedback", "Validation messages and persistent notices.", FeedbackPage.class, "components/feedback"),
        new Example("tables", "Tables", "Tables & data", "Native cells, compact rows and pinned columns.", TablePage.class, "components/table"),
        new Example("data", "Data tables", "Tables & data", "Providers, sorting, selection and row actions.", DataTablePage.class, "components/data-table"),
        new Example("advanced-tables", "Advanced tables", "Tables & data", "Inline editing, row details, columns and CSV.", AdvancedTablePage.class, "components/advanced-table"),
        new Example("pagination", "Pagination", "Tables & data", "Page sizes, summaries and bookmarkable state.", PaginationPage.class, "components/pagination"),
        new Example("invoice", "Invoice detail", "Tables & data", "A detail view connected to the data-table example.", InvoicePage.class, "components/data-table"),
        new Example("calendar", "Calendar & date picker", "Dates & charts", "Single dates, ranges and locale-aware calendars.", CalendarPage.class, "components/calendar"),
        new Example("month-view", "Month view", "Dates & charts", "An agenda with custom day content and navigation.", MonthViewPage.class, "components/month-view"),
        new Example("charts", "Charts", "Dates & charts", "Native SVG bars, lines, areas and exact data.", ChartPage.class, "components/charts"),
        new Example("shell", "App shell", "Layout & navigation", "Headers, breadcrumbs and skip navigation.", ShellPage.class, "components/app-shell"),
        new Example("sidebar", "Sidebar", "Layout & navigation", "Grouped links, icon rails and mobile navigation.", SidebarPage.class, "components/sidebar"),
        new Example("user-menu", "User menu", "Layout & navigation", "Account actions in a header or sidebar.", UserMenuPage.class, "components/user-menu"),
        new Example("theme", "Theme & preferences", "Layout & navigation", "Light, dark and system appearance preferences.", ThemePage.class, "guides/theming"),
        new Example("layouts", "Sidebar layout", "Layout & navigation", "A reusable workspace with sidebar navigation.", AppLayoutPage.class, "components/app-layout"),
        new Example("header-layout", "Header layout", "Layout & navigation", "The same workspace with navigation in the header.", HeaderLayoutPage.class, "components/app-layout"),
        new Example("disclosure", "Tabs & disclosures", "Layout & navigation", "Tabs, accordions and standalone collapsibles.", DisclosurePage.class, "components/tabs"),
        new Example("display", "Cards, badges & display", "Display & media", "Cards, avatars, badges, icons and loading states.", DisplayPage.class, "components/cards"),
        new Example("attachments", "Attachments", "Display & media", "File cards, image previews and download actions.", AttachmentPage.class, "components/attachments"),
        new Example("status-controls", "Progress & ratings", "Display & media", "Busy buttons, progress, meters and star ratings.", StatusControlsPage.class, "components/progress"),
        new Example("internationalization", "Internationalization", "Getting started", "Nine component languages and application overrides.", InternationalizationPage.class, "guides/internationalization")
    );
    static final List<Group> GROUPS = ALL.stream().map(Example::group).distinct()
            .map(title -> new Group(title, ALL.stream().filter(e -> e.group().equals(title)).toList())).toList();
    static Example find(Class<?> page) {
        return ALL.stream().filter(e -> e.page().equals(page)).findFirst().orElse(null);
    }
    static PageParameters preferences(PageParameters source) {
        var result = new PageParameters();
        for (String name : List.of("skin", "mode", "lang")) if (!source.get(name).isEmpty()) result.set(name, source.get(name).toString());
        return result;
    }
    static String guide(Class<?> page) {
        Example entry = find(page);
        return "https://danielbartl.github.io/shadwick/docs/" + (entry == null ? "guides/installation" : entry.guide()) + "/";
    }
    private CatalogPages() {}
}
