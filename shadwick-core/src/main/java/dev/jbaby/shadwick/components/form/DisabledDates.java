package dev.jbaby.shadwick.components.form;

import java.io.Serializable;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Strict shared date/interval/weekday rules, applied to both native submission and the grid. */
public final class DisabledDates implements Serializable {
    private final Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
    private final List<DateRange> ranges = new ArrayList<>();
    public DisabledDates(String specification) {
        for (String raw : Objects.requireNonNullElse(specification, "").split(",")) {
            String token=raw.strip().toLowerCase(Locale.ROOT);
            if(token.isEmpty()) continue;
            DayOfWeek weekday=Arrays.stream(DayOfWeek.values()).filter(d->d.name().toLowerCase(Locale.ROOT).equals(token)
                    || d.name().substring(0,3).toLowerCase(Locale.ROOT).equals(token)).findFirst().orElse(null);
            if(weekday!=null) { weekdays.add(weekday); continue; }
            try {
                String[] parts=token.split("\\.\\.",-1);
                if(parts.length>2) throw new IllegalArgumentException();
                LocalDate from=LocalDate.parse(parts[0]), to=parts.length==1?from:LocalDate.parse(parts[1]);
                if(from.getYear()<1||to.getYear()>9999||to.isBefore(from)) throw new IllegalArgumentException();
                ranges.add(new DateRange(from,to));
            } catch(RuntimeException error) { throw new IllegalArgumentException("Invalid disabled date rule: "+raw,error); }
        }
    }
    public boolean contains(LocalDate date) {
        return weekdays.contains(date.getDayOfWeek()) || ranges.stream().anyMatch(range->range.contains(date));
    }
    public boolean intersects(DateRange range) {
        if(range==null) return false;
        if(range.from()==null) return range.to()!=null&&contains(range.to());
        if(range.to()==null) return contains(range.from());
        if(ranges.stream().anyMatch(blocked->!range.to().isBefore(blocked.from())&&!range.from().isAfter(blocked.to()))) return true;
        if(weekdays.isEmpty()) return false;
        long days=ChronoUnit.DAYS.between(range.from(),range.to());
        for(int i=0;i<=Math.min(6,days);i++) if(weekdays.contains(range.from().plusDays(i).getDayOfWeek())) return true;
        return false;
    }
}
