package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwConfirmDialog;
import dev.jbaby.shadwick.components.SwDropdown;
import dev.jbaby.shadwick.components.SwSheet;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/** Action factories, caller-owned links/checked items and menus inside other overlays. */
public class MenuPage extends BasePage {
    private final Model<Integer> actions = Model.of(0);
    private final IModel<List<String>> entries = Model.ofList(new ArrayList<>(List.of(
            "Edit project", "Export CSV", "Export PDF", "Archive (unavailable)", "Delete project…")));
    private final Model<Boolean> compact = Model.of(false);
    private final Model<String> sort = Model.of("Name");

    public MenuPage(PageParameters parameters) {
        super(parameters);
        Label status = new Label("status", Model.of("No action chosen.")); status.setOutputMarkupId(true);
        Label count = new Label("count", (IModel<String>) () -> "Actions: " + actions.getObject()); count.setOutputMarkupId(true);
        add(status, count);
        SwConfirmDialog confirm = new SwConfirmDialog("confirm"); add(confirm);

        SwDropdown<String> project = new SwDropdown<>("project", "Project actions", entries) {
            @Override protected void populateItem(ListItem<String> item) {
                item.add(new Label("label", item.getModel()));
                item.setEnabled(!item.getModelObject().startsWith("Archive"));
                if (item.getModelObject().startsWith("Delete")) item.getBehaviors(DropdownMenuItemBehavior.class).get(0)
                        .setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
                item.add(AjaxEventBehavior.onEvent("click", target -> {
                    if (item.getModelObject().startsWith("Delete")) {
                        confirm.setConfirmLabel("Delete preview").ask(target, "Delete this preview project?",
                                "This demo only records your answer.", t -> record(status, count, "Deletion confirmed.", t));
                    } else record(status, count, "Chosen: " + item.getModelObject() + ".", target);
                    close(target);
                }));
            }
        };
        add(project);
        add(SwButton.onClick("refresh", "Replace project actions", target -> {
            entries.setObject(new ArrayList<>(List.of("Rename project", "Export JSON", "Delete project…")));
            target.add(project);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwButton.onClick("after", "Continue on page", target -> record(status, count, "Continued on page.", target))
                .setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwDropdown.of("format", "Export as", Model.ofList(List.of("CSV", "JSON", "XML")), x -> x,
                (target, value) -> record(status, count, "Export format: " + value + ".", target)));
        add(SwDropdown.of("empty", "No available actions", Model.ofList(List.<String>of()), x -> x, (target, value) -> {}));

        WebMarkupContainer viewTrigger = new WebMarkupContainer("viewTrigger"); viewTrigger.add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE));
        WebMarkupContainer view = new WebMarkupContainer("view"); view.add(Sw.Behaviors.dropdownMenu(viewTrigger).setAlign(DropdownMenuBehavior.Align.END));
        add(viewTrigger, view);
        view.add(new Label("heading", "Display options").add(Sw.Behaviors.dropdownMenuLabel()));
        view.add(action("compact", "Compact rows", target -> {
            compact.setObject(!compact.getObject()); target.add(view);
            record(status, count, "Compact rows: " + compact.getObject() + ".", target);
        }).add(Sw.Behaviors.dropdownMenuCheckboxItem(compact)));
        view.add(new WebMarkupContainer("separator").add(Sw.Behaviors.dropdownMenuSeparator()));
        WebMarkupContainer sorting = new WebMarkupContainer("sorting"); sorting.add(Sw.Behaviors.dropdownMenuGroup());
        for (String value : List.of("Name", "Updated")) {
            sorting.add(action(value.toLowerCase(java.util.Locale.ROOT), "Sort by " + value.toLowerCase(java.util.Locale.ROOT), target -> {
                sort.setObject(value); target.add(view); record(status, count, "Sort order: " + value + ".", target);
            }).add(Sw.Behaviors.dropdownMenuRadioItem((IModel<Boolean>) () -> value.equals(sort.getObject()))));
        }
        view.add(sorting);

        WebMarkupContainer navigationTrigger = new WebMarkupContainer("navigationTrigger"); navigationTrigger.add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE));
        WebMarkupContainer navigation = new WebMarkupContainer("navigation"); navigation.add(Sw.Behaviors.dropdownMenu(navigationTrigger));
        add(navigationTrigger, navigation);
        navigation.add(new BookmarkablePageLink<>("home", HomePage.class).add(Sw.Behaviors.dropdownMenuItem()));
        BookmarkablePageLink<Void> popupLink = new BookmarkablePageLink<>("popups", PopupPage.class);
        popupLink.add(Sw.Behaviors.dropdownMenuItem());
        popupLink.add(new Label("shortcut", "⌘P").add(Sw.Behaviors.dropdownMenuShortcut()));
        navigation.add(popupLink);
        navigation.add(new BookmarkablePageLink<>("unavailable", MenuPage.class).add(Sw.Behaviors.dropdownMenuItem()).setEnabled(false));

        add(new SwSheet("sheet", "Menu in a sheet").setTrigger("Open menu sheet").setFooterVisible(false)
                .setContent(SwDropdown.of(SwSheet.CONTENT_ID, "Sheet actions", Model.ofList(List.of("Copy reference", "Check status")), x -> x,
                        (target, value) -> record(status, count, "Sheet: " + value + ".", target))));
    }

    private void record(Label status, Label count, String value, AjaxRequestTarget target) {
        actions.setObject(actions.getObject() + 1); status.setDefaultModelObject(value); target.add(status, count);
    }
    private static AjaxLink<Void> action(String id, String label, SerializableConsumer<AjaxRequestTarget> callback) {
        AjaxLink<Void> item = new AjaxLink<>(id) { @Override public void onClick(AjaxRequestTarget target) { callback.accept(target); } };
        item.setBody(Model.of(label)); return item;
    }
    @Override protected String getTitle() { return "Dropdown menus"; }
}
