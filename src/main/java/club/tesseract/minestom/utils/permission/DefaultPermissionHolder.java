package club.tesseract.minestom.utils.permission;

import club.tesseract.minestom.utils.permission.cache.PermissionCache;
import club.tesseract.minestom.utils.permission.event.PlayerPermissionsRecalculateEvent;
import club.tesseract.minestom.utils.permission.group.PermissionGroup;
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

    private final Set<PermissionGroup> groups = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "perm-recalc"));
    private volatile PermissionCache cache = new PermissionCache();

    public DefaultPermissionHolder(Player player) {
        this.player = player;
    }

    public void addGroup(PermissionGroup group) {
        groups.add(group);
        recalcAsync();
    }

    public void removeGroup(PermissionGroup group) {
        groups.remove(group);
        recalcAsync();
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
    public @NotNull Set<PermissionGroup> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    @Override
    public void setGroups(@NotNull Set<PermissionGroup> groups) {
        this.groups.clear();
        this.groups.addAll(groups);
        recalcAsync();
    }

    @Override
    public @Nullable PermissionGroup getPrimaryGroup() {
        return groups.stream()
                .max(Comparator.comparingInt(PermissionGroup::getWeight))
                .orElse(null);
    }

    @Override
    public @NotNull CompletableFuture<SetPermissionResult> setPermission(String permission, Boolean value) {

        TriState state =
                Boolean.TRUE.equals(value) ? TriState.TRUE :
                        Boolean.FALSE.equals(value) ? TriState.FALSE :
                        TriState.DEFAULT;

        apply(root, permission.toLowerCase(), state);

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
        return cache.permissions.getOrDefault(permission.toLowerCase(), TriState.DEFAULT);
    }


    private void recalcAsync() {
        executor.submit(this::recalculate);
    }

    private void recalculate() {

        PermissionCache newCache = new PermissionCache();

        flattenTree(root, "", newCache.permissions);

        List<PermissionGroup> sorted = new ArrayList<>(groups);
        sorted.sort((a, b) -> Integer.compare(b.getWeight(), a.getWeight()));

        for (PermissionGroup group : sorted) {
            resolveGroup(group, newCache, new HashSet<>());
        }

        newCache.prefix = resolveMeta("prefix");
        newCache.suffix = resolveMeta("suffix");

        this.cache = newCache;

        MinecraftServer.getGlobalEventHandler()
                .call(new PlayerPermissionsRecalculateEvent(player));
    }


    private void resolveGroup(PermissionGroup group, PermissionCache cache, Set<String> visited) {

        if (!visited.add(group.getName())) return; // cycle protection

        flattenTree(group.getRoot(), "", cache.permissions);

        for (PermissionGroup parent : group.getParents()) {
            resolveGroup(parent, cache, visited);
        }
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

        List<PermissionGroup> sorted = new ArrayList<>(groups);
        sorted.sort((a, b) -> Integer.compare(b.getWeight(), a.getWeight()));

        for (PermissionGroup group : sorted) {
            best = resolveGroupMeta(group, key, best, new HashSet<>());
        }

        return best;
    }

    private String resolveGroupMeta(
            PermissionGroup group,
            String key,
            @Nullable String current,
            Set<String> visited
    ) {
        if (!visited.add(group.getName())) return current;

        String value = group.getMeta(key);

        if (value != null) {
            current = value;
        }

        for (PermissionGroup parent : group.getParents()) {
            current = resolveGroupMeta(parent, key, current, visited);
        }

        return current;
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