package cc.arrowstats.metrics;

import java.util.Map;
import java.util.concurrent.Callable;

/** A map of label -> count per server, summed per label. Server type: simple_bar. */
public class SimpleBarChart extends CustomChart {

    private final Callable<Map<String, Integer>> callable;

    public SimpleBarChart(String chartId, Callable<Map<String, Integer>> callable) {
        super(chartId);
        this.callable = callable;
    }

    public SimpleBarChart(String chartId, String title, Callable<Map<String, Integer>> callable) {
        super(chartId, title);
        this.callable = callable;
    }

    @Override
    protected String getType() {
        return "simple_bar";
    }

    @Override
    protected boolean appendData(JsonObjectBuilder builder) throws Exception {
        Map<String, Integer> map = callable.call();
        if (map == null || map.isEmpty()) {
            return false;
        }
        JsonObjectBuilder values = new JsonObjectBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            int value = entry.getValue() == null ? 0 : entry.getValue();
            values.appendField(entry.getKey(), value);
        }
        builder.appendField("data", values.build());
        return true;
    }
}
