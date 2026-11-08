package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import java.io.Serializable;
import java.util.*;

/** Groups compose existing avatars and ordinary Wicket actions; overflow belongs to this example. */
public class AvatarGroupPage extends BasePage {
    private boolean expanded;
    private final String picture;
    private final Label receipt = new Label("receipt", Model.of(""));
    public AvatarGroupPage(PageParameters parameters) {
        super(parameters);
        picture=urlFor(new PackageResourceReference(DisplayPage.class,"avatar.svg"),null).toString();
        receipt.setOutputMarkupId(true);add(receipt);
        var people=people();
        add(group("small",Model.ofList(List.copyOf(people.subList(0,3))),SwAvatarGroup.Size.SMALL,false).setLabel("Small team"));
        add(group("standard",Model.ofList(List.copyOf(people.subList(0,3))),SwAvatarGroup.Size.DEFAULT,false).setLabel("Default team"));
        add(group("large",Model.ofList(List.copyOf(people.subList(0,3))),SwAvatarGroup.Size.LARGE,false).setLabel("Large team"));
        var team=group("team",()->expanded?people:people.subList(0,3),SwAvatarGroup.Size.DEFAULT,true).setLabel("Project team");
        var overflow=new WebMarkupContainer("overflow"){
            @Override protected void onConfigure(){super.onConfigure();setVisible(!expanded);}
        };
        var more=new Link<Void>("more"){
            @Override public void onClick(){expanded=true;receipt.setDefaultModelObject("Showing all five team members.");}
        };
        more.add(AttributeModifier.replace("aria-label","Show 2 more members (+2)"));overflow.add(more);team.add(overflow);add(team);
        var dynamicMembers=Model.ofList(List.copyOf(people.subList(0,3)));
        var changing=new WebMarkupContainer("changing");changing.setOutputMarkupId(true);add(changing);
        var dynamic=group("dynamic",dynamicMembers,SwAvatarGroup.Size.DEFAULT,false).setLabel("Current reviewers");changing.add(dynamic);
        changing.add(new Label("empty","No reviewers assigned."){
            @Override protected void onConfigure(){super.onConfigure();setVisible(dynamicMembers.getObject().isEmpty());}
        });
        add(SwButton.onClick("clear","Clear reviewers",target->{dynamicMembers.setObject(List.of());target.add(changing);}));
        add(SwButton.onClick("restore","Restore reviewers",target->{dynamicMembers.setObject(List.copyOf(people.subList(0,3)));target.add(changing);}));
        add(SwButton.onClick("replace","Replace reviewer group",target->{
            changing.replace(group("dynamic",dynamicMembers,SwAvatarGroup.Size.LARGE,false).setLabel("Current reviewers"));target.add(changing);
        }));
        var many=new ArrayList<Member>();for(int i=1;i<=20;i++)many.add(new Member("Reviewer "+i,false));
        add(group("wrapping",Model.ofList(many),SwAvatarGroup.Size.LARGE,false).setLabel("All reviewers"));
        var fixed=new WebMarkupContainer("fixed");fixed.add(Sw.Behaviors.avatarGroup().setLabel("Fixed team"));
        fixed.add(new SwAvatar("ada","AL").setLabel("Ada Lovelace"),new SwAvatar("grace","GH").setLabel("Grace Hopper"));add(fixed);
    }
    private SwAvatarGroup<Member> group(String id,IModel<List<Member>> members,SwAvatarGroup.Size size,boolean links){
        return SwAvatarGroup.of(id,members,size,(item,member)->{
            // Deliberately different child size: the group controls its members' final size.
            var avatar=new SwAvatar("avatar",member.picture?Model.of(picture):null,Model.of(SwAvatar.initialsOf(member.name)),SwAvatar.Size.SMALL).setLabel(member.name);
            if(links){
                var link=new Link<Void>("person"){
                    @Override public void onClick(){receipt.setDefaultModelObject("Selected "+member.name+".");}
                };
                link.add(AttributeModifier.replace("aria-label","View "+SwAvatar.initialsOf(member.name)+": "+member.name),AttributeModifier.replace("title",member.name));
                avatar.add(AttributeModifier.replace("aria-hidden","true"));link.add(avatar);item.add(link);
            }else item.add(avatar);
        });
    }
    private static List<Member> people(){return List.of(new Member("Ada Lovelace",true),new Member("Grace Hopper",false),new Member("Katherine Johnson",false),new Member("山田 太郎",false),new Member("Sofía Martínez",false));}
    private record Member(String name,boolean picture) implements Serializable{}
    @Override protected String getTitle(){return "Avatar groups";}
}
