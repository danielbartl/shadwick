package dev.jbaby.shadwick.components;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SwStepsTest {
    static final List<String> ORDER=List.of("Ordered","Paid","Shipped","Delivered");
    WicketTester tester;
    @BeforeEach void start(){tester=new WicketTester();tester.getSession().setLocale(Locale.ENGLISH);}
    @AfterEach void stop(){tester.destroy();}
    List<TagTester> items(){return TagTester.createTagsByAttribute(tester.getLastResponseAsString(),"wicket:id","items",false);}
    Component link(int index){return tester.getComponentFromLastRenderedPage("steps:list:items:"+index+":link");}
    @Test void nativeListShowsOrderedTitlesNumbersLocalizedStateAndExactlyOneCurrentStep(){
        tester.startComponentInPage(new SwSteps("steps",ORDER,Model.of(2)));
        var tag=tester.getTagByWicketId("list");assertThat(tag.getName()).isEqualTo("ol");assertThat(tag.getAttribute("role")).isEqualTo("list");assertThat(tag.getAttribute("aria-label")).isEqualTo("Steps");
        assertThat(items()).extracting(t->t.getAttribute("data-state")).containsExactly("done","done","current","upcoming");
        assertThat(items()).extracting(t->t.getAttribute("aria-current")).containsExactly(null,null,"step",null);
        for(int i=0;i<4;i++){
            tester.assertLabel("steps:list:items:"+i+":link:title",ORDER.get(i));tester.assertLabel("steps:list:items:"+i+":link:number",Integer.toString(i+1));
            assertThat(tester.getTagByWicketId("link").getName()).isEqualTo("span");
        }
        tester.assertLabel("steps:list:items:0:link:status","Completed");tester.assertLabel("steps:list:items:2:link:status","Current");tester.assertLabel("steps:list:items:3:link:status","Upcoming");
    }
    @ParameterizedTest @NullSource @ValueSource(ints={-1,-9,Integer.MIN_VALUE})
    void nullAndNegativeCurrentIndicesMeanNotStarted(Integer current){
        tester.startComponentInPage(new SwSteps("steps",ORDER,Model.of(current)));
        assertThat(items()).extracting(t->t.getAttribute("data-state")).containsOnly("upcoming");
        assertThat(items()).allMatch(t->t.getAttribute("aria-current")==null);
    }
    @ParameterizedTest @ValueSource(ints={4,5,Integer.MAX_VALUE})
    void indicesPastTheEndMeanAllCompleted(int current){
        tester.startComponentInPage(new SwSteps("steps",ORDER,Model.of(current)));
        assertThat(items()).extracting(t->t.getAttribute("data-state")).containsOnly("done");
        assertThat(items()).allMatch(t->t.getAttribute("aria-current")==null);
    }
    @Test void onlyCompletedStepsAreLinksAndCallbacksCanUpdateTheSameResponse(){
        var current=Model.of(2);var selected=Model.of(-1);var steps=new SwSteps("steps",ORDER,current);
        steps.setOnSelect((target,index)->{selected.setObject(index);current.setObject(index);target.add(steps);});
        tester.startComponentInPage(steps);assertThat(link(0)).isInstanceOf(AjaxLink.class);assertThat(link(0).isEnabled()).isTrue();assertThat(link(1).isEnabled()).isTrue();assertThat(link(2).isEnabled()).isFalse();assertThat(link(3).isEnabled()).isFalse();
        assertThat(tester.getLastResponseAsString()).contains("<noscript");tester.executeAjaxEvent(link(1),"click");
        assertThat(selected.getObject()).isEqualTo(1);assertThat(current.getObject()).isEqualTo(1);
        assertThat(tester.getLastResponseAsString()).contains("aria-current=\"step\"","data-state=\"current\"");
        tester.assertLabel("steps:list:items:1:link:status","Current");assertThat(link(1).isEnabled()).isFalse();
    }
    @Test void staleCompletedCallbacksCannotSelectNowFutureRemovedOrRenamedSteps(){
        var current=Model.of(2);var titles=Model.ofList(new ArrayList<>(ORDER));var selected=Model.of(-1);
        var steps=new SwSteps("steps",titles,current).setOnSelect((target,index)->selected.setObject(index));tester.startComponentInPage(steps);
        current.setObject(0);((AjaxLink<?>)link(1)).onClick(null);assertThat(selected.getObject()).isEqualTo(-1);
        current.setObject(2);titles.setObject(List.of("Different","Renamed"));tester.getLastRenderedPage().detach();((AjaxLink<?>)link(1)).onClick(null);assertThat(selected.getObject()).isEqualTo(-1);
        titles.setObject(List.of());tester.getLastRenderedPage().detach();((AjaxLink<?>)link(1)).onClick(null);assertThat(selected.getObject()).isEqualTo(-1);
    }
    @Test void clearingTheCallbackAndDisablingThePanelBlockOldLinks(){
        var selected=Model.of(-1);var steps=new SwSteps("steps",ORDER,Model.of(2)).setOnSelect((target,index)->selected.setObject(index));tester.startComponentInPage(steps);
        steps.setOnSelect(null);((AjaxLink<?>)link(0)).onClick(null);assertThat(selected.getObject()).isEqualTo(-1);
        steps.setOnSelect((target,index)->selected.setObject(index));steps.setEnabled(false);((AjaxLink<?>)link(0)).onClick(null);assertThat(selected.getObject()).isEqualTo(-1);
    }
    @Test void modelsAreReadOnceForEachRenderAndDetachWhileHidden(){
        var titles=new CountingModel<List<String>>(ORDER);var current=new CountingModel<>(1);var label=new CountingModel<>("Order status");
        var steps=new SwSteps("steps",titles,current).setLabel(label);tester.startComponentInPage(steps);
        assertThat(titles.reads).isEqualTo(1);assertThat(current.reads).isEqualTo(1);assertThat(titles.detaches).isEqualTo(1);assertThat(current.detaches).isEqualTo(1);assertThat(label.detaches).isEqualTo(1);
        steps.setVisible(false);tester.startPage(tester.getLastRenderedPage());assertThat(titles.reads).isEqualTo(1);assertThat(current.reads).isEqualTo(1);assertThat(titles.detaches).isEqualTo(2);assertThat(current.detaches).isEqualTo(2);assertThat(label.detaches).isEqualTo(2);
    }
    @Test void nullEmptyAndSingleListsAreSupportedAndModelChangesRepopulate(){
        var titles=Model.<String>ofList(null);var steps=new SwSteps("steps",titles,Model.of(0));tester.startComponentInPage(steps);assertThat(items()).isEmpty();
        titles.setObject(List.of("Only"));tester.startPage(tester.getLastRenderedPage());assertThat(items()).hasSize(1);tester.assertLabel("steps:list:items:0:link:title","Only");
        titles.setObject(List.of());tester.startPage(tester.getLastRenderedPage());assertThat(items()).isEmpty();
    }
    @Test void titlesAndLabelsAreEscapedAndBlankLabelsUseTheDefault(){
        var label=Model.of("Order <status>");var steps=new SwSteps("steps",List.of("<script>literal</script>",""),Model.of(0)).setLabel(label);tester.startComponentInPage(steps);
        assertThat(tester.getLastResponseAsString()).contains("&lt;script&gt;literal&lt;/script&gt;","Order &lt;status&gt;");
        label.setObject(" ");tester.startPage(tester.getLastRenderedPage());assertThat(tester.getTagByWicketId("list").getAttribute("aria-label")).isEqualTo("Steps");
        steps.setLabel("Custom");tester.startPage(tester.getLastRenderedPage());assertThat(tester.getTagByWicketId("list").getAttribute("aria-label")).isEqualTo("Custom");
    }
    @ParameterizedTest @CsvSource({"en,Steps,Completed,Current,Upcoming","de,Schritte,Abgeschlossen,Aktuell,Ausstehend","fr,Étapes,Terminée,En cours,À venir","es,Pasos,Completado,Actual,Próximo","it,Passaggi,Completato,Attuale,Successivo","ja,ステップ,完了,現在,未完了","nl,Stappen,Voltooid,Huidig,Komend","pt,Etapas,Concluída,Atual,Próxima","hr,Koraci,Dovršeno,Trenutačno,Slijedi"})
    void everyLanguageNamesTheListAndAllStates(String lang,String label,String done,String current,String upcoming){
        tester.getSession().setLocale(Locale.forLanguageTag(lang));tester.startComponentInPage(new SwSteps("steps",ORDER,Model.of(1)));
        assertThat(tester.getTagByWicketId("list").getAttribute("aria-label")).isEqualTo(label);
        tester.assertLabel("steps:list:items:0:link:status",done);tester.assertLabel("steps:list:items:1:link:status",current);tester.assertLabel("steps:list:items:2:link:status",upcoming);
    }
    @Test void requiredModelReferencesAreRejected(){
        assertThatThrownBy(()->new SwSteps("steps",(IModel<List<String>>)null,Model.of(0))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(()->new SwSteps("steps",ORDER,null)).isInstanceOf(NullPointerException.class);
    }
    static class CountingModel<T> implements IModel<T>{T value;int reads,detaches;CountingModel(T value){this.value=value;}@Override public T getObject(){reads++;return value;}@Override public void detach(){detaches++;}}
}
