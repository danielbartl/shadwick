package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.AbstractConverter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Locale-independent converters for native HTML date/time controls, following
 * Wicket Oat's Html5Converters. Syntax and calendar validity are checked on the
 * server, including for month/week fields whose models remain strings.
 */
public final class Html5Converters {
    private static final String YEAR = "[0-9]{4,9}";
    private static final String DATE = YEAR + "-[0-9]{2}-[0-9]{2}";
    private static final String TIME = "[0-9]{2}:[0-9]{2}(:[0-9]{2}(\\.[0-9]{1,3})?)?";
    private static final DateTimeFormatter MONTH_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4, 9, SignStyle.NOT_NEGATIVE)
            .appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .append(MONTH_FORMAT).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
            .append(DATE_FORMAT).appendLiteral('T').append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    private Html5Converters() {}

    public static IConverter<LocalDate> date() {
        return new HtmlConverter<>(LocalDate.class, DATE, "YYYY-MM-DD", value -> {
            LocalDate date = LocalDate.parse(value, DATE_FORMAT);
            requirePositiveYear(date.getYear());
            return date;
        }, Html5Converters::formatDate);
    }

    public static IConverter<LocalTime> time() {
        return new HtmlConverter<>(LocalTime.class, TIME, "HH:mm[:ss[.SSS]]",
                LocalTime::parse, Html5Converters::formatTime);
    }

    /** Local wall-clock time, with no offset or time zone conversion. */
    public static IConverter<LocalDateTime> dateTimeLocal() {
        return new HtmlConverter<>(LocalDateTime.class, DATE + "[T ]" + TIME,
                "YYYY-MM-DDTHH:mm[:ss[.SSS]]", value -> {
            LocalDateTime dateTime = LocalDateTime.parse(value.replace(' ', 'T'), DATE_TIME_FORMAT);
            requirePositiveYear(dateTime.getYear());
            return dateTime;
        }, value -> formatDate(value.toLocalDate()) + "T" + formatTime(value.toLocalTime()));
    }

    public static IConverter<String> month() {
        return new HtmlConverter<>(String.class, YEAR + "-[0-9]{2}", "YYYY-MM", value -> {
            requirePositiveYear(YearMonth.parse(value, MONTH_FORMAT).getYear());
            return value;
        }, value -> value);
    }

    /** ISO week-year and week number; rejects week 53 in a 52-week year. */
    public static IConverter<String> week() {
        return new HtmlConverter<>(String.class, YEAR + "-W[0-9]{2}", "YYYY-Www", value -> {
            int separator = value.indexOf('-');
            int year = Integer.parseInt(value.substring(0, separator));
            requirePositiveYear(year);
            int week = Integer.parseInt(value.substring(separator + 2));
            int lastWeek = LocalDate.of(year, 12, 28).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            if (week < 1 || week > lastWeek) throw new DateTimeException("Invalid ISO week");
            return value;
        }, value -> value);
    }

    private static void requirePositiveYear(int year) {
        if (year <= 0) throw new DateTimeException("HTML dates require a positive year");
    }

    private static String formatDate(LocalDate value) {
        requirePositiveYear(value.getYear());
        return DATE_FORMAT.format(value);
    }

    private static String formatTime(LocalTime value) {
        // Omit zero seconds; native HTML inputs support at most millisecond precision.
        return value.truncatedTo(ChronoUnit.MILLIS).toString();
    }

    private static final class HtmlConverter<T> extends AbstractConverter<T> {
        private final Class<T> type;
        private final Pattern syntax;
        private final String format;
        private final SerializableFunction<String, T> parser;
        private final SerializableFunction<T, String> printer;

        private HtmlConverter(Class<T> type, String syntax, String format,
                              SerializableFunction<String, T> parser, SerializableFunction<T, String> printer) {
            this.type = type;
            this.syntax = Pattern.compile(syntax);
            this.format = format;
            this.parser = parser;
            this.printer = printer;
        }

        @Override
        protected Class<T> getTargetType() { return type; }

        @Override
        public T convertToObject(String value, Locale locale) {
            if (value == null || value.isEmpty()) return null;
            try {
                if (!syntax.matcher(value).matches()) throw new DateTimeException("Invalid HTML date/time syntax");
                return parser.apply(value);
            } catch (DateTimeException | NumberFormatException ex) {
                throw newConversionException("Invalid HTML date/time value", value, locale)
                        .setResourceKey("Html5Converters.invalid").setVariable("format", format);
            }
        }

        @Override
        public String convertToString(T value, Locale locale) {
            return value == null ? null : printer.apply(value);
        }
    }
}
