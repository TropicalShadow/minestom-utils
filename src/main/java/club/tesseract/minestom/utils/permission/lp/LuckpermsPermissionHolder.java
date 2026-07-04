package club.tesseract.minestom.utils.permission.lp;

import club.tesseract.minestom.utils.permission.PermissionHolder;
import club.tesseract.minestom.utils.permission.SetPermissionResult;
import club.tesseract.minestom.utils.permission.TriState;
import club.tesseract.minestom.utils.permission.group.PermissionGroup;
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

import java.util.*;
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
    public @NotNull Set<PermissionGroup> getGroups() {
        Set<PermissionGroup> groups = new HashSet<>();
        for (var group : getLuckpermsUser().getInheritedGroups(QueryOptions.defaultContextualOptions())) {
            groups.add(new PermissionGroup(group.getName(), group.getWeight().orElse(0)));
        }
        return groups;
    }

    @Override
    public void setGroups(@NotNull Set<PermissionGroup> groups) {
        var user = getLuckpermsUser();
        var data = user.data();

        data.clear(node -> node instanceof InheritanceNode);

        List<PermissionGroup> sorted = new ArrayList<>(groups);
        sorted.sort(Comparator.comparingInt(PermissionGroup::getWeight));

        for (PermissionGroup group : sorted) {
            data.add(InheritanceNode.builder(group.getName()).build());
        }

        luckperms().getUserManager().saveUser(user).join();
    }

    @Override
    public @Nullable PermissionGroup getPrimaryGroup() {
        return getGroups().stream()
                .max(Comparator.comparingInt(PermissionGroup::getWeight))
                .orElse(null);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull SetPermissionResult> setPermission(String permission, @Nullable Boolean value) {
        var user = getLuckpermsUser();
        DataMutateResult result;

        if (value == null) {
            user.data().clear(node -> node.getKey().equalsIgnoreCase(permission));
            result = DataMutateResult.SUCCESS;
        } else {
            result = user.data().add(Node.builder(permission).value(value).build());
        }

        return luckperms().getUserManager().saveUser(user)
                .thenApply((ignored) -> new SetPermissionResult(result.wasSuccessful(), "luckperms handling", result));
    }

    @Override
    public @NotNull String getSuffix() {
        return Optional.ofNullable(getLuckpermsUser().getCachedData().getMetaData().getSuffix()).orElse("");
    }

    @Override
    public @NotNull String getPrefix() {
        return Optional.ofNullable(getLuckpermsUser().getCachedData().getMetaData().getPrefix()).orElse("");
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
