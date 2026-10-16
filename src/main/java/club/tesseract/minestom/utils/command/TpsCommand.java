package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import club.tesseract.minestom.utils.misc.Palette;

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

        setCondition(ExtraConditions.orOp(ExtraConditions.hasPermission("minecraft.command.tps")));

        MinecraftServer.getGlobalEventHandler().addListener(ServerTickMonitorEvent.class, event -> LAST_TICK.set(event.getTickMonitor()));
        ConnectionManager connectionManager = MinecraftServer.getConnectionManager();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        setDefaultExecutor((sender, _) -> {
            long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            long ramUsage = totalMem - freeMem;

            double cpuPercent = getCpuLoadPercent();

            TickMonitor monitor = LAST_TICK.get();
            if(monitor == null){
                sender.sendMessage(Component.text("No tick monitor available"));
                return;
            }
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
                    Component.text("RAM: ", Palette.MUTED.color()),
                    Component.text(ramUsage + "MB / " + totalMem + "MB", Palette.MUTED.color()),
                    Component.text(" (", Palette.MUTED.color()),
                    Component.text(((int) floor((ramUsage * 100.0) / totalMem)) + "%", Palette.SUCCESS.color()),
                    Component.text(")", Palette.MUTED.color()),

                    // CPU
                    Component.text("\nCPU: ", Palette.MUTED.color()),
                    Component.text((cpuPercent < 0 ? "..." : cpuPercent) + "%", Palette.SUCCESS.color()),

                    // TPS
                    Component.text("\nTPS: ", Palette.MUTED.color()),
                    Component.text(String.valueOf(tps), Palette.SUCCESS.color()),
                    Component.text(" (", Palette.MUTED.color()),
                    Component.text((floor(tickMs * 1000.0) / 1000) + "ms",
                            Palette.gauge(tickMs / maxTickMs)),
                    Component.text(")\n", Palette.MUTED.color()),

                    createGcComponent(),

                    // Entities
                    Component.text("\nEntities: ", Palette.MUTED.color())
                            .append(Component.text(entities + onlinePlayers, Palette.ACCENT.color()))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text()
                                            .append(Component.text("Players: ", Palette.MUTED.color()))
                                            .append(Component.text(String.valueOf(onlinePlayers), Palette.ACCENT.color()))
                                            .append(Component.text("\nEntities: ", Palette.MUTED.color()))
                                            .append(Component.text(String.valueOf(entities), Palette.ACCENT.color()))
                                            .build()
                            )),

                    // Instances
                    Component.text("\nInstances: ", Palette.MUTED.color())
                            .append(Component.text(String.valueOf(instances.size()), Palette.ACCENT.color()))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text()
                                            .append(Component.text("Shared: ", Palette.MUTED.color()))
                                            .append(Component.text(String.valueOf(sharedInstances), Palette.ACCENT.color()))
                                            .append(Component.text("\nChunks: ", Palette.MUTED.color()))
                                            .append(Component.text(String.valueOf(chunks), Palette.ACCENT.color()))
                                            .build()
                            ))
            );

            sender.sendMessage(armify(message, 40));
        });
    }

    // ---------------- GC Components ----------------

    private Component createGcComponent() {
        TextComponent.Builder builder = Component.text()
                .append(Component.text("GC Info:", Palette.MUTED.color()));

        for (var entry : getGcInfo().entrySet()) {
            GcInfo info = entry.getValue();
            long millisSinceRun = getUptime() - info.getEndTime();
            String lastRunText = parsed(millisSinceRun / 1000);

            Component entryComponent = Component.text()
                    .append(Component.text("\n  " + entry.getKey() + ":", Palette.MUTED.color()))
                    .append(Component.text("\n    Last Run: ", Palette.MUTED.color()))
                    .append(Component.text(lastRunText, Palette.ACCENT.color()))
                    .hoverEvent(HoverEvent.showText(createGcHover(entry.getKey(), info)))
                    .build();

            builder.append(entryComponent);
        }
        return builder.build();
    }

    private Component createGcHover(String name, GcInfo info) {
        return Component.text()
                .append(Component.text("Name: ", Palette.ACCENT.color()))
                .append(Component.text(name, Palette.MUTED.color()))
                .append(Component.newline())
                .append(Component.text("Duration: ", Palette.ACCENT.color()))
                .append(Component.text(info.getDuration() + "ms", Palette.MUTED.color()))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Memory After:", Palette.ACCENT.color()))
                .append(Component.newline())
                .append(createMemoryUsagePeriod(info.getMemoryUsageAfterGc()))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Memory Before:", Palette.ACCENT.color()))
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
                            .append(Component.text("  " + key + ": ", Palette.ACCENT.color()))
                            .append(Component.text((usage.getUsed() / 1024 / 1024) + " MB", Palette.MUTED.color()))
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

            var att = (Attribute) list.getFirst();
            Double value = (Double) att.getValue();
            if (value == null || value < 0) return -1;

            return floor(value * 10000) / 100.0;
        } catch (Exception e) {
            return -1;
        }
    }

    Component armify(Component component, int length){
        return Component.text()
                .append(Component.text(" ".repeat(length) + "\n", Palette.SUBTLE.color(), TextDecoration.STRIKETHROUGH))
                .append(component)
                .append(Component.text("\n" + " ".repeat(length), Palette.SUBTLE.color(), TextDecoration.STRIKETHROUGH))
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
