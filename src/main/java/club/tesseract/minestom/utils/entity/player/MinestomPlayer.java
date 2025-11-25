package club.tesseract.minestom.utils.entity.player;

import club.tesseract.minestom.utils.misc.PermissionHolder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;

public class MinestomPlayer extends Player implements PermissionHolder {
    public MinestomPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    public static void register(){
        MinecraftServer.getConnectionManager().setPlayerProvider(MinestomPlayer::new);
    }
}
