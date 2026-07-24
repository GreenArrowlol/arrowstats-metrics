package cc.arrowstats.metrics;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A nested map, group -> (sub-value -> count), e.g. Java "21" broken into "21.0.1",
 * "21.0.2". Server type: drilldown_pie.
 */
public class DrilldownPie extends CustomChart {

    private final Callable<Map<String, Map<String, Integer>>> callable;

    public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
        super(chartId);
        this.callable = callable;
    }

    public DrilldownPie(String chartId, String title, Callable<Map<String, Map<String, Integer>>> callable) {
        super(chartId, title);
        this.callable = callable;
    }

    @Override
    protected String getType() {
        return "drilldown_pie";
    }

    @Override
    protected boolean appendData(JsonObjectBuilder builder) throws Exception {
        Map<String, Map<String, Integer>> map = callable.call();
        if (map == null || map.isEmpty()) {
            return false;
        }
        JsonObjectBuilder groups = new JsonObjectBuilder();
        boolean anyGroup = false;
        for (Map.Entry<String, Map<String, Integer>> group : map.entrySet()) {
            if (group.getValue() == null || group.getValue().isEmpty()) {
                continue;
            }
            JsonObjectBuilder subs = new JsonObjectBuilder();
            boolean anySub = false;
            for (Map.Entry<String, Integer> sub : group.getValue().entrySet()) {
                int value = sub.getValue() == null ? 0 : sub.getValue();
                subs.appendField(sub.getKey(), value);
                anySub = true;
            }
            if (!anySub) {
                continue;
            }
            groups.appendField(group.getKey(), subs.build());
            anyGroup = true;
        }
        if (!anyGroup) {
            return false;
        }
        builder.appendField("data", groups.build());
        return true;
    }
}
