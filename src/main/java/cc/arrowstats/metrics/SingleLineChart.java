package cc.arrowstats.metrics;

import java.util.concurrent.Callable;

/** One number per server, summed across servers over time. Server type: single_line. */
public class SingleLineChart extends CustomChart {

    private final Callable<Integer> callable;

    public SingleLineChart(String chartId, Callable<Integer> callable) {
        super(chartId);
        this.callable = callable;
    }

    public SingleLineChart(String chartId, String title, Callable<Integer> callable) {
        super(chartId, title);
        this.callable = callable;
    }

    @Override
    protected String getType() {
        return "single_line";
    }

    @Override
    protected boolean appendData(JsonObjectBuilder builder) throws Exception {
        Integer value = callable.call();
        if (value == null) {
            return false;
        }
        builder.appendField("data", value);
        return true;
    }
}
