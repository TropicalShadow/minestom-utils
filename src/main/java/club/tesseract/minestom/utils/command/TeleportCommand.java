package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.PlayerArgument;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Optional;

public class TeleportCommand extends Command {

    PlayerArgument multiplePlayersArgument = new PlayerArgument("targets", false);
    PlayerArgument targetPlayerArgument = new PlayerArgument("destination", true);


    public TeleportCommand() {
        super("teleport", "tp");

        setCondition(ExtraConditions.hasPermission("minecraft.command.tp"));

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("Usage: /teleport <player> [player]");
        });


        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                return;
            }
            Optional<Player> destination = targetPlayerArgument.getPlayer(sender, context);
            if (destination.isEmpty()) {
                sender.sendMessage(Component.translatable("commands.teleport.invalidPosition", NamedTextColor.RED));
                return;
            }

            handleTeleport(player, destination.get());
        }, targetPlayerArgument);

        addSyntax((sender, context) -> {
            List<Player> targets = multiplePlayersArgument.getPlayers(sender, context);
            Optional<Player> destinationOpt = targetPlayerArgument.getPlayer(sender, context);

            if (targets.isEmpty()) {
                sender.sendMessage(Component.text("Invalid targets", NamedTextColor.RED));
                return;
            }
            if (destinationOpt.isEmpty()) {
                sender.sendMessage(Component.translatable("commands.teleport.invalidPosition", NamedTextColor.RED));
                return;
            }

            Player destination = destinationOpt.get();

            for (Entity entity : targets) {
                if (entity.getInstance() != destination.getInstance()) {
                    entity.setInstance(destination.getInstance(),       destination.getPosition());
                    return;
                }
                entity.teleport(destination.getPosition());
            }

            if (targets.size() > 1) {
                sender.sendMessage(Component.translatable("commands.teleport.success.entity.multiple")
                        .arguments(Component.text(targets.size()), destination.getName()));
            } else {
                Player target = targets.getFirst();
                sender.sendMessage(Component.translatable("commands.teleport.success.entity.single")
                        .arguments(target.getName(), destination.getName()));
            }
        }, multiplePlayersArgument, targetPlayerArgument);
    }


    public static void handleTeleport(Player player, Player destination) {
        if(player.getInstance().equals(destination.getInstance())) {
            player.teleport(destination.getPosition());
        }else{
            player.setInstance(destination.getInstance(), destination.getPosition());
        }
        player.sendMessage(Component.translatable("commands.teleport.success.entity.single")
                .arguments(player.getName(), destination.getName()));
    }
}
