package club.tesseract.minestom.utils.permission.lp;

import club.tesseract.minestom.utils.permission.PermissionHolder;
import club.tesseract.minestom.utils.permission.SetPermissionResult;
import club.tesseract.minestom.utils.permission.TriState;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
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


    @Nullable
    public Group getTopGroup() {
        String primaryGroup = getLuckpermsUser().getPrimaryGroup();
        return luckperms().getGroupManager().getGroup(primaryGroup);
    }

    public OptionalInt getTopGroupWeight() {
        Group topGroup = getTopGroup();
        if (topGroup == null) {
            return OptionalInt.empty();
        }
        return topGroup.getWeight();
    }

    @Override
    public @NotNull CompletableFuture<@NotNull SetPermissionResult> setPermission(String permission, boolean value) {
        DataMutateResult result = getLuckpermsUser().data().add(Node.builder(permission).value(value).build());

        return luckperms().getUserManager().saveUser(getLuckpermsUser()).thenApply((ignored) -> new SetPermissionResult(result.wasSuccessful(), "luckperms handling", result));
    }

    public String getSuffix() {
        return Optional.ofNullable(getLuckpermsUser().getCachedData().getMetaData().getSuffix()).orElse("");
    }

    public String getPrefix() {
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
    public Component getName() {
        return this.player.getName();
    }

    @Override
    public UUID getUuid() {
        return this.player.getUuid();
    }
}
