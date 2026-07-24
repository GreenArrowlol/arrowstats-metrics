package cc.arrowstats.metrics;

import java.util.concurrent.Callable;

/** One string value per server (e.g. "language" -> "en"). Server type: simple_pie. */
public class SimplePie extends CustomChart {

    private final Callable<String> callable;

    public SimplePie(String chartId, Callable<String> callable) {
        super(chartId);
        this.callable = callable;
    }

    public SimplePie(String chartId, String title, Callable<String> callable) {
        super(chartId, title);
        this.callable = callable;
    }

    @Override
    protected String getType() {
        return "simple_pie";
    }

    @Override
    protected boolean appendData(JsonObjectBuilder builder) throws Exception {
        String value = callable.call();
        if (value == null || value.isEmpty()) {
            return false;
        }
        builder.appendField("data", value);
        return true;
    }
}
