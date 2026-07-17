package club.tesseract.minestom.utils.permission;

import club.tesseract.minestom.utils.permission.cache.PermissionCache;
import club.tesseract.minestom.utils.permission.event.PlayerPermissionsRecalculateEvent;
import club.tesseract.minestom.utils.permission.group.PermissionGroup;
import club.tesseract.minestom.utils.permission.group.PermissionGroupRegistry;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultPermissionHolder implements PermissionHolder {

    private final Player player;

    private final PermissionNode root = new PermissionNode();

    private final Set<String> groupIds = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "perm-recalc"));
    private volatile PermissionCache cache = new PermissionCache();

    public DefaultPermissionHolder(Player player) {
        this.player = player;
    }

    @Override
    public @Nullable String getSuffix() {
        return cache.suffix;
    }

    @Override
    public @Nullable String getPrefix() {
        return cache.prefix;
    }

    @Override
    public @NotNull CompletableFuture<SetPermissionResult> setPermission(String permission, Boolean value) {
        // TODO - correctly handle null value as unset permission node, remove from tree
        TriState state =
                Boolean.TRUE.equals(value) ? TriState.TRUE :
                        Boolean.FALSE.equals(value) ? TriState.FALSE :
                        TriState.DEFAULT;

        String normalized = permission.toLowerCase();
        if (normalized.startsWith("group.")) {
            String groupId = normalized.substring("group.".length());
            PermissionGroup group = PermissionGroupRegistry.get(groupId);
            if (group != null) {
                if (state == TriState.TRUE) {
                    groupIds.add(group.getId().toLowerCase());
                } else {
                    groupIds.remove(group.getId().toLowerCase());
                }
                recalcAsync();
                return CompletableFuture.completedFuture(new SetPermissionResult(true, null, group));
            }
        }

        apply(root, normalized, state);

        recalcAsync();

        return CompletableFuture.completedFuture(
                new SetPermissionResult(true, null, null)
        );
    }

    private void apply(PermissionNode node, String permission, TriState value) {
        String[] parts = permission.split("\\.");

        for (String part : parts) {
            node = node.children.computeIfAbsent(part, ignored -> new PermissionNode());
        }

        node.value = value;
    }

    @Override
    public TriState hasPermission(String permission) {
        String normalized = permission.toLowerCase();
        Map<String, TriState> perms = cache.permissions;

        // Direct match first
        TriState direct = perms.get(normalized);
        if (direct != null && direct != TriState.DEFAULT) {
            return direct;
        }

        // Check wildcard parents: command.test.foo -> command.test.* -> command.* -> *
        String[] parts = normalized.split("\\.");
        for (int i = parts.length - 1; i >= 0; i--) {
            StringBuilder wildcard = new StringBuilder();
            for (int j = 0; j < i; j++) {
                if (j > 0) wildcard.append(".");
                wildcard.append(parts[j]);
            }
            if (wildcard.length() > 0) wildcard.append(".");
            wildcard.append("*");

            TriState wildcardResult = perms.get(wildcard.toString());
            if (wildcardResult != null && wildcardResult != TriState.DEFAULT) {
                return wildcardResult;
            }
        }

        // Check global wildcard
        TriState globalWildcard = perms.get("*");
        if (globalWildcard != null && globalWildcard != TriState.DEFAULT) {
            return globalWildcard;
        }

        return direct != null ? direct : TriState.DEFAULT;
    }


    private void recalcAsync() {
        executor.submit(this::recalculate);
    }

    private void recalculate() {

        PermissionCache newCache = new PermissionCache();

        flattenTree(root, "", newCache.permissions);

        List<PermissionGroup> allGroups = collectAllGroups(resolveGroups());
        allGroups.sort(Comparator.comparingInt(PermissionGroup::getWeight));

        for (PermissionGroup group : allGroups) {
            flattenTree(group.getRoot(), "", newCache.permissions);
        }

        newCache.prefix = resolveMeta("prefix");
        newCache.suffix = resolveMeta("suffix");

        this.cache = newCache;

        MinecraftServer.getGlobalEventHandler()
                .call(new PlayerPermissionsRecalculateEvent(player));
    }


    private List<PermissionGroup> collectAllGroups(List<PermissionGroup> topGroups) {
        Set<String> visited = new HashSet<>();
        List<PermissionGroup> all = new ArrayList<>();
        Deque<PermissionGroup> stack = new ArrayDeque<>(topGroups);
        while (!stack.isEmpty()) {
            PermissionGroup group = stack.pop();
            if (!visited.add(group.getId())) continue;
            all.add(group);
            for (PermissionGroup parent : group.getParents()) {
                stack.push(parent);
            }
        }
        return all;
    }

    private void flattenTree(PermissionNode node, String prefix, Map<String, TriState> out) {

        if (node == null) return;

        if (node.value != TriState.DEFAULT) {
            out.put(prefix.isEmpty() ? "*" : prefix, node.value);
        }

        for (Map.Entry<String, PermissionNode> entry : node.children.entrySet()) {

            String nextKey = prefix.isEmpty()
                    ? entry.getKey()
                    : prefix + "." + entry.getKey();

            flattenTree(entry.getValue(), nextKey, out);
        }
    }


    private String resolveMeta(String key) {

        String best = null;

        List<PermissionGroup> allGroups = collectAllGroups(resolveGroups());
        allGroups.sort((a, b) -> Integer.compare(b.getWeight(), a.getWeight()));

        for (PermissionGroup group : allGroups) {
            String value = group.getMeta(key);
            if (value != null) {
                best = value;
            }
        }

        return best;
    }

    private List<PermissionGroup> resolveGroups() {
        List<PermissionGroup> groups = new ArrayList<>();
        for (String id : groupIds) {
            PermissionGroup group = PermissionGroupRegistry.get(id);
            if (group != null) {
                groups.add(group);
            }
        }
        return groups;
    }

    @Override
    public @NotNull Component getName() {
        return player.getName();
    }

    @Override
    public @NotNull java.util.UUID getUuid() {
        return player.getUuid();
    }
}