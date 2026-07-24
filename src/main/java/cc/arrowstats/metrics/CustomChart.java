package cc.arrowstats.metrics;

import java.util.function.BiConsumer;

/**
 * Base class for the author-defined custom charts. Each subclass wraps a callback the
 * plugin supplies; the callback is invoked once per submission. Any exception from the
 * callback is swallowed and the chart is simply skipped that round — a broken chart
 * must never break the submission or the host plugin.
 *
 * A chart carries its type and (optionally) a title. When the same chart is also
 * defined on the ArrowStats site, the server uses its stored type; the inline type/
 * title here act as the auto-registration fallback for code-first charts.
 */
public abstract class CustomChart {

    private final String chartId;
    private final String title;

    protected CustomChart(String chartId) {
        this(chartId, null);
    }

    protected CustomChart(String chartId, String title) {
        if (chartId == null || chartId.isEmpty()) {
            throw new IllegalArgumentException("chartId must not be null or empty");
        }
        this.chartId = chartId;
        this.title = title;
    }

    String getChartId() {
        return chartId;
    }

    /**
     * Build the {type, title?, data} entry for this chart, or null to skip it this
     * round (no data, or the callback failed).
     */
    JsonObjectBuilder.JsonObject getRequestJsonObject(BiConsumer<String, Throwable> errorLogger) {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        builder.appendField("type", getType());
        if (title != null) {
            builder.appendField("title", title);
        }
        try {
            if (!appendData(builder)) {
                return null;
            }
        } catch (Throwable t) {
            errorLogger.accept("Failed to get data for custom chart with id " + chartId, t);
            return null;
        }
        return builder.build();
    }

    /** The server-side chart type string (see the ArrowStats ChartType constants). */
    protected abstract String getType();

    /**
     * Append the "data" field to {@code builder} from the chart's callback.
     * Return false to skip the chart (e.g. the callback returned null/empty).
     */
    protected abstract boolean appendData(JsonObjectBuilder builder) throws Exception;
}
