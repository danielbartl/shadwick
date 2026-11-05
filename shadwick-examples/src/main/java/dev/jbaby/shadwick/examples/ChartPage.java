package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.chart.*;
import java.io.Serializable;
import java.util.*;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** Server SVG and exact tables, enhanced with keyboard/hover tooltips. */
public class ChartPage extends BasePage {
    public record Month(String name, Double desktop, Double mobile) implements Serializable {}
    public static List<Month> months() {
        return List.of(new Month("January", 186.0, 80.0), new Month("February", 305.0, 200.0),
                new Month("March", 237.0, 120.0), new Month("April", 73.0, 190.0), new Month("May", 209.0, 130.0),
                new Month("June", 214.0, 140.0));
    }

    public ChartPage(PageParameters parameters) {
        super(parameters);
        add(chart("bars")
                        .setTitle("Visitors by device")
                        .addSeries(ChartSeries.bar("Desktop", Month::desktop))
                        .addSeries(ChartSeries.bar("Mobile", Month::mobile)));
        add(chart("stacked")
                        .setTitle("A shared picture")
                        .addSeries(ChartSeries.bar("Desktop", Month::desktop).setStack("visitors"))
                        .addSeries(ChartSeries.bar("Mobile", Month::mobile).setStack("visitors")));
        add(chart("lines")
                        .setTitle("Steady progress")
                        .addSeries(ChartSeries.line("Desktop", Month::desktop).setDots(true))
                        .addSeries(ChartSeries.line("Mobile", Month::mobile).setDashed(true)));
        add(chart("areas")
                        .setTitle("Room to grow")
                        .addSeries(ChartSeries.area("Desktop", Month::desktop).setStack("visitors"))
                        .addSeries(ChartSeries.area("Mobile", Month::mobile).setStack("visitors")));
        add(chart("composed")
                        .setTitle("Revenue and target")
                        .setCurrency(Currency.getInstance("EUR"))
                        .addSeries(ChartSeries.bar("Revenue", Month::desktop))
                        .addSeries(ChartSeries.line("Target", Month::mobile).setDashed(true)));
        IModel<List<Month>> changes = Model.ofList(months());
        var live = new SwChart<>("live", changes, Month::name)
                           .setTitle("Live report")
                           .addSeries(ChartSeries.area("Desktop", Month::desktop));
        add(live);
        add(SwButton.onClick("refresh", "Refresh report", target -> {
            changes.setObject(List.of(new Month("July", 250.0, 100.0), new Month("August", 320.0, 120.0),
                    new Month("September", 280.0, 110.0)));
            target.add(live);
        }));
        add(new SwChart<Month>("empty", Model.ofList(List.of()), Month::name)
                        .setTitle("Your next report")
                        .addSeries(ChartSeries.bar("Visitors", Month::desktop)));
        add(new SwChart<>("gaps",
                Model.ofList(List.of(new Month("January", 20.0, 10.0), new Month("February", null, -5.0),
                        new Month("March", -10.0, 15.0), new Month("April", 30.0, 25.0))),
                Month::name)
                        .setTitle("Honest gaps and negative values")
                        .addSeries(ChartSeries.line("Actual", Month::desktop).setCurve(ChartSeries.Curve.LINEAR))
                        .addSeries(ChartSeries.line("Previous", Month::mobile).setCurve(ChartSeries.Curve.STEP)));
    }

    private static SwChart<Month> chart(String id) {
        return new SwChart<>(id, Model.ofList(months()), Month::name).setCategoryHeader("Month");
    }

    @Override
    protected String getTitle() {
        return "Charts";
    }
}
