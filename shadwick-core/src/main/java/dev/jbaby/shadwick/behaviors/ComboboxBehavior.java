package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.components.form.SwCombobox;
import dev.jbaby.shadwick.util.LocalizedText;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/** Searchable single/multiple select enhancement for caller-owned native markup. */
public class ComboboxBehavior extends StyledBehavior {
    public static final JavaScriptResourceReference COMBOBOX_JS = new JavaScriptResourceReference(Shadwick.class, "js/combobox.js");
    private IModel<String> placeholder;
    private boolean customPlaceholder;
    private IModel<String> empty;
    private IModel<String> searchUrl;
    private boolean showClear, autoHighlight;
    public ComboboxBehavior() { super("sw-combobox"); }
    public String getPlaceholderText() { return !customPlaceholder ? "Search…" : placeholder == null || placeholder.getObject() == null ? "" : placeholder.getObject(); }
    /** Resolve the default for the rendering component; explicit empty/null placeholders stay empty. */
    public String getPlaceholderText(Component component) {
        return customPlaceholder ? getPlaceholderText() : LocalizedText.get(component, SwCombobox.class, "SwCombobox.placeholder", "Search…");
    }
    public ComboboxBehavior setPlaceholder(String value) { return setPlaceholder(Model.of(value)); }
    public ComboboxBehavior setPlaceholder(IModel<String> value) { placeholder = value; customPlaceholder = true; return this; }
    public ComboboxBehavior setEmpty(String value) { return setEmpty(Model.of(value)); }
    public ComboboxBehavior setEmpty(IModel<String> value) { empty = value; return this; }
    /** Same-origin JSON endpoint; typed fields install their Wicket callback automatically. */
    public ComboboxBehavior setSearchUrl(String value) { return setSearchUrl(Model.of(value)); }
    public ComboboxBehavior setSearchUrl(IModel<String> value) { searchUrl = value; return this; }
    public ComboboxBehavior setShowClear(boolean value) { showClear = value; return this; }
    public ComboboxBehavior setAutoHighlight(boolean value) { autoHighlight = value; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("data-placeholder", getPlaceholderText(component));
        tag.put("data-empty", empty == null ? LocalizedText.get(component, SwCombobox.class, "SwCombobox.empty", "No items found.") : empty.getObject());
        tag.put("data-show-clear", String.valueOf(showClear));
        tag.put("data-auto-highlight", String.valueOf(autoHighlight));
        if (searchUrl != null && searchUrl.getObject() != null) tag.put("data-search-url", searchUrl.getObject());
        for (String[] text : new String[][] {
                {"searching","Searching…"}, {"search-error","The options could not be loaded."},
                {"result","1 result"}, {"results","{0} results"}, {"remove-label","Remove {0}"},
                {"clear","Clear selection"}, {"toggle","Show options"}})
            tag.put("data-" + text[0], LocalizedText.get(component, SwCombobox.class, "SwCombobox." + text[0], text[1]));
    }
    @Override public void detach(Component component) {
        if (placeholder != null) placeholder.detach();
        if (empty != null) empty.detach();
        if (searchUrl != null) searchUrl.detach();
        super.detach(component);
    }
    @Override public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
        response.render(JavaScriptHeaderItem.forReference(COMBOBOX_JS));
    }
}
