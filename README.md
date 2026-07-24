# ArrowStats Metrics — client integration

A tiny (~21 KB), dependency-free metrics client you shade into your Minecraft plugin.
It anonymously reports server/player counts and environment info to ArrowStats
(https://stats.arrowtan.cc) every 30 minutes, off the main thread, and never throws
into your plugin.

Supports **Bukkit / Spigot / Paper** today. BungeeCord and Velocity slot in later via
the platform-neutral `MetricsBase` core (no rewrite needed).

> **Licensed under the MIT License** — free to use in any plugin, including closed-source
> and paid ones. Just keep the license notice.

---

## 1. Get your plugin ID

Every plugin is identified by a **numeric ID** (created in the ArrowStats admin area).
The demo plugin is ID `1`. You pass this ID to the client — never a name.

## 2. Add the dependency (via JitPack)

**Maven**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.GreenArrowlol</groupId>
    <artifactId>arrowstats-metrics</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle**
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.GreenArrowlol:arrowstats-metrics:1.0.0'
}
```

> Prefer no build-tool dependency? Copy the `cc/arrowstats/metrics/*.java` files
> straight into your project under **your own package** and skip to step 4 — no
> relocation needed because they already live in your namespace.

## 3. Relocate the package (required when using the dependency)

Because several plugins may each ship this client, the classes **must be relocated**
into your own package so they don't clash on the classpath. (The package to relocate is
always `cc.arrowstats.metrics`, regardless of the JitPack groupId.)

**Maven Shade**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>cc.arrowstats.metrics</pattern>
                        <!-- change to YOUR plugin's package -->
                        <shadedPattern>com.example.myplugin.metrics</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Gradle Shadow**
```groovy
plugins {
    id 'com.gradleup.shadow' version '8.3.0'
}

shadowJar {
    // change to YOUR plugin's package
    relocate 'cc.arrowstats.metrics', 'com.example.myplugin.metrics'
}
```

## 4. Two lines in `onEnable()`

```java
@Override
public void onEnable() {
    int pluginId = 1; // your ArrowStats plugin ID
    Metrics metrics = new Metrics(this, pluginId);
}
```

Submissions go to **https://stats.arrowtan.cc** by default. To point somewhere else
(local development, or your own ArrowStats deployment), pass a URL:

```java
Metrics metrics = new Metrics(this, pluginId, "http://localhost:8020/api/v1/ingest");
```

## 5. Custom charts (optional)

Add these after constructing `Metrics`. Each callback runs once per submission; if it
throws, that chart is skipped — your plugin is never affected.

```java
metrics.addCustomChart(new SimplePie("server_type",
        () -> getConfig().getString("mode", "Survival")));

metrics.addCustomChart(new SingleLineChart("crops_harvested",
        () -> harvestedSinceLastSubmit.getAndSet(0)));

metrics.addCustomChart(new AdvancedPie("gamemodes", () -> {
    Map<String, Integer> map = new HashMap<>();
    map.put("Survival", survivalPlayers());
    map.put("Creative", creativePlayers());
    return map;
}));
```

Available types: `SimplePie`, `AdvancedPie`, `DrilldownPie`, `SingleLineChart`,
`SimpleBarChart`, `AdvancedBarChart`. The chart ID is the key the server aggregates
under; an optional second constructor argument sets a title. If you define the chart on
the ArrowStats site first, the server owns its type and you only need to send data.

## What gets collected

Server & player count, Minecraft version, server software, online mode, plugin
version, Java version, OS, architecture, CPU core count — plus your custom charts.
**Server location is derived server-side from the request IP; it is never sent by the
client.**

## Opting out

A shared file, `plugins/ArrowStats/config.yml`, is created on first run with an
anonymous server UUID and an `enabled` flag. Setting `enabled: false` opts the whole
server out of **every** ArrowStats-using plugin. It also carries `logSentData` and
`logResponseStatusText` toggles for debugging.

## License

MIT — see [LICENSE](LICENSE).
