package club.tesseract.minestom.utils.permission;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPermissionHolder implements PermissionHolder {

    private final Map<String, TriState> permissions = new ConcurrentHashMap<>();
    private final Player player;

    private String suffix;
    private String prefix;

    public DefaultPermissionHolder(Player player) {
        this.player = player;
    }

    @Override
    public @Nullable String getSuffix() {
        return suffix;
    }

    @Override
    public @Nullable String getPrefix() {
        return prefix;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull SetPermissionResult> setPermission(String permission, boolean value) {
        if (permission.startsWith("prefix.")) {
            this.prefix = permission.substring("prefix.".length());
        }
        if (permission.startsWith("suffix.")) {
            this.suffix = permission.substring("suffix.".length());
        }

        return CompletableFuture.supplyAsync(() -> {
            this.permissions.put(permission, value ? TriState.TRUE : TriState.FALSE);
            return new SetPermissionResult(true, null, null);
        });
    }

    @Override
    public TriState hasPermission(String permission) {
        return this.permissions.getOrDefault(permission, TriState.DEFAULT);
    }

    @Override
    public @NotNull Component getName() {
        return this.player.getName();
    }

    @Override
    public @NotNull UUID getUuid() {
        return this.player.getUuid();
    }
}
