package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SwAvatarGroupTest {
    WicketTester tester;
    @BeforeEach void start(){tester=new WicketTester();}
    @AfterEach void stop(){tester.destroy();}
    <T> void render(SwAvatarGroup<T> group){tester.startComponentInPage(group,Markup.of("<ul wicket:id='group'><li wicket:id='avatars'><span wicket:id='avatar'></span></li></ul>"));}
    SwAvatarGroup<String> group(IModel<List<String>> model){return SwAvatarGroup.of("group",model,(item,name)->item.add(new SwAvatar("avatar",SwAvatar.initialsOf(name)).setLabel(name)));}
    @Test void repeatsExistingAvatarsInTheSuppliedOrderWithNativeListSemantics(){
        render(group(Model.ofList(List.of("Ada Lovelace","Grace Hopper"))).setLabel("Team"));
        var tag=tester.getTagByWicketId("group");assertThat(tag.getAttribute("role")).isEqualTo("list");assertThat(tag.getAttribute("aria-label")).isEqualTo("Team");
        assertThat(tag.getAttribute("class")).contains("sw-avatar-group");
        tester.assertLabel("group:avatars:0:avatar:container:initials","AL");tester.assertLabel("group:avatars:1:avatar:container:initials","GH");
        assertThat(tester.getTagByWicketId("avatars").getAttribute("class")).contains("sw-avatar-group-item");
    }
    @ParameterizedTest @EnumSource(SwAvatarGroup.Size.class)
    void factoriesAndConstructorsAcceptEverySize(SwAvatarGroup.Size size){
        var group=Sw.Components.avatarGroup("group",Model.ofList(List.of("Ada")),size,(item,value)->item.add(new SwAvatar("avatar",value)));
        render(group);assertThat(tester.getTagByWicketId("group").getAttribute("data-size")).isEqualTo(size.getClassName());
        group.setSize(SwAvatarGroup.Size.SMALL);tester.startPage(tester.getLastRenderedPage());assertThat(tester.getTagByWicketId("group").getAttribute("data-size")).isEqualTo("sm");
        group.setSize(SwAvatarGroup.Size.DEFAULT);tester.startPage(tester.getLastRenderedPage());assertThat(tester.getTagByWicketId("group").getAttribute("data-size")).isNull();
    }
    @Test void subclassAndDefaultFacadeKeepTheOatPopulateItemContract(){
        var group=new SwAvatarGroup<String>("group",Model.ofList(List.of("AL"))){
            @Override protected void populateItem(ListItem<String> item){item.add(new SwAvatar("avatar",item.getModelObject()));}
        };render(group);tester.assertLabel("group:avatars:0:avatar:container:initials","AL");
        render(Sw.Components.avatarGroup("group",Model.ofList(List.of("GH")),(item,value)->item.add(new SwAvatar("avatar",value))));
        tester.assertLabel("group:avatars:0:avatar:container:initials","GH");
    }
    @Test void nullAndEmptyListsRenderEmptyAndChangingModelsReplaceItems(){
        var model=Model.<String>ofList(null);var group=group(model);render(group);
        assertThat(((org.apache.wicket.MarkupContainer)group.get("avatars")).size()).isZero();
        model.setObject(List.of("Grace Hopper"));tester.startPage(tester.getLastRenderedPage());tester.assertLabel("group:avatars:0:avatar:container:initials","GH");
        model.setObject(List.of());tester.startPage(tester.getLastRenderedPage());assertThat(((org.apache.wicket.MarkupContainer)group.get("avatars")).size()).isZero();
    }
    @Test void modelsReadOncePerRequestAndDetachIncludingWhenHidden(){
        var members=new CountingModel<List<String>>(List.of("Ada Lovelace"));var label=new CountingModel<>("Team");
        var group=group(members).setLabel(label);render(group);assertThat(members.reads).isEqualTo(1);assertThat(members.detaches).isEqualTo(1);assertThat(label.detaches).isEqualTo(1);
        group.setVisible(false);tester.startPage(tester.getLastRenderedPage());assertThat(members.reads).isEqualTo(1);assertThat(members.detaches).isEqualTo(2);assertThat(label.detaches).isEqualTo(2);
        group.setVisible(true);tester.startPage(tester.getLastRenderedPage());assertThat(members.reads).isEqualTo(2);
    }
    @Test void snapshotPreventsPopulationFromMutatingTheCurrentIteration(){
        var values=new ArrayList<>(List.of("Ada","Grace"));
        var group=SwAvatarGroup.of("group",Model.ofList(values),(item,value)->{if(item.getIndex()==0)values.add("Katherine");item.add(new SwAvatar("avatar",value));});
        render(group);assertThat(((org.apache.wicket.MarkupContainer)group.get("avatars")).size()).isEqualTo(2);assertThat(values).hasSize(3);
    }
    @Test void escapedLabelsAndCallerOwnedAttributesArePreserved(){
        var group=group(Model.ofList(List.of("<Ada>"))).setLabel("Team <all>");render(group);
        assertThat(tester.getLastResponseAsString()).contains("aria-label=\"Team &lt;all&gt;\"");
        assertThat(tester.getTagByWicketId("initials").getAttribute("aria-label")).isEqualTo("<Ada>");
        var fixed=new WebMarkupContainer("group");fixed.add(Sw.Behaviors.avatarGroup());
        tester.startComponentInPage(fixed,Markup.of("<div wicket:id='group' role='region' aria-labelledby='team-heading'></div>"));
        assertThat(tester.getTagByWicketId("group").getAttribute("role")).isEqualTo("region");assertThat(tester.getTagByWicketId("group").getAttribute("aria-labelledby")).isEqualTo("team-heading");
    }
    @Test void behaviorSupportsFixedGroupsAndDetachableLabels(){
        var fixed=new WebMarkupContainer("group");var label=Model.of("Team");fixed.add(Sw.Behaviors.avatarGroup().setLabel(label).setSize(SwAvatarGroup.Size.LARGE));
        tester.startComponentInPage(fixed,Markup.of("<div wicket:id='group' aria-label='old'></div>"));
        assertThat(tester.getTagByWicketId("group").getAttribute("role")).isEqualTo("group");assertThat(tester.getTagByWicketId("group").getAttribute("aria-label")).isEqualTo("Team");
        label.setObject(" ");tester.startPage(tester.getLastRenderedPage());assertThat(tester.getTagByWicketId("group").getAttribute("aria-label")).isNull();
    }
    @Test void itemVisibilityAndExtraCountMarkupRemainApplicationOwned(){
        var group=SwAvatarGroup.of("group",Model.ofList(List.of("Ada","Hidden")),(item,value)->{item.setVisible(!value.equals("Hidden"));item.add(new SwAvatar("avatar",value));});
        group.add(new Label("more","+3").add(new StyledBehavior("sw-avatar-group-count")));
        tester.startComponentInPage(group,Markup.of("<ul wicket:id='group'><li wicket:id='avatars'><span wicket:id='avatar'></span></li><li class='sw-avatar-group-item'><span wicket:id='more'></span></li></ul>"));
        tester.assertInvisible("group:avatars:1");tester.assertLabel("group:more","+3");
    }
    @Test void invalidConfigurationFailsEarly(){
        assertThatThrownBy(()->group(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(()->group(Model.ofList(List.of())).setSize(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(()->SwAvatarGroup.of("group",Model.ofList(List.of()),null)).isInstanceOf(NullPointerException.class);
        assertThat(SwAvatarGroup.Size.SMALL.getClassName()).isEqualTo("sm");assertThat(SwAvatarGroup.Size.LARGE.getClassName()).isEqualTo("lg");
    }
    static class CountingModel<T> implements IModel<T>{
        T value;int reads,detaches;CountingModel(T value){this.value=value;}
        @Override public T getObject(){reads++;return value;}
        @Override public void detach(){detaches++;}
    }
}
