package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwCombobox;
import dev.jbaby.shadwick.components.form.SwMultiCombobox;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.*;

/** Searchable single/multiple fields with normal/Ajax and server search. */
public class ComboboxPage extends BasePage {
    public record Place(String id,String name,String group,boolean disabled) implements Serializable {}
    private static final List<Place> PLACES=List.of(new Place("berlin","Berlin","Europe",false),new Place("dublin","Dublin","Europe",false),
            new Place("sao","São Paulo","Americas",false),new Place("seattle","Seattle","Americas",false),new Place("locked","New York · unavailable","Americas",true),
            new Place("tokyo","Tokyo","Asia",false),new Place("turkiye","Türkiye","Europe",false));
    public static class Settings implements Serializable { public Place place; public List<String> tags=new ArrayList<>(); }
    public ComboboxPage(PageParameters parameters) {
        super(parameters);
        var saved=new Label("saved",Model.of(""));
        var form=new Form<Settings>("form",new CompoundPropertyModel<>(new Settings())) {
            @Override protected void onSubmit() { saved.setDefaultModelObject(summary(getModelObject())); }
        };
        fields(form);form.add(new Button("save").add(Sw.Behaviors.button()),saved);add(form);
        var ajaxSaved=new Label("saved",Model.of(""));ajaxSaved.setOutputMarkupId(true);
        var ajax=new Form<Settings>("ajaxForm",new CompoundPropertyModel<>(new Settings()));fields(ajax);
        ajax.add(SwSubmitButton.onSubmit("save","Save choices",target->{ajaxSaved.setDefaultModelObject(summary(ajax.getModelObject()));target.add(ajaxSaved);}),ajaxSaved);add(ajax);
        IModel<Place> live=Model.of(PLACES.get(0));var current=new Label("current",(IModel<String>)()->"Current: "+(live.getObject()==null?"none":live.getObject().name()));current.setOutputMarkupId(true);
        IModel<List<Place>> options=Model.ofList(PLACES);
        var liveField=new SwCombobox<>("place","Live city",live,options,new ChoiceRenderer<>("name","id"))
                .setGroup(Place::group).setDisabledWhen(Place::disabled).setShowClear(true).setPlaceholder("Search cities").setRequired(true);
        liveField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override protected void onUpdate(AjaxRequestTarget target){target.add(current);}
        });
        var liveForm=new Form<Void>("liveForm");liveForm.add(liveField,current,SwButton.onClick("refresh","Refresh cities",target->{
            options.setObject(options.getObject().size()==PLACES.size()?PLACES.subList(0,2):PLACES);
            if(!options.getObject().contains(live.getObject()))live.setObject(null);target.add(liveField,current);
        }));add(liveForm);
        IModel<Place> country=Model.of(PLACES.get(0));IModel<List<Place>> destinations=Model.ofList(new ArrayList<>(List.of(PLACES.get(1))));
        var remoteForm=new Form<Void>("remoteForm");
        var remote=new SwCombobox<Place>("remote","Server city",country).setChoices(ComboboxPage::search)
                .setChoiceId(Place::id).setChoiceResolver(ComboboxPage::resolve).setDisplay(Place::name).setGroup(Place::group).setIcon(p->"map-pin")
                .setDisabledWhen(Place::disabled).setMaxChoices(5).setAutoHighlight(true).setShowClear(true).setPlaceholder("Search the server")
                .setDescription("Searches after a short pause. Only the first two cities are loaded initially.");
        var many=new SwMultiCombobox<Place>("destinations","Server destinations",destinations).setChoices(ComboboxPage::search)
                .setChoiceId(Place::id).setChoiceResolver(ComboboxPage::resolve).setDisplay(Place::name).setGroup(Place::group)
                .setDisabledWhen(Place::disabled).setMaxChoices(5).setAutoHighlight(true).setShowClear(true).setPlaceholder("Add destinations");
        var remoteSaved=new Label("saved",Model.of(""));remoteSaved.setOutputMarkupId(true);
        remoteForm.add(remote,many,remoteSaved,SwSubmitButton.onSubmit("save","Save destinations",target->{
            remoteSaved.setDefaultModelObject("Saved: "+(country.getObject()==null?"none":country.getObject().name())+"; "+destinations.getObject().stream().map(Place::name).toList());target.add(remoteSaved);
        }));add(remoteForm);
        var managed=new SwMultiCombobox<>("managed","Managed tags",Model.ofList(new ArrayList<>(List.of("Design","Engineering"))),Model.ofList(List.of("Design","Engineering","Support"))).setShowClear(true);
        managed.setEnabled(false);add(managed);
        add(new SwCombobox<>("dialogPlace","Dialog city",Model.of(PLACES.get(0)),Model.ofList(PLACES),new ChoiceRenderer<>("name","id")));
        add(new SwCombobox<>("popoverPlace","Popover city",Model.of(PLACES.get(0)),Model.ofList(PLACES),new ChoiceRenderer<>("name","id")));
    }
    private static void fields(Form<Settings> form) {
        form.add(new SwCombobox<>("place","City",null,Model.ofList(PLACES),new ChoiceRenderer<Place>("name","id"))
                .setGroup(Place::group).setIcon(p->"map-pin").setDisabledWhen(Place::disabled).setSeparatorBefore(p->p.id().equals("sao"))
                .setPlaceholder("Find a city").setShowClear(true).setRequired(true).setDescription("Type to filter; choose a result. Case and accents are ignored."));
        form.add(new SwMultiCombobox<String>("tags","Teams",null,Model.ofList(List.of("Design","Engineering","Support","Success","Research")))
                .setPlaceholder("Add teams").setAutoHighlight(true).setShowClear(true).setRequired(true).setDescription("Choose several teams. Use Backspace or the remove buttons to edit chips."));
    }
    private static String summary(Settings settings){return "Saved: "+settings.place.name()+"; "+String.join(", ",settings.tags);}
    private static List<Place> search(String query){return query.isBlank()?PLACES.subList(0,2):PLACES.stream().filter(p->normalize(p.name()).contains(normalize(query))).toList();}
    private static String normalize(String value){return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
    private static Place resolve(String id){return PLACES.stream().filter(p->p.id().equals(id)).findFirst().orElse(null);}
    @Override protected String getTitle(){return "Combobox";}
}
