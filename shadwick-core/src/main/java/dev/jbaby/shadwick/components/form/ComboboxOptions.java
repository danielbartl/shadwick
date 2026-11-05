package dev.jbaby.shadwick.components.form;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.behaviors.ComboboxBehavior;
import dev.jbaby.shadwick.components.SwIcon;
import dev.jbaby.shadwick.util.SerializableFunction;
import dev.jbaby.shadwick.util.SerializableSupplier;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;
import java.util.*;

/** Shared choice identity, markup and bounded search; never stores search results on the page. */
final class ComboboxOptions<T> implements IChoiceRenderer<T> {
    final ComboboxBehavior appearance = new ComboboxBehavior();
    final IChoiceRenderer<? super T> renderer;
    final IModel<? extends List<? extends T>> choices;
    SerializableFunction<String, ? extends List<? extends T>> search;
    SerializableFunction<? super T, String> display, id, group, icon;
    SerializableFunction<String, ? extends T> resolver;
    SerializableFunction<? super T, Boolean> disabled, separator;
    int maxChoices = 10;
    final AbstractAjaxBehavior callback = new AbstractAjaxBehavior() {
        @Override public void onRequest() {
            if (!getComponent().isEnabledInHierarchy() || !getComponent().isVisibleInHierarchy() || search == null)
                throw new org.apache.wicket.authorization.UnauthorizedActionException(getComponent(), org.apache.wicket.Component.ENABLE);
            requireRemoteIdentity();
            String query = RequestCycle.get().getRequest().getRequestParameters().getParameterValue("q").toString("");
            if (query.length() > 256) query = query.substring(0,256);
            List<? extends T> found = search.apply(query);
            JSONArray result = new JSONArray(); Set<String> seen = new HashSet<>();
            if (found != null) for (T choice : found.stream().limit(maxChoices).toList()) {
                String key = getIdValue(choice, -1);
                if (!seen.add(key)) continue;
                JSONObject row = new JSONObject().put("id",key).put("text",Objects.toString(getDisplayValue(choice), ""))
                        .put("disabled",isDisabled(choice));
                if (group != null) row.put("group",group.apply(choice));
                if (separator != null) row.put("separator",Boolean.TRUE.equals(separator.apply(choice)));
                if (icon != null && icon.apply(choice) != null) row.put("icon",SwIcon.decorativeMarkup(icon.apply(choice)));
                result.put(row);
            }
            RequestCycle.get().scheduleRequestHandlerAfterCurrent(new TextRequestHandler("application/json", "UTF-8", result.toString()));
        }
    };
    ComboboxOptions(IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this.choices = choices;
        this.renderer = renderer == null ? new ChoiceRenderer<>() : renderer;
        appearance.setSearchUrl((IModel<String>) () -> search == null ? null : callback.getCallbackUrl().toString());
    }
    void requireRemoteIdentity() {
        if (search != null && (id == null || resolver == null))
            throw new IllegalStateException("Remote combobox needs setChoiceId(...) and setChoiceResolver(...) for stable, freshly validated choices.");
    }
    @Override public Object getDisplayValue(T choice) { return display == null ? renderer.getDisplayValue(choice) : display.apply(choice); }
    @Override public String getIdValue(T choice, int index) {
        String key = id == null ? renderer.getIdValue(choice,index) : id.apply(choice);
        if (Strings.isEmpty(key)) throw new IllegalArgumentException("Combobox choice IDs must be nonempty and unique");
        return key;
    }
    @Override public T getObject(String key, IModel<? extends List<? extends T>> current) {
        return resolve(key,current);
    }
    boolean isDisabled(T choice) { return disabled != null && Boolean.TRUE.equals(disabled.apply(choice)); }
    @SuppressWarnings("unchecked")
    T resolve(String key, IModel<? extends List<? extends T>> current) {
        if (Strings.isEmpty(key)) return null;
        if (search != null) {
            requireRemoteIdentity();
            T value = resolver.apply(key);
            if (value != null && key.equals(getIdValue(value,-1)) && !isDisabled(value)) return value;
        } else {
            List<? extends T> available = current.getObject();
            for (int i=0;i<available.size();i++) {
                T value=available.get(i);
                if (key.equals(getIdValue(value,i))) {
                    if (isDisabled(value)) break;
                    if (id != null) return value;
                    T converted = (T) renderer.getObject(key,current);
                    if (converted != null && !isDisabled(converted)) return converted;
                    break;
                }
            }
        }
        throw new ConversionException("The selected option is unavailable.").setResourceKey("SwCombobox.unavailable");
    }
    IModel<List<? extends T>> snapshot(SerializableSupplier<Collection<T>> selected) {
        return new IModel<>() {
            private transient List<? extends T> current;
            @Override public List<? extends T> getObject() {
                if (current == null) {
                    requireRemoteIdentity();
                    List<? extends T> loaded = choices == null ? search == null ? List.of() : search.apply("") : choices.getObject();
                    List<T> values = new ArrayList<>(loaded == null ? List.of() : search == null ? loaded : loaded.stream().limit(maxChoices).toList());
                    if (search != null) {
                        Collection<T> chosen = selected.get();
                        if (chosen != null) for (T value : chosen) if (value != null && values.stream().noneMatch(v -> getIdValue(v,-1).equals(getIdValue(value,-1)))) values.add(value);
                    }
                    Set<String> seen = new HashSet<>();
                    for (int i=0;i<values.size();i++) if (!seen.add(getIdValue(values.get(i),i))) throw new IllegalArgumentException("Combobox choice IDs must be unique");
                    current = List.copyOf(values);
                }
                return current;
            }
            @Override public void detach() { current=null; if (choices != null) choices.detach(); }
        };
    }
    String decorate(String markup,T choice) {
        int start=markup.indexOf('>')+1;
        String name=icon == null ? null : icon.apply(choice);
        return markup.substring(0,start).replace("<option ","<option class=\"sw-combobox-item\" ")
                +(name == null ? "" : SwIcon.decorativeMarkup(name))+markup.substring(start);
    }
    String render(List<? extends T> values, SerializableFunction<Integer,String> option) {
        StringBuilder html=new StringBuilder(); String previous=null;
        for (int i=0;i<values.size();i++) {
            T value=values.get(i); String heading=group == null ? null : group.apply(value);
            if (heading != null && heading.isBlank()) heading=null;
            boolean split=separator != null && Boolean.TRUE.equals(separator.apply(value));
            if (!Objects.equals(previous,heading)||split) {
                if (previous != null) html.append("</optgroup>");
                if (split && i>0) html.append("<hr class=\"sw-combobox-separator\">");
                if (heading != null) html.append("<optgroup class=\"sw-combobox-group\" label=\"").append(Strings.escapeMarkup(heading)).append("\">");
            }
            previous=heading; html.append(option.apply(i));
        }
        if (previous != null) html.append("</optgroup>");
        return html.toString();
    }
}
