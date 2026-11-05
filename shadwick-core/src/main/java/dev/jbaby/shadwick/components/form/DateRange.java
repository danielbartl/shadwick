package dev.jbaby.shadwick.components.form;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** Inclusive date range. Null endpoints are open; fields reject reversed complete ranges. */
public record DateRange(LocalDate from, LocalDate to) implements Serializable {
    public boolean contains(LocalDate day) {
        Objects.requireNonNull(day,"day");
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }
}
