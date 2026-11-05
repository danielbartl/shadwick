package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.AttachmentBehavior;
import dev.jbaby.shadwick.behaviors.AttachmentGroupBehavior;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import java.util.List;

/** Application-owned files, native downloads/previews and ordinary Ajax state updates. */
public class AttachmentPage extends BasePage {
    static final PackageResourceReference LANDSCAPE = new PackageResourceReference(AttachmentPage.class, "attachment-landscape.svg");
    static final PackageResourceReference NOTES = new PackageResourceReference(AttachmentPage.class, "workspace-notes.txt");
    public AttachmentPage(PageParameters parameters) {
        super(parameters);
        var status = new Label("status", Model.of("Your files are ready.")); status.setOutputMarkupId(true); add(status);
        var preview = new SwDialog("preview", "A little room to wander.").setDescription("Wicklow hills · Original illustration").setFooterVisible(false);
        preview.setContent(new Preview(SwDialog.CONTENT_ID)); add(preview);
        var document = new SwAttachment("document", "Workspace notes.txt").setDescription("Text document · 114 B").setFullWidth(true);
        document.setOutputMarkupPlaceholderTag(true); document.setMarkupId("notes-attachment"); add(document);
        var restore = SwButton.onClick("restore", "Restore notes", target -> {
            document.setVisible(true); status.setDefaultModelObject("Workspace notes restored."); target.add(document, status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE); restore.setOutputMarkupId(true); add(restore);
        var actions = new FileActions(SwAttachment.ACTIONS_ID);
        var remove = SwButton.onClick("remove", (org.apache.wicket.model.IModel<String>) null, target -> {
            document.setVisible(false); status.setDefaultModelObject("Workspace notes removed."); target.add(document, status); target.focusComponent(restore);
        }).setVariant(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.ICON);
        remove.add(Sw.Behaviors.attachmentAction().setLabel("Remove Workspace notes.txt")); remove.add(new SwIcon("icon", "x")); actions.add(remove);
        document.setActions(actions).setLink(urlFor(NOTES, null).toString(), "Open Workspace notes.txt");
        var photo = new SwAttachment("photo", "Wicklow hills.svg").setDescription("Illustration · 800 × 600").setImageUrl(urlFor(LANDSCAPE, null).toString())
                .setFullWidth(true).setPreview(preview.get("dialog"), "Preview Wicklow hills.svg"); photo.setMarkupId("photo-attachment"); add(photo);
        photo.setActions(new DownloadAction(SwAttachment.ACTIONS_ID));
        var processing = new SwAttachment("processing", "Spring collection.zip").setDescription("Ready to send · 8.2 MB").setState(AttachmentBehavior.State.IDLE).setFullWidth(true);
        processing.setOutputMarkupId(true); processing.setMarkupId("status-attachment"); add(processing);
        add(SwButton.onClick("advance", "Next file status", target -> {
            var states = List.of(AttachmentBehavior.State.IDLE, AttachmentBehavior.State.UPLOADING, AttachmentBehavior.State.PROCESSING, AttachmentBehavior.State.ERROR, AttachmentBehavior.State.DONE);
            var descriptions = List.of("Ready to send · 8.2 MB", "Uploading · 64%", "Processing · Preparing your files", "Upload failed · Please try again", "Ready · 8.2 MB");
            int next = (states.indexOf(processing.getState()) + 1) % states.size();
            processing.setState(states.get(next)).setDescription(descriptions.get(next));
            status.setDefaultModelObject("Spring collection.zip: " + descriptions.get(next)); target.add(processing, status);
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        var row = new SwAttachmentGroup("row").setLabel("Project documents"); row.setMarkupId("attachment-row"); add(row);
        row.add(new ListView<String>("files", List.of("Project brief.pdf", "Research notes.txt", "Brand guidelines.pdf", "Meeting notes.txt", "Launch checklist.txt")) {
            @Override protected void populateItem(ListItem<String> item) {
                item.setRenderBodyOnly(true);
                item.add(new SwAttachment("file", item.getModel()).setDescription("Shared with your team").setSize(AttachmentBehavior.Size.SMALL));
            }
        });
        var gallery = new SwAttachmentGroup("gallery").setLayout(AttachmentGroupBehavior.Layout.GRID).setLabel("Landscape illustrations"); gallery.setMarkupId("attachment-gallery"); add(gallery);
        gallery.add(new ListView<String>("photos", List.of("Morning in Wicklow.svg", "The scenic route.svg", "A place to return.svg")) {
            @Override protected void populateItem(ListItem<String> item) {
                item.setRenderBodyOnly(true);
                item.add(new SwAttachment("file", item.getModel()).setImageUrl(urlFor(LANDSCAPE, null).toString())
                        .setDescription("Original illustration").setOrientation(AttachmentBehavior.Orientation.VERTICAL).setFullWidth(true)
                        .setPreview(preview.get("dialog"), "Preview " + item.getModelObject()));
            }
        });
        add(new SwAttachment("compact", "Wicklow landscape").setImageUrl(urlFor(LANDSCAPE, null).toString()).setShowContent(false)
                .setOrientation(AttachmentBehavior.Orientation.VERTICAL).setSize(AttachmentBehavior.Size.EXTRA_SMALL));
        add(new SwAttachment("tiny", "Invoice.pdf").setDescription("PDF · 240 KB").setSize(AttachmentBehavior.Size.EXTRA_SMALL));
        add(new SwAttachment("small", "Project brief.pdf").setDescription("PDF · 1.2 MB").setSize(AttachmentBehavior.Size.SMALL));
    }
    public static class FileActions extends Panel {
        public FileActions(String id) {
            super(id);
            var link = new ResourceLink<>("download", NOTES);
            link.add(AttributeModifier.replace("download", "workspace-notes.txt"), Sw.Behaviors.attachmentAction().setLabel("Download Workspace notes.txt"));
            link.add(new SwIcon("icon", "download")); add(link);
        }
    }
    public static class DownloadAction extends Panel {
        public DownloadAction(String id) {
            super(id);
            var link = new ResourceLink<>("download", LANDSCAPE);
            link.add(AttributeModifier.replace("download", "wicklow-hills.svg"), Sw.Behaviors.attachmentAction().setLabel("Download Wicklow hills.svg"));
            link.add(new SwIcon("icon", "download")); add(link);
        }
    }
    public static class Preview extends Panel {
        public Preview(String id) { super(id); add(new Image("image", LANDSCAPE)); }
    }
    @Override protected String getTitle() { return "Attachments"; }
}
