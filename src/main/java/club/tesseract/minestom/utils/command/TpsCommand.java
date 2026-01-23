package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.command.builder.Command;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.SharedInstance;
import net.minestom.server.monitoring.TickMonitor;
import net.minestom.server.network.ConnectionManager;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Math.floor;

@CommandMetadata(
        categories = {CommandCategory.GENERIC, CommandCategory.SERVER},
        description = "Displays server statistics"
)
public class TpsCommand extends Command {

    public static final AtomicReference<TickMonitor> LAST_TICK = new AtomicReference<>();
    private final List<Pattern> EXCLUDED_MEMORY_SPACES = Stream.of("Metaspace", "Compressed Class Space", "^CodeHeap").map(Pattern::compile).toList();


    public TpsCommand() {
        super("tps", "tickrate");

        setCondition(ExtraConditions.hasPermission("gamesdk.command.tps"));

        MinecraftServer.getGlobalEventHandler().addListener(ServerTickMonitorEvent.class, event ->{
            LAST_TICK.set(event.getTickMonitor());
        });
        ConnectionManager connectionManager = MinecraftServer.getConnectionManager();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        setDefaultExecutor((sender, context) -> {
            long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            long ramUsage = totalMem - freeMem;

            double cpuPercent = getCpuLoadPercent();

            TickMonitor monitor = LAST_TICK.get();
            double tickMs = monitor.getTickTime();
            int tpsSetting = ServerFlag.SERVER_TICKS_PER_SECOND;
            int maxTickMs = MinecraftServer.TICK_MS;

            int tps = (int) floor(1000 / tickMs);
            if (tps > tpsSetting) tps = tpsSetting;

            int onlinePlayers = connectionManager.getOnlinePlayers().size();
            int entities = instanceManager.getInstances().stream().mapToInt(i -> i.getEntities().size()).sum() - onlinePlayers;

            var instances = instanceManager.getInstances();
            long sharedInstances = instances.stream().filter(i -> i instanceof SharedInstance).count();
            long chunks = instances.stream().mapToLong(i -> i.getChunks().size()).sum();

            Component message = Component.textOfChildren(
                    // RAM
                    Component.text("RAM: ", NamedTextColor.GRAY),
                    Component.text(ramUsage + "MB / " + totalMem + "MB", NamedTextColor.GRAY),
                    Component.text(" (", NamedTextColor.GRAY),
                    Component.text(((int) floor((ramUsage * 100.0) / totalMem)) + "%", NamedTextColor.GREEN),
                    Component.text(")", NamedTextColor.GRAY),

                    // CPU
                    Component.text("\nCPU: ", NamedTextColor.GRAY),
                    Component.text((cpuPercent < 0 ? "..." : cpuPercent) + "%", NamedTextColor.GREEN),

                    // TPS
                    Component.text("\nTPS: ", NamedTextColor.GRAY),
                    Component.text(String.valueOf(tps), NamedTextColor.GREEN),
                    Component.text(" (", NamedTextColor.GRAY),
                    Component.text((floor(tickMs * 1000.0) / 1000) + "ms",
                            TextColor.lerp((float) (tickMs / maxTickMs), NamedTextColor.GREEN, NamedTextColor.RED)),
                    Component.text(")\n", NamedTextColor.GRAY),

                    createGcComponent(),

                    // Entities
                    Component.text("\nEntities: ", NamedTextColor.GRAY)
                            .append(Component.text(entities + onlinePlayers, NamedTextColor.GOLD))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text()
                                            .append(Component.text("Players: ", NamedTextColor.GRAY))
                                            .append(Component.text(String.valueOf(onlinePlayers), NamedTextColor.GOLD))
                                            .append(Component.text("\nEntities: ", NamedTextColor.GRAY))
                                            .append(Component.text(String.valueOf(entities), NamedTextColor.GOLD))
                                            .build()
                            )),

                    // Instances
                    Component.text("\nInstances: ", NamedTextColor.GRAY)
                            .append(Component.text(String.valueOf(instances.size()), NamedTextColor.GOLD))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text()
                                            .append(Component.text("Shared: ", NamedTextColor.GRAY))
                                            .append(Component.text(String.valueOf(sharedInstances), NamedTextColor.GOLD))
                                            .append(Component.text("\nChunks: ", NamedTextColor.GRAY))
                                            .append(Component.text(String.valueOf(chunks), NamedTextColor.GOLD))
                                            .build()
                            ))
            );

            sender.sendMessage(armify(message, 40));
        });
    }

    // ---------------- GC Components ----------------

    private Component createGcComponent() {
        TextComponent.Builder builder = Component.text()
                .append(Component.text("GC Info:", NamedTextColor.GRAY));

        for (var entry : getGcInfo().entrySet()) {
            GcInfo info = entry.getValue();
            long millisSinceRun = getUptime() - info.getEndTime();
            String lastRunText = parsed(millisSinceRun / 1000);

            Component entryComponent = Component.text()
                    .append(Component.text("\n  " + entry.getKey() + ":", NamedTextColor.GRAY))
                    .append(Component.text("\n    Last Run: ", NamedTextColor.GRAY))
                    .append(Component.text(lastRunText, NamedTextColor.GOLD))
                    .hoverEvent(HoverEvent.showText(createGcHover(entry.getKey(), info)))
                    .build();

            builder.append(entryComponent);
        }
        return builder.build();
    }

    private Component createGcHover(String name, GcInfo info) {
        return Component.text()
                .append(Component.text("Name: ", NamedTextColor.GOLD))
                .append(Component.text(name, NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Duration: ", NamedTextColor.GOLD))
                .append(Component.text(info.getDuration() + "ms", NamedTextColor.GRAY))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Memory After:", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(createMemoryUsagePeriod(info.getMemoryUsageAfterGc()))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Memory Before:", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(createMemoryUsagePeriod(info.getMemoryUsageBeforeGc()))
                .build();
    }

    private Component createMemoryUsagePeriod(Map<String, MemoryUsage> memoryUsageMap) {
        List<Component> lines = new ArrayList<>();

        for (var entry : memoryUsageMap.entrySet()) {
            String key = entry.getKey();
            MemoryUsage usage = entry.getValue();

            if (EXCLUDED_MEMORY_SPACES.stream().anyMatch(p -> p.matcher(key).find()))
                continue;

            lines.add(
                    Component.text()
                            .append(Component.text("  " + key + ": ", NamedTextColor.GOLD))
                            .append(Component.text((usage.getUsed() / 1024 / 1024) + " MB", NamedTextColor.GRAY))
                            .build()
            );
        }

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private Map<String, GcInfo> getGcInfo() {
        Map<String, GcInfo> map = new HashMap<>();
        for (var bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean instanceof GarbageCollectorMXBean gc) {
                GcInfo info = gc.getLastGcInfo();
                if (info != null) {
                    map.put(gc.getName(), info);
                }
            }
        }
        return map;
    }

    private long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private double getCpuLoadPercent() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
            AttributeList list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});

            if (list.isEmpty()) return -1;

            var att = (Attribute) list.get(0);
            Double value = (Double) att.getValue();
            if (value == null || value < 0) return -1;

            return floor(value * 10000) / 100.0;
        } catch (Exception e) {
            return -1;
        }
    }

    Component armify(Component component, int length){
        return Component.text()
                .append(Component.text(" ".repeat(length) + "\n", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                .append(component)
                .append(Component.text("\n" + " ".repeat(length), NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                .build();
    }

    String parsed(long millis) {
        if (millis == 0L) return "0s";

        StringBuilder stringBuilder = new StringBuilder();
        long days = millis / 86400;
        long hours = (millis % 86400) / 3600;
        long minutes = (millis % 3600) / 60;
        long seconds = millis % 60;

        if (days > 0) stringBuilder.append(days).append("d ");
        if (hours > 0) stringBuilder.append(hours).append("h ");
        if (minutes > 0) stringBuilder.append(minutes).append("m ");
        if (seconds > 0) stringBuilder.append(seconds).append("s ");

        return stringBuilder.toString().trim();
    }

}
