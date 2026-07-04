package club.tesseract.minestom.utils.permission.lp;

import club.tesseract.minestom.utils.permission.PermissionHolder;
import club.tesseract.minestom.utils.permission.SetPermissionResult;
import club.tesseract.minestom.utils.permission.TriState;
import club.tesseract.minestom.utils.permission.group.PermissionGroup;
import club.tesseract.minestom.utils.permission.group.PermissionGroupRegistry;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LuckpermsPermissionHolder implements PermissionHolder {

    private final Player player;

    public LuckpermsPermissionHolder(Player player) {
        this.player = player;
    }

    /**
     * Lazily retrieves the LuckPerms instance to avoid class-load-time failures
     * when LuckPerms has not yet been registered.
     */
    private static LuckPerms luckperms() {
        return LuckPermsProvider.get();
    }

    PlayerAdapter<Player> getPlayerAdapter() {
        return luckperms().getPlayerAdapter(Player.class);
    }

    public User getLuckpermsUser() {
        return getPlayerAdapter().getUser(this.player);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull SetPermissionResult> setPermission(String permission, @Nullable Boolean value) {
        var user = getLuckpermsUser();
        DataMutateResult result;

        String normalized = permission.toLowerCase();
        if (normalized.startsWith("group.")) {
            String groupId = normalized.substring("group.".length());
            PermissionGroup group = PermissionGroupRegistry.get(groupId);
            if (group != null) {
                if (Boolean.TRUE.equals(value)) {
                    result = user.data().add(InheritanceNode.builder(group.getName()).build());
                } else if (Boolean.FALSE.equals(value)) {
                    result = user.data().remove(InheritanceNode.builder(group.getName()).build());
                } else {
                    user.data().clear(node -> node instanceof InheritanceNode && ((InheritanceNode) node).getGroupName().equalsIgnoreCase(group.getName()));
                    result = DataMutateResult.SUCCESS;
                }

                return luckperms().getUserManager().saveUser(user)
                        .thenApply((ignored) -> new SetPermissionResult(result.wasSuccessful(), "luckperms handling", result));
            }
        }

        if (value == null) {
            user.data().clear(node -> node.getKey().equalsIgnoreCase(normalized));
            result = DataMutateResult.SUCCESS;
        } else {
            result = user.data().add(Node.builder(normalized).value(value).build());
        }

        return luckperms().getUserManager().saveUser(user)
                .thenApply((ignored) -> new SetPermissionResult(result.wasSuccessful(), "luckperms handling", result));
    }

    @Override
    public @Nullable String getSuffix() {
        return getLuckpermsUser().getCachedData().getMetaData().getSuffix();
    }

    @Override
    public @Nullable String getPrefix() {
        return getLuckpermsUser().getCachedData().getMetaData().getPrefix();
    }

    @Override
    public TriState hasPermission(String permission) {
        var lpTristate = getLuckpermsUser().getCachedData().permissionData().get(QueryOptions.defaultContextualOptions()).checkPermission(permission);

        switch (lpTristate) {
            case TRUE -> {
                return TriState.TRUE;
            }
            case FALSE -> {
                return TriState.FALSE;
            }
            default -> {
                return TriState.DEFAULT;
            }
        }
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
