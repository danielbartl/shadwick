package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.TabsBehavior;
import dev.jbaby.shadwick.components.SwAccordion;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwCollapsible;
import dev.jbaby.shadwick.components.SwTabs;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.List;

/** Native disclosures and browser-switched tabs with bookmarkable fallback routes. */
public class DisclosurePage extends BasePage {
    record Question(String title, String answer) implements Serializable {}
    private static final List<String> SECTIONS = List.of("Account", "Preferences", "History", "Admin");

    public DisclosurePage(PageParameters parameters) {
        super(parameters);
        var settings = new SwTabs<String>("settingsTabs", Model.ofList(SECTIONS)) {
            @Override protected void populateTab(ListItem<String> item) { item.add(new Label("tabLabel", item.getModelObject())); }
            @Override protected void populatePanel(ListItem<String> item) {
                item.add(new Label("panelTitle", item.getModelObject().equals("Account") ? "A little more you." : "Make it feel like home."));
                item.add(new Label("panelText", "Your " + item.getModelObject().toLowerCase() + ", in a familiar place."));
                TextField<String> note = new TextField<>("note", Model.of("A little room for good ideas"));
                note.setMarkupId("settings-note-" + item.getIndex()); note.setOutputMarkupId(true); item.add(note);
                item.add(new Label("noteLabel", "Workspace note").add(org.apache.wicket.AttributeModifier.replace("for", note.getMarkupId())));
            }
            @Override protected boolean isTabEnabled(String item, int index) { return index != 2; }
            @Override protected boolean isTabVisible(String item, int index) { return index != 3; }
            @Override protected CharSequence tabUrl(String item, int index) {
                return urlFor(DisclosurePage.class, new PageParameters(parameters).set("tab", item.toLowerCase()));
            }
        };
        int selected = 0;
        for (int i = 0; i < SECTIONS.size(); i++) if (SECTIONS.get(i).equalsIgnoreCase(parameters.get("tab").toString("account"))) selected = i;
        settings.setSelectedTab(selected).setLabel("Workspace settings"); settings.setMarkupId("settings-tabs"); add(settings);
        add(SwButton.onClick("refreshTabs", "Restore initial tab", target -> target.add(settings)).setVariant(ButtonBehavior.Variant.OUTLINE));

        var manual = SwTabs.of("manualTabs", Model.ofList(List.of("Overview", "Activity", "People")),
                (item, title) -> item.add(new Label("tabLabel", title)), (item, title) -> {
                    item.add(new Label("panelContent", title + ": a little space to see the whole picture."));
                    var nested = SwTabs.of("nestedTabs", Model.ofList(List.of("Summary", "Details")),
                            (tab, text) -> tab.add(new Label("tabLabel", text)),
                            (panel, text) -> panel.add(new Label("panelContent", text + " for your workspace.")));
                    nested.setLabel("Overview detail"); nested.setMarkupId("nested-tabs-" + item.getIndex());
                    nested.setVisible(item.getIndex() == 0); item.add(nested);
                });
        manual.setLabel("Project views").setVariant(TabsBehavior.Variant.LINE).setActivation(TabsBehavior.Activation.MANUAL)
                .setOrientation(TabsBehavior.Orientation.VERTICAL); manual.setMarkupId("manual-tabs"); add(manual);

        WebMarkupContainer single = new WebMarkupContainer("single"); single.setOutputMarkupId(true); single.setMarkupId("faq-single"); add(single);
        SwAccordion<Question> faq = SwAccordion.of("questions", Model.ofList(List.of(
                new Question("Will it feel familiar?", "Ordinary Wicket components, with a fresh coat. Bring your models and your ideas."),
                new Question("Can I make it my own?", "Choose a skin, change your palette and compose the pieces your way."),
                new Question("What is still taking shape?", "This preview is not available yet."))), (item, question) -> {
            item.add(new Label("title", question.title()));
            Label answer = new Label("content", question.answer()); answer.setMarkupId("faq-answer-" + item.getIndex()); answer.setOutputMarkupId(true); item.add(answer);
            if (item.getIndex() == 2) item.setEnabled(false);
        }).setOpenItems(0);
        single.add(faq);
        add(SwButton.onClick("refreshAccordion", "Open the second answer", target -> { faq.setOpenItems(1); target.add(single); })
                .setVariant(ButtonBehavior.Variant.OUTLINE));
        var multiple = SwAccordion.of("multiple", Model.ofList(List.of("Made for the keyboard", "Comfortable in any light")),
                false, (item, title) -> item.add(new Label("title", title), new Label("content", "A considered detail, wherever you work."))).setOpenItems(0, 1);
        add(multiple);

        var more = new SwCollapsible("more", "A little more about this workspace").setVariant(ButtonBehavior.Variant.OUTLINE)
                .setContent(new MoreContent(SwCollapsible.CONTENT_ID)); more.setMarkupId("workspace-more"); add(more);
        var unavailable = new SwCollapsible("unavailable", "Coming soon").setContent("Private preview content"); unavailable.setEnabled(false); add(unavailable);
        var managed = new SwCollapsible("managed", "Managed by your team").setOpen(true).setContent("A shared choice, kept in view."); managed.setEnabled(false); add(managed);
    }

    public static class MoreContent extends Panel {
        public MoreContent(String id) {
            super(id);
            add(SwAccordion.of("nested", Model.ofList(List.of("A place to begin", "A place to return")), false,
                    (item, title) -> item.add(new Label("title", title), new Label("content", "The little details make it yours."))).setOpenItems(0));
        }
    }
    @Override protected String getTitle() { return "Tabs & disclosures"; }
}
