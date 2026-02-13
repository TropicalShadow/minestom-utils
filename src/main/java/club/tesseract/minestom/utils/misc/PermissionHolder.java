package club.tesseract.minestom.utils.misc;

import net.kyori.adventure.audience.Audience;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.identity.NamedAndIdentified;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

public interface PermissionHolder extends NamedAndIdentified, Audience {
    /**
     * The LuckPerms instance used for permission management.
     * This is a static reference to the LuckPerms API, which can be used to access permissions and roles.
     */
    LuckPerms luckperms = LuckPermsProvider.get();

    default PlayerAdapter<Player> getPlayerAdapter() {
        return luckperms.getPlayerAdapter(Player.class);
    }

    default User getLuckpermsUser(){
        return getPlayerAdapter().getUser((Player) this);
    }


    @Nullable
    default Group getTopGroup() {
        String primaryGroup = getLuckpermsUser().getPrimaryGroup();
        return luckperms.getGroupManager().getGroup(primaryGroup);
    }

    default OptionalInt getTopGroupWeight() {
        Group topGroup = getTopGroup();
        if (topGroup == null) {
            return OptionalInt.empty();
        }
        return topGroup.getWeight();
    }

    default String getSuffix() {
        return Optional.ofNullable(getLuckpermsUser().getCachedData().getMetaData().getSuffix()).orElse("");
    }

    default String getPrefix() {
        return Optional.ofNullable(getLuckpermsUser().getCachedData().getMetaData().getPrefix()).orElse("");
    }

    @NotNull
    default CompletableFuture<@NotNull DataMutateResult> setPermission(String permission, boolean value) {
        DataMutateResult result = getLuckpermsUser().data().add(Node.builder(permission).value(value).build());

        return luckperms.getUserManager().saveUser(getLuckpermsUser()).thenApply((x) -> result);
    }

    /**
     * Override for custom permission user loading logic.
     */
    default void onPermissionUserLoad(){

    }

    /**
     * Override for custom permission recalculation logic.
     */
    default void onPermissionRecalculate() {

    }

    /**
     * Checks if this holder has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the holder has the permission, false otherwise
     */
    default Tristate hasPermission(String permission) {
        return getLuckpermsUser().getCachedData().permissionData().get(QueryOptions.defaultContextualOptions()).checkPermission(permission);
    }
}
