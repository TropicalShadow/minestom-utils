package club.tesseract.minestom.utils.entity.player;

import club.tesseract.minestom.utils.permission.DefaultPermissionHolder;
import club.tesseract.minestom.utils.permission.PermissionHolder;
import lombok.experimental.Delegate;
import net.kyori.adventure.audience.Audience;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.utils.identity.NamedAndIdentified;

import java.util.function.Function;

public class MinestomPlayer extends Player implements PermissionHolder {

    @Delegate(types = PermissionHolder.class, excludes = {Audience.class, NamedAndIdentified.class})
    private final PermissionHolder holder;

    public MinestomPlayer(PlayerConnection playerConnection, GameProfile gameProfile, Function<Player, PermissionHolder> holder) {
        super(playerConnection, gameProfile);
        this.holder = holder.apply(this);
    }

    public static void register(){
        MinecraftServer.getConnectionManager().setPlayerProvider((conn, profile) -> new MinestomPlayer(conn, profile, DefaultPermissionHolder::new));
    }

    public static void register(Function<Player, PermissionHolder> permissionHolderFunction) {
        MinecraftServer.getConnectionManager().setPlayerProvider((conn, profile) -> new MinestomPlayer(conn, profile, permissionHolderFunction));
    }
}
