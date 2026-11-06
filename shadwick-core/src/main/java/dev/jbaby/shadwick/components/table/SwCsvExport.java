package dev.jbaby.shadwick.components.table;

import org.apache.wicket.Application;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.Locale;

/**
 * Writes a data table's rows as CSV (RFC 4180, UTF-8 with a byte order mark so
 * spreadsheet apps detect the encoding), for {@code SwDataTable.setCsvExport}.
 * <ul>
 * <li>The columns are those with a header text and a value ({@link IExportableColumn}).</li>
 * <li>Values stay machine-readable: numbers as plain digits with a {@code .} decimal
 * point, dates and times in ISO 8601, booleans as {@code true}/{@code false}; anything
 * else through the application's converter for its type.</li>
 * <li>A text starting with {@code = + - @}, a tab or a carriage return is prefixed with
 * {@code '}, so a spreadsheet doesn't run it as a formula (CSV injection).</li>
 * </ul>
 */
public final class SwCsvExport {

    /** Rows are fetched from the provider this many at a time. */
    private static final int BATCH = 500;

    private SwCsvExport() {
    }

    /** Sends the rows as a CSV download, as the response to the current request. */
    public static <T> void download(RequestCycle cycle, IDataProvider<T> provider, List<? extends IColumn<T, ?>> columns,
                                    String fileName, Locale locale) {
        validateFileName(fileName);
        AbstractResourceStreamWriter stream = new AbstractResourceStreamWriter() {
            @Override
            public void write(OutputStream output) throws IOException {
                SwCsvExport.write(provider, columns, locale, output);
            }

            @Override
            public String getContentType() {
                return "text/csv; charset=UTF-8";
            }
        };
        cycle.scheduleRequestHandlerAfterCurrent(new ResourceStreamRequestHandler(stream, fileName)
                .setContentDisposition(ContentDisposition.ATTACHMENT).setCacheDuration(java.time.Duration.ZERO));
    }

    /** Writes the header line and every row. */
    @SuppressWarnings("unchecked")
    public static <T> void write(IDataProvider<T> provider, List<? extends IColumn<T, ?>> columns, Locale locale,
                                 OutputStream output) throws IOException {
        Objects.requireNonNull(provider); Objects.requireNonNull(columns); Objects.requireNonNull(locale); Objects.requireNonNull(output);
        try {
            List<IExportableColumn<T, ?>> exported = columns.stream()
                    .filter(IExportableColumn.class::isInstance)
                    .<IExportableColumn<T, ?>>map(column -> (IExportableColumn<T, ?>) column)
                    .filter(column -> column.getDisplayModel() != null && column.getDisplayModel().getObject() != null
                            && !column.getDisplayModel().getObject().isBlank()).toList();
            if (exported.isEmpty()) throw new IllegalArgumentException("CSV export needs a named exportable column");
            Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writeLine(writer, exported.stream().map(column -> text(column.getDisplayModel().getObject())).toList());
            long size = provider.size();
            for (long first = 0; first < size;) {
                long count = Math.min(BATCH, size - first);
                Iterator<? extends T> rows = provider.iterator(first, count);
                long fetched = 0;
                while (fetched < count && rows.hasNext()) {
                    IModel<T> row = provider.model(rows.next());
                    try {
                        List<String> cells = new ArrayList<>(exported.size());
                        for (IExportableColumn<T, ?> column : exported) {
                            IModel<?> cell = column.getDataModel(row);
                            try { cells.add(value(cell == null ? null : cell.getObject(), locale)); }
                            finally { if (cell != null) cell.detach(); }
                        }
                        writeLine(writer, cells);
                    } finally { row.detach(); }
                    fetched++;
                }
                // A changing provider may shrink while a download is in progress.
                if (fetched < count) break;
                first += count;
            }
            writer.flush(); // The response owns the stream; never close it here.
        } finally { provider.detach(); }
    }

    /** Reject path and header-control characters; the response supplies attachment disposition. */
    public static void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.equals(".") || fileName.equals("..")
                || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0
                || fileName.chars().anyMatch(c -> Character.isISOControl(c)))
            throw new IllegalArgumentException("CSV filename must be a nonblank filename without paths or control characters");
    }

    private static void writeLine(Writer writer, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) writer.write(',');
            writer.write(quote(cells.get(i)));
        }
        writer.write("\r\n");
    }

    /** A value as CSV text; numbers and dates stay machine-readable. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static String value(Object value, Locale locale) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof TemporalAccessor) {
            return value.toString();
        }
        if (value instanceof CharSequence || value instanceof Enum<?>) {
            return text(value.toString());
        }
        IConverter converter = Application.get().getConverterLocator().getConverter(value.getClass());
        return text(converter != null ? converter.convertToString(value, locale) : value.toString());
    }

    /** Text, defused if a spreadsheet would read it as a formula. */
    static String text(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Spreadsheet importers may ignore leading spaces/controls before a formula.
        int index = 0;
        while (index < text.length() && (Character.isWhitespace(text.charAt(index))
                || Character.isISOControl(text.charAt(index)) || text.charAt(index) == '\ufeff')) index++;
        char first = index < text.length() ? text.charAt(index) : 0;
        return first == '=' || first == '+' || first == '-' || first == '@'
                || text.charAt(0) == '\t' || text.charAt(0) == '\r' || text.charAt(0) == '\n' ? "'" + text : text;
    }

    /** Quotes a cell when it holds a comma, a quote or a line break. */
    static String quote(String cell) {
        if (cell.indexOf(',') >= 0 || cell.indexOf('"') >= 0 || cell.indexOf('\n') >= 0 || cell.indexOf('\r') >= 0) {
            return '"' + cell.replace("\"", "\"\"") + '"';
        }
        return cell;
    }
}
