package club.tesseract.minestom.utils.command.condition;


import club.tesseract.minestom.utils.misc.PermissionHolder;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@SuppressWarnings("unused")
public final class ExtraConditions {

    public static ArrayList<String> permissions = new ArrayList<>();

    public static @NotNull CommandCondition orOp(@NotNull CommandCondition... conditions) {
        return or(isOp(), and(conditions));
    }

    public static @NotNull CommandCondition or(@NotNull CommandCondition... conditions) {
        return Conditions.any(conditions);
    }

    public static @NotNull CommandCondition and(@NotNull CommandCondition... conditions) {
        return Conditions.all(conditions);
    }

    public static @NotNull CommandCondition alwaysTrue() {
        return (sender, _) -> true;
    }

    public static @NotNull CommandCondition isPlayer() {
        return (sender, _) -> {
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

            return false;
        };
    }

    public static @NotNull CommandCondition isOp() {
        return (sender, _) -> {
            if (sender instanceof ConsoleSender) {
                return true; // Console is considered as op
            }

            if(!(sender instanceof Player player)){
                return false;
            }

            return player.getPermissionLevel() >= 4;
        };
    }

    public static CommandCondition isTropical(){
        return (sender, _) -> {
            if (sender instanceof ConsoleSender) {
                return true; // Console can execute this command
            }

            Auth auth = MinecraftServer.process().auth();
            if((auth instanceof Auth.Velocity || auth instanceof Auth.Online)){
                if(sender instanceof Player player){
                    return player.getUsername().equals("TropicalShadow") || player.getUsername().equals("BridgeSplash");
                }
            }
            return false; // Non-player entities cannot execute this command
        };
    }

    private ExtraConditions() {
        throw new AssertionError("This class cannot be instantiated.");
    }
}
