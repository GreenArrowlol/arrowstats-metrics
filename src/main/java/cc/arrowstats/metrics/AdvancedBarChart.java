package cc.arrowstats.metrics;

import java.util.Map;
import java.util.concurrent.Callable;

/** A map of label -> number series per server, summed element-wise. Type: advanced_bar. */
public class AdvancedBarChart extends CustomChart {

    private final Callable<Map<String, int[]>> callable;

    public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> callable) {
        super(chartId);
        this.callable = callable;
    }

    public AdvancedBarChart(String chartId, String title, Callable<Map<String, int[]>> callable) {
        super(chartId, title);
        this.callable = callable;
    }

    @Override
    protected String getType() {
        return "advanced_bar";
    }

    @Override
    protected boolean appendData(JsonObjectBuilder builder) throws Exception {
        Map<String, int[]> map = callable.call();
        if (map == null || map.isEmpty()) {
            return false;
        }
        JsonObjectBuilder values = new JsonObjectBuilder();
        boolean any = false;
        for (Map.Entry<String, int[]> entry : map.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            values.appendField(entry.getKey(), entry.getValue());
            any = true;
        }
        if (!any) {
            return false;
        }
        builder.appendField("data", values.build());
        return true;
    }
}
