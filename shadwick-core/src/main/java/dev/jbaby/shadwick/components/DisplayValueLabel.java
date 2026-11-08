package dev.jbaby.shadwick.components;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/** Oat-style display conversions without changing the consuming application's converter registry. */
class DisplayValueLabel extends Label {
    private final IModel<ZoneId> zone;
    DisplayValueLabel(String id, IModel<?> value, IModel<ZoneId> zone) { super(id, value); this.zone = zone; }

    @Override protected IConverter<?> createConverter(Class<?> type) {
        if (!Number.class.isAssignableFrom(type) && !DisplayValues.isTemporal(type)) return super.createConverter(type);
        return new IConverter<Object>() {
            @Override public Object convertToObject(String value, Locale locale) { throw new UnsupportedOperationException("Display-only value"); }
            @Override public String convertToString(Object value, Locale locale) {
                if (value == null) return null;
                return value instanceof Number number ? DisplayValues.number(number, locale)
                        : DisplayValues.temporal((TemporalAccessor) value, locale, zone.getObject());
            }
        };
    }
}
