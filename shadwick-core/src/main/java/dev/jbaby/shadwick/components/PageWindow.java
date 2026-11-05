package dev.jbaby.shadwick.components;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/** Bounded page windows; -1 represents an ellipsis. Page indexes are zero-based. */
public final class PageWindow {
    private PageWindow() {}
    public static List<Long> pages(long current, long count, int siblings) {
        if (siblings < 0 || siblings > 10) throw new IllegalArgumentException("Use between 0 and 10 page siblings");
        long last = Math.max(1, count) - 1; current = Math.max(0, Math.min(current, last));
        SortedSet<Long> shown = new TreeSet<>(); shown.add(0L); shown.add(last);
        long from = Math.max(0, current - siblings), to = current + Math.min(siblings, last - current);
        for (long page = from;; page++) { shown.add(page); if (page == to) break; }
        var result = new ArrayList<Long>(); long previous = -1;
        for (long page : shown) {
            if (previous >= 0 && page - previous == 2) result.add(previous + 1);
            else if (previous >= 0 && page - previous > 2) result.add(-1L);
            result.add(page); previous = page;
        }
        return List.copyOf(result);
    }
}
