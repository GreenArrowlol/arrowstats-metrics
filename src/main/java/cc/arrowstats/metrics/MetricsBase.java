package cc.arrowstats.metrics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Platform-neutral core of the ArrowStats client. It owns the schedule, assembles the
 * JSON payload from platform + service appenders and the custom charts, and performs
 * the HTTP submit — all on a private daemon thread, so the host server's main thread
 * is never blocked by network I/O.
 *
 * This class knows nothing about Bukkit. A BungeeCord or Velocity client is added by
 * writing a thin platform entry (like {@link Metrics}) that constructs a MetricsBase
 * with its own config, "enabled" check, and data appenders. That is the intended
 * multi-platform seam.
 */
public class MetricsBase {

    public static final String METRICS_VERSION = "1.0.1";

    private static final long SUBMIT_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);

    private final String platform;
    private final String serverUuid;
    private final int serviceId;
    private final String submitUrl;
    private final boolean enabled;

    private final Consumer<JsonObjectBuilder> appendPlatformDataConsumer;
    private final Consumer<JsonObjectBuilder> appendServiceDataConsumer;
    private final Supplier<Boolean> checkServiceEnabledSupplier;
    private final BiConsumer<String, Throwable> errorLogger;
    private final Consumer<String> infoLogger;
    private final boolean logErrors;
    private final boolean logSentData;
    private final boolean logResponseStatusText;

    private final Set<CustomChart> customCharts = new LinkedHashSet<>();
    private final ScheduledExecutorService scheduler;

    public MetricsBase(
            String platform,
            String serverUuid,
            int serviceId,
            boolean enabled,
            String submitUrl,
            Consumer<JsonObjectBuilder> appendPlatformDataConsumer,
            Consumer<JsonObjectBuilder> appendServiceDataConsumer,
            Supplier<Boolean> checkServiceEnabledSupplier,
            BiConsumer<String, Throwable> errorLogger,
            Consumer<String> infoLogger,
            boolean logErrors,
            boolean logSentData,
            boolean logResponseStatusText) {
        this.platform = platform;
        this.serverUuid = serverUuid;
        this.serviceId = serviceId;
        this.enabled = enabled;
        this.submitUrl = submitUrl;
        this.appendPlatformDataConsumer = appendPlatformDataConsumer;
        this.appendServiceDataConsumer = appendServiceDataConsumer;
        this.checkServiceEnabledSupplier = checkServiceEnabledSupplier;
        this.errorLogger = errorLogger;
        this.infoLogger = infoLogger;
        this.logErrors = logErrors;
        this.logSentData = logSentData;
        this.logResponseStatusText = logResponseStatusText;

        this.scheduler = Executors.newScheduledThreadPool(1, task -> {
            Thread thread = new Thread(task, "arrowstats-metrics");
            thread.setDaemon(true);
            return thread;
        });

        if (enabled) {
            startSubmitting();
        }
    }

    public void addCustomChart(CustomChart chart) {
        customCharts.add(chart);
    }

    /** Stop the scheduler (e.g. from the plugin's onDisable). Safe to call more than once. */
    public void shutdown() {
        scheduler.shutdown();
    }

    private void startSubmitting() {
        Runnable task = () -> {
            if (!enabled || !checkServiceEnabledSupplier.get()) {
                scheduler.shutdown(); // plugin disabled or opted out: stop cleanly
                return;
            }
            try {
                submitData();
            } catch (Throwable t) {
                logError("Could not submit metrics data", t);
            }
        };

        // Randomised initial delay (3-6 min) so a fleet of servers does not all submit at once,
        // then every 30 minutes.
        long initialDelay = (long) (TimeUnit.MINUTES.toMillis(3) + Math.random() * TimeUnit.MINUTES.toMillis(3));
        scheduler.scheduleAtFixedRate(task, initialDelay, SUBMIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // Package-visible so an in-package harness can trigger one submit without waiting
    // for the 30-minute schedule. Production callers use the timer set up in the ctor.
    void submitData() {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        builder.appendField("pluginId", serviceId);
        builder.appendField("serverUUID", serverUuid);
        builder.appendField("platform", platform);

        try {
            appendPlatformDataConsumer.accept(builder);
        } catch (Throwable t) {
            logError("Failed to append platform data", t);
        }
        try {
            appendServiceDataConsumer.accept(builder);
        } catch (Throwable t) {
            logError("Failed to append service data", t);
        }

        JsonObjectBuilder chartsBuilder = new JsonObjectBuilder();
        for (CustomChart chart : customCharts) {
            JsonObjectBuilder.JsonObject entry = chart.getRequestJsonObject(errorLogger);
            if (entry != null) {
                chartsBuilder.appendField(chart.getChartId(), entry);
            }
        }
        builder.appendField("customCharts", chartsBuilder.build());

        JsonObjectBuilder.JsonObject data = builder.build();
        if (logSentData) {
            infoLogger.accept("Sending data to ArrowStats: " + data);
        }
        try {
            sendData(data);
        } catch (Throwable t) {
            logError("Could not send metrics data to ArrowStats", t);
        }
    }

    private void sendData(JsonObjectBuilder.JsonObject data) throws Exception {
        byte[] body = data.toString().getBytes(StandardCharsets.UTF_8);

        HttpURLConnection connection = (HttpURLConnection) new URL(submitUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("User-Agent", "ArrowStats-Metrics/" + METRICS_VERSION);

        try (OutputStream out = connection.getOutputStream()) {
            out.write(body);
        }

        int status = connection.getResponseCode();
        if (logResponseStatusText) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (Exception ignored) {
                // response body is best-effort
            }
            infoLogger.accept("ArrowStats responded (" + status + "): " + response);
        }
    }

    private void logError(String message, Throwable error) {
        if (logErrors) {
            errorLogger.accept(message, error);
        }
    }
}
