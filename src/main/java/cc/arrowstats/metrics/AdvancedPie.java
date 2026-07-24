package cc.arrowstats.metrics;

import java.util.Map;
import java.util.concurrent.Callable;

/** A map of label -> count per server, summed per label. Server type: advanced_pie. */
public class AdvancedPie extends CustomChart {

    private final Callable<Map<String, Integer>> callable;

    public AdvancedPie(String chartId, Callable<Map<String, Integer>> callable) {
        super(chartId);
        this.callable = callable;
    }

    public AdvancedPie(String chartId, String title, Callable<Map<String, Integer>> callable) {
        super(chartId, title);
        this.callable = callable;
    }

    @Override
    protected String getType() {
        return "advanced_pie";
    }

    @Override
    protected boolean appendData(JsonObjectBuilder builder) throws Exception {
        Map<String, Integer> map = callable.call();
        if (map == null || map.isEmpty()) {
            return false;
        }
        JsonObjectBuilder values = new JsonObjectBuilder();
        boolean any = false;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() == null || entry.getValue() == 0) {
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
