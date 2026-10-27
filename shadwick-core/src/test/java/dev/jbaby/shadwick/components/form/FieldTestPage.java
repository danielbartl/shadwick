package dev.jbaby.shadwick.components.form;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;

import java.io.Serializable;

public class FieldTestPage extends WebPage {

    public static class Person implements Serializable {
        public String name;
        public Integer age;
        public String nickname;
        public String bio;
    }

    public final Person person = new Person();
    public final SwTextField<String> name;
    public final SwTextField<Integer> age;
    public final SwTextField<String> nickname;
    public final SwTextArea<String> bio;

    public FieldTestPage() {
        Form<Person> form = new Form<>("form", new CompoundPropertyModel<>(person));
        add(form);

        form.add(name = new SwTextField<String>("name").setRequired(true));
        form.add(age = new SwTextField<>("age"));
        form.add(nickname = new SwTextField<>("nickname"));
        form.add(bio = new SwTextArea<String>("bio").setDescription("A few words about you."));

        form.add(new AjaxButton("submit") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                target.add(name, age, nickname, bio);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(name, age, nickname, bio);
            }
        });
    }
}
