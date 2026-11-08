package dev.jbaby.shadwick.examples;

import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class AvatarGroupPageTest {
    WicketTester tester;
    @BeforeEach void start(){tester=new WicketTester(new ExamplesApplication());}
    @AfterEach void stop(){tester.destroy();}
    @Test void groupsShowAllSizesAndApplicationOwnedOverflow(){
        tester.startPage(AvatarGroupPage.class);var page=tester.getLastRenderedPage();
        assertThat(((ListView<?>)page.get("team:avatars")).size()).isEqualTo(3);
        tester.clickLink("team:overflow:more");assertThat(((ListView<?>)page.get("team:avatars")).size()).isEqualTo(5);
        assertThat(page.get("team:overflow").isVisible()).isFalse();
        assertThat(page.get("receipt").getDefaultModelObjectAsString()).isEqualTo("Showing all five team members.");
    }
    @Test void memberLinksWorkAsNativeWicketLinks(){
        tester.startPage(AvatarGroupPage.class);tester.clickLink("team:avatars:0:person");
        assertThat(tester.getLastRenderedPage().get("receipt").getDefaultModelObjectAsString()).isEqualTo("Selected Ada Lovelace.");
    }
    @Test void ajaxClearRestoreAndReplacementKeepTheCurrentTree(){
        tester.startPage(AvatarGroupPage.class);var page=tester.getLastRenderedPage();var old=page.get("changing:dynamic");
        tester.executeAjaxEvent("clear","click");assertThat(((ListView<?>)page.get("changing:dynamic:avatars")).size()).isZero();
        assertThat(page.get("changing:empty").isVisible()).isTrue();
        tester.executeAjaxEvent("restore","click");assertThat(((ListView<?>)page.get("changing:dynamic:avatars")).size()).isEqualTo(3);
        tester.executeAjaxEvent("replace","click");assertThat(page.get("changing:dynamic")).isNotSameAs(old);
    }
    @Test void renderedPageKeepsNativeAndAjaxCallbacksAfterSerialization(){
        tester.startPage(AvatarGroupPage.class);var serializer=tester.getApplication().getFrameworkSettings().getSerializer();
        var restored=(AvatarGroupPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));tester.startPage(restored);
        tester.clickLink("team:avatars:1:person");assertThat(restored.get("receipt").getDefaultModelObjectAsString()).isEqualTo("Selected Grace Hopper.");
        tester.executeAjaxEvent("clear","click");tester.executeAjaxEvent("restore","click");tester.executeAjaxEvent("replace","click");
        assertThat(((ListView<?>)restored.get("changing:dynamic:avatars")).size()).isEqualTo(3);
    }
    @Test void catalogExposesPackagedSourceAndTheAvatarGuide(){
        assertThat(CatalogPages.find(AvatarGroupPage.class).route()).isEqualTo("avatar-groups");
        assertThat(ExampleSources.files(AvatarGroupPage.class)).extracting(ExampleSources.File::name).contains("AvatarGroupPage.java","AvatarGroupPage.html");
    }
}
