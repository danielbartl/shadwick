package dev.jbaby.shadwick.components;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/** Shared presentation for native details and histories; not an application conversion API. */
final class DisplayValues {
    private DisplayValues() {}

    static String number(Number value, Locale locale) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        // Read-only details must not silently round stored decimal values to the formatter's usual three places.
        format.setMaximumFractionDigits(value instanceof BigDecimal decimal ? Math.max(0, decimal.scale()) : 340);
        return format.format(value);
    }

    static boolean isTemporal(Class<?> type) {
        return type == LocalDate.class || type == LocalTime.class || type == LocalDateTime.class
                || type == Instant.class || type == OffsetTime.class || type == OffsetDateTime.class
                || type == ZonedDateTime.class || type == YearMonth.class || type == MonthDay.class;
    }

    static String temporal(TemporalAccessor value, Locale locale, ZoneId zone) {
        try { return formattedTemporal(value, locale, zone); }
        catch (DateTimeException outsideSupportedCalendar) { return value.toString(); }
    }

    private static String formattedTemporal(TemporalAccessor value, Locale locale, ZoneId zone) {
        if (value instanceof Instant instant) value = instant.atZone(zone);
        else if (value instanceof OffsetDateTime offset) value = offset.atZoneSameInstant(zone);
        else if (value instanceof ZonedDateTime zoned) value = zoned.withZoneSameInstant(zone);
        DateTimeFormatter format;
        if (value instanceof LocalDate) format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        else if (value instanceof LocalTime || value instanceof OffsetTime) format = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        else if (value instanceof LocalDateTime || value instanceof ZonedDateTime)
            format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);
        else if (value instanceof YearMonth) format = DateTimeFormatter.ofPattern("LLLL uuuu");
        else if (value instanceof MonthDay) format = DateTimeFormatter.ofPattern("MMMM d");
        else return value.toString();
        return format.withLocale(locale).format(value);
    }

    static String datetime(TemporalAccessor value) {
        if (value instanceof LocalDate date) return htmlYear(date.getYear()) ? date.toString() : null;
        if (value instanceof YearMonth month) return htmlYear(month.getYear()) ? month.toString() : null;
        if (value instanceof MonthDay day) return day.toString().substring(2);
        if (value instanceof LocalTime time) return time.truncatedTo(ChronoUnit.MILLIS).toString();
        if (value instanceof LocalDateTime time) return htmlYear(time.getYear()) ? time.truncatedTo(ChronoUnit.MILLIS).toString() : null;
        if (value instanceof ZonedDateTime time) return datetime(time.toOffsetDateTime());
        if (value instanceof OffsetDateTime time) {
            // HTML offsets have minute precision; a region's historical second-level offset has no matching syntax.
            if (!htmlYear(time.getYear()) || time.getOffset().getTotalSeconds() % 60 != 0) return null;
            return time.truncatedTo(ChronoUnit.MILLIS).toString();
        }
        if (value instanceof Instant instant) {
            try { return datetime(instant.atOffset(ZoneOffset.UTC)); }
            catch (DateTimeException outsideHtmlRange) { return null; }
        }
        return null;
    }
    private static boolean htmlYear(int year) { return year >= 1 && year <= 9999; }
}
