package club.tesseract.minestom.utils.permission;

import net.kyori.adventure.audience.Audience;
import net.minestom.server.utils.identity.NamedAndIdentified;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface PermissionHolder extends NamedAndIdentified, Audience {



    @Nullable
    String getSuffix();

    @Nullable
    String getPrefix();

    @NotNull
    CompletableFuture<@NotNull SetPermissionResult> setPermission(String permission, boolean value);

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
     * @return tristate, true, false, or default (which may be treated as true or false depending on the implementation)
     */
    TriState hasPermission(String permission);
}
