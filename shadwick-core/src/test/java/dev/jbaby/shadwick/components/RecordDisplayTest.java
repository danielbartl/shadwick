package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.TimelineEventBehavior;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class RecordDisplayTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    List<TagTester> tags(String id) { return TagTester.createTagsByAttribute(tester.getLastResponseAsString(), "wicket:id", id, false); }
    List<String> values(String id) { return tags(id).stream().map(TagTester::getValue).toList(); }
    String normalized(String value) { return value.replace('\u202f', ' ').replace('\u00a0', ' '); }
    public static class Customer implements Serializable {
        public String name = "Fern <Field>";
        public String vatId;
        public LocalDate since = LocalDate.of(2019, 3, 1);
        public BigDecimal total = new BigDecimal("1234.56789");
    }
    public static class DetailsPage extends WebPage implements IMarkupResourceStreamProvider {
        final Model<Customer> record = Model.of(new Customer());
        final SwDescriptionList details = new SwDescriptionList("details", record)
                .addProperty("name").addProperty("vatId").addProperty("since").addProperty("total")
                .addItem("Blank", Model.of(" \n ")).addItem("Unsafe <label>", Model.of("<script>alert(1)</script>"))
                .addItem(Model.of("Status"), id -> new SwBadge(id, "Active"));
        public DetailsPage() { add(details, SwButton.onClick("refresh", "Refresh", target -> target.add(details))); }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c, Class<?> type) {
            return new StringResourceStream("<html><body><dl wicket:id='details'></dl><button wicket:id='refresh'></button></body></html>");
        }
    }
    @Test void nativeDetailsUseResourceLabelsEmptyFallbackAndEscapedCustomValues() {
        tester.startPage(DetailsPage.class);
        assertThat(tester.getTagByWicketId("details").getName()).isEqualTo("dl");
        assertThat(tester.getTagByWicketId("details").getAttribute("class")).contains("sw-description-list");
        assertThat(tags("term")).allMatch(t -> t.getName().equals("dt"));
        assertThat(values("term")).containsExactly("Customer name", "vatId", "Customer since", "total", "Blank", "Unsafe &lt;label&gt;", "Status");
        assertThat(values("value")).containsExactly("Fern &lt;Field&gt;", "—", "Mar 1, 2019", "1,234.56789", "—", "&lt;script&gt;alert(1)&lt;/script&gt;", "Active");
        assertThat(tester.getLastResponseAsString()).doesNotContain("<script>alert(1)", "style=").doesNotContainPattern("\\son[a-z]+=");
    }
    @Test void changedRecordModelsAndLocalesRenderFreshPropertiesOnAjax() {
        var p = tester.startPage(DetailsPage.class); var replacement = new Customer(); replacement.name = "New customer";
        replacement.total = new BigDecimal("9876.54321"); p.record.setObject(replacement);
        tester.getSession().setLocale(Locale.GERMANY); tester.clickLink("refresh");
        assertThat(values("term")).startsWith("Kundenname");
        assertThat(values("value")).contains("New customer", "9.876,54321", "01.03.2019");
    }
    @Test void valueModelsLoadOncePerRequestAndDetachEvenWhenTheirRowIsHidden() {
        int[] count = {0, 0};
        IModel<BigDecimal> source = new IModel<>() {
            @Override public BigDecimal getObject() { count[0]++; return new BigDecimal("42.12345"); }
            @Override public void detach() { count[1]++; }
        };
        var details = new SwDescriptionList("details").addItem("Amount", source);
        tester.startComponentInPage(details, Markup.of("<dl wicket:id='details'></dl>"));
        assertThat(count).containsExactly(1, 1);
        details.get("items:1").setVisible(false); tester.startPage(tester.getLastRenderedPage());
        assertThat(count[0]).isEqualTo(1); assertThat(count[1]).isEqualTo(2);
    }
    @Test void factoriesValidateIdsBeforeAddingARowAndWrongRootTagsFailClearly() {
        var details = new SwDescriptionList("details");
        assertThatIllegalArgumentException().isThrownBy(() -> details.addItem(Model.of("Name"), id -> new Label("wrong", "Value")));
        assertThatIllegalArgumentException().isThrownBy(() -> details.addItem(Model.of("Name"), id -> (org.apache.wicket.Component)null));
        details.addItem("Name", Model.of("Value")); tester.startComponentInPage(details, Markup.of("<dl wicket:id='details'></dl>"));
        assertThat(values("term")).containsExactly("Name");
        assertThatThrownBy(() -> tester.startComponentInPage(new SwDescriptionList("bad"), Markup.of("<div wicket:id='bad'></div>")))
                .hasStackTraceContaining("requires a <dl>");
    }
    record Event(String title, LocalDateTime at, String detail, TimelineEventBehavior.Variant variant) implements Serializable {}
    public static class HistoryPage extends WebPage implements IMarkupResourceStreamProvider {
        final IModel<List<Event>> events = Model.ofList(new ArrayList<>(List.of(
                new Event("Invoice <sent>", LocalDateTime.of(2026, 10, 1, 9, 30), "By <Jordan>", null),
                new Event("Payment received", LocalDateTime.of(2026, 10, 9, 14, 5), null, TimelineEventBehavior.Variant.SECONDARY),
                new Event("Delivery failed", null, "  ", TimelineEventBehavior.Variant.DESTRUCTIVE))));
        final SwTimeline<Event> timeline = new SwTimeline<>("timeline", events, Event::title, Event::at)
                .setDescription(Event::detail).setVariant(Event::variant);
        public HistoryPage() { add(timeline, SwButton.onClick("refresh", "Refresh", target -> target.add(timeline))); }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='timeline'></div><button wicket:id='refresh'></button></body></html>");
        }
    }
    @Test void historyPreservesOrderAndNativeTimeSemanticsAndOmitsBlankDescriptions() {
        tester.startPage(HistoryPage.class);
        assertThat(tester.getTagByWicketId("history").getName()).isEqualTo("ol");
        assertThat(tester.getTagByWicketId("history").getAttribute("role")).isEqualTo("list");
        assertThat(tags("events")).allMatch(t -> t.getName().equals("li"));
        assertThat(values("title")).containsExactly("Invoice &lt;sent&gt;", "Payment received", "Delivery failed");
        assertThat(values("description")).containsExactly("By &lt;Jordan&gt;");
        assertThat(tags("time")).hasSize(2).allMatch(t -> t.getName().equals("time"));
        assertThat(tags("time").get(0).getAttribute("datetime")).isEqualTo("2026-10-01T09:30");
        assertThat(normalized(values("time").get(0))).isEqualTo("Oct 1, 2026, 9:30 AM");
        assertThat(tags("events")).extracting(t -> t.getAttribute("data-variant")).containsExactly(null, "secondary", "destructive");
        assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }
    @Test void reorderedAndEmptyHistoriesReplaceWithoutStaleTimesDescriptionsOrMarkers() {
        var p = tester.startPage(HistoryPage.class);
        var reversed = new ArrayList<>(p.events.getObject()); Collections.reverse(reversed); p.events.setObject(reversed);
        tester.clickLink("refresh");
        assertThat(values("title")).containsExactly("Delivery failed", "Payment received", "Invoice &lt;sent&gt;");
        p.events.setObject(new ArrayList<>(List.of(new Event("Only event", null, null, null))));
        tester.clickLink("refresh");
        assertThat(values("title")).containsExactly("Only event");
        assertThat(tags("time")).isEmpty(); assertThat(tags("description")).isEmpty();
        assertThat(tags("events").get(0).getAttribute("data-variant")).isNull();
        p.events.setObject(null); tester.clickLink("refresh"); assertThat(tags("events")).isEmpty();
        p.events.setObject(new ArrayList<>(List.of(new Event("Restored", null, "New detail", TimelineEventBehavior.Variant.OUTLINE))));
        tester.clickLink("refresh"); assertThat(values("title")).containsExactly("Restored");
    }
    @Test void eventsAndExtractorsAreReadOnceAndModelsDetach() {
        int[] count = {0, 0, 0, 0};
        IModel<List<String>> events = new IModel<>() {
            @Override public List<String> getObject() { count[0]++; return List.of("One", "Two"); }
            @Override public void detach() { count[1]++; }
        };
        var timeline = new SwTimeline<>("timeline", events, event -> { count[2]++; return event; },
                event -> { count[3]++; return LocalDate.of(2026, 11, 8); });
        tester.startComponentInPage(timeline); assertThat(count).containsExactly(1, 1, 2, 2);
        tester.startPage(tester.getLastRenderedPage()); assertThat(count).containsExactly(2, 2, 4, 4);
        timeline.setVisible(false); tester.startPage(tester.getLastRenderedPage());
        assertThat(count).containsExactly(2, 3, 4, 4);
    }
    @Test void nullTimeExtractorAndNullTitlesAreSupportedWithoutLiteralNullText() {
        tester.startComponentInPage(new SwTimeline<>("timeline", Model.ofList(List.of("One")), event -> null, null));
        assertThat(values("title")).containsExactly(""); assertThat(tags("time")).isEmpty();
        assertThat(tester.getLastResponseAsString()).doesNotContain(">null<");
    }
    static Stream<Arguments> timestamps() {
        return Stream.of(
                Arguments.of(LocalDate.of(2026, 11, 8), "2026-11-08"),
                Arguments.of(LocalTime.of(9, 30, 1, 123456789), "09:30:01.123"),
                Arguments.of(LocalDateTime.of(2026, 11, 8, 9, 30, 1, 123456789), "2026-11-08T09:30:01.123"),
                Arguments.of(Instant.parse("2026-11-08T09:30:01.123456789Z"), "2026-11-08T09:30:01.123Z"),
                Arguments.of(ZonedDateTime.of(2026, 11, 8, 9, 30, 0, 0, ZoneId.of("Europe/Berlin")), "2026-11-08T09:30+01:00"),
                Arguments.of(YearMonth.of(2026, 11), "2026-11"), Arguments.of(MonthDay.of(2,29), "02-29"));
    }
    @ParameterizedTest @MethodSource("timestamps")
    void htmlTimesUseSupportedIsoSyntaxAndMillisecondPrecision(TemporalAccessor when, String expected) {
        tester.startComponentInPage(new SwTimeline<>("timeline", Model.ofList(List.of("Event")), event -> event, event -> when));
        assertThat(tester.getTagByWicketId("time").getAttribute("datetime")).isEqualTo(expected);
        assertThat(tester.getTagByWicketId("time").getName()).isEqualTo("time");
    }
    static Stream<TemporalAccessor> unsupportedTimes() {
        return Stream.of(Year.of(2026), LocalDate.of(0,1,1), Instant.MAX,
                OffsetTime.of(9,30,0,0, ZoneOffset.ofHours(1)), OffsetDateTime.of(1900,1,1,9,30,0,0,ZoneOffset.ofTotalSeconds(3601)));
    }
    @ParameterizedTest @MethodSource("unsupportedTimes")
    void unsupportedHtmlTimeValuesUseReadableSpansInsteadOfInvalidMetadata(TemporalAccessor when) {
        tester.startComponentInPage(new SwTimeline<>("timeline", Model.ofList(List.of("Event")), event -> event, event -> when));
        assertThat(tester.getTagByWicketId("time").getName()).isEqualTo("span");
        assertThat(tester.getTagByWicketId("time").getAttribute("datetime")).isNull();
    }
    @Test void explicitZoneChangesInstantsAndPreservesLocalWallTimesInBothComponents() {
        var details = new SwDescriptionList("details").setTimeZone(ZoneId.of("America/New_York"))
                .addItem("Instant", Model.of(Instant.parse("2026-11-08T00:30:00Z")))
                .addItem("Local", Model.of(LocalDateTime.of(2026,11,8,0,30)));
        tester.startComponentInPage(details, Markup.of("<dl wicket:id='details'></dl>"));
        assertThat(normalized(values("value").get(0))).isEqualTo("Nov 7, 2026, 7:30 PM");
        assertThat(normalized(values("value").get(1))).isEqualTo("Nov 8, 2026, 12:30 AM");
        tester.startComponentInPage(new SwTimeline<>("timeline", Model.ofList(List.of("Event")), e -> e,
                e -> Instant.parse("2026-11-08T00:30:00Z")).setTimeZone(ZoneId.of("America/New_York")));
        assertThat(normalized(values("time").get(0))).isEqualTo("Nov 7, 2026, 7:30 PM");
    }
    @Test void serializationRetainsPropertyFactoriesEventExtractorsAndAjaxCallbacks() {
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var details = tester.startPage(DetailsPage.class);
        var restored = (DetailsPage)serializer.deserialize(serializer.serialize(details)); tester.startPage(restored); tester.clickLink("refresh");
        assertThat(values("value")).contains("1,234.56789", "Active");
        var history = tester.startPage(HistoryPage.class);
        var restoredHistory = (HistoryPage)serializer.deserialize(serializer.serialize(history)); tester.startPage(restoredHistory);
        restoredHistory.events.setObject(new ArrayList<>(List.of(new Event("After restore", null, "Current", null))));
        tester.clickLink("refresh"); assertThat(values("title")).containsExactly("After restore");
    }
    @Test void callerOwnedBehaviorsKeepNativeStructureAndDetachMarkerModels() {
        int[] detached = {0}; var marker = new Model<>(TimelineEventBehavior.Variant.DESTRUCTIVE) {
            @Override public void detach() { detached[0]++; }
        };
        var list = new WebMarkupContainer("list"); list.add(Sw.Behaviors.timeline());
        list.add(new WebMarkupContainer("event").add(Sw.Behaviors.timelineEvent().setVariant(marker)));
        tester.startComponentInPage(list, Markup.of("<ol wicket:id='list'><li wicket:id='event'><strong>Failed</strong></li></ol>"));
        assertThat(tester.getTagByWicketId("event").getAttribute("data-variant")).isEqualTo("destructive");
        assertThat(detached[0]).isEqualTo(1);
    }

    record AccountCode(String value) implements Serializable {}
    @Test void applicationConvertersStillFormatOtherValueTypes() {
        ((org.apache.wicket.ConverterLocator)tester.getApplication().getConverterLocator()).set(AccountCode.class,
                new org.apache.wicket.util.convert.IConverter<AccountCode>() {
                    @Override public AccountCode convertToObject(String value, Locale locale) { return new AccountCode(value); }
                    @Override public String convertToString(AccountCode value, Locale locale) { return "Account " + value.value(); }
                });
        tester.startComponentInPage(new SwDescriptionList("details").addItem("Code", Model.of(new AccountCode("REF-42"))),
                Markup.of("<dl wicket:id='details'></dl>"));
        assertThat(values("value")).containsExactly("Account REF-42");
    }

    static class UnserializedTime implements TemporalAccessor {
        @Override public boolean isSupported(TemporalField field) { return false; }
        @Override public long getLong(TemporalField field) { throw new UnsupportedTemporalTypeException("Unknown field"); }
        @Override public String toString() { return "Unspecified <time>"; }
    }
    public static class UnserializedTimePage extends WebPage implements IMarkupResourceStreamProvider {
        public UnserializedTimePage() {
            add(new SwTimeline<>("timeline", Model.ofList(List.of("Note")), event -> event, event -> new UnserializedTime()));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='timeline'></div></body></html>");
        }
    }
    @Test void nonSerializableTemporalResultsAreNotRetainedInSerializedPages() {
        tester.startPage(UnserializedTimePage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (WebPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        assertThat(values("time")).containsExactly("Unspecified &lt;time&gt;");
        assertThat(tester.getTagByWicketId("time").getName()).isEqualTo("span");
    }
    @Test void defaultDisplayZoneIsIndependentOfTheServersTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            tester.startComponentInPage(new SwDescriptionList("details").addItem("Instant", Model.of(Instant.parse("2026-11-08T00:30:00Z"))),
                    Markup.of("<dl wicket:id='details'></dl>"));
            assertThat(normalized(values("value").get(0))).isEqualTo("Nov 8, 2026, 12:30 AM");
        } finally { TimeZone.setDefault(original); }
    }
}
