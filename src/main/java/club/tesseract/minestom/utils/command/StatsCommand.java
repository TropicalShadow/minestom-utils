package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.PlayerArgument;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import club.tesseract.minestom.utils.misc.Palette;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentUUID;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.network.ConnectionManager;

import java.util.Optional;
import java.util.UUID;

@CommandMetadata(
        categories = {CommandCategory.SERVER, CommandCategory.ADMIN},
        description = "Displays stats about the server"
)
public class StatsCommand extends Command {
    public StatsCommand() {
        super("stats");

        setCondition(ExtraConditions.orOp(ExtraConditions.hasPermission("minecraft.command.stats")));

        addSubcommand(new InstanceStatsCommand());
        addSubcommand(new PlayerStatsCommand());
    }


    private static final class PlayerStatsCommand extends Command {
        private static final PlayerArgument PLAYER_ARGUMENT = new PlayerArgument("target", true);

        public PlayerStatsCommand() {
            super("player");

            ConnectionManager connectionManager = MinecraftServer.getConnectionManager();

            setDefaultExecutor((sender, _) -> {
                Component message = Component.text("Players Online (", Palette.MUTED.color())
                        .append(Component.text(connectionManager.getOnlinePlayers().size(), Palette.SUCCESS.color()))
                        .append(Component.text(") ", Palette.MUTED.color()));

                Player[] players = connectionManager.getOnlinePlayers().stream().limit(10).toArray(Player[]::new);
                if (players.length > 0) {
                    message = message.append(Component.newline());
                    boolean first = true;
                    for (Player player : players) {
                        if (!first) {
                            message = message.append(Component.text(", ", Palette.MUTED.color()));
                        }
                        first = false;
                        message = message.append(Component.text(player.getUsername(), Palette.SUCCESS.color()).hoverEvent(getPlayerStats(player))
                                .clickEvent(ClickEvent.copyToClipboard(player.getUuid().toString())));

                    }
                } else {
                    message = message.append(Component.newline())
                            .append(Component.text("No players online", Palette.ERROR.color()));
                }

                sender.sendMessage(message);
            });


            addSyntax((sender, context) -> {
                Optional<Player> targetPlayer = PLAYER_ARGUMENT.getPlayer(sender, context);
                if (targetPlayer.isEmpty()) {
                    sender.sendMessage(Component.text("Please specify a player", Palette.ERROR.color()));
                    return;
                }

                sender.sendMessage(getPlayerStats(targetPlayer.get()));
            }, PLAYER_ARGUMENT);
        }

        Component getPlayerStats(Player player) {
            return Component.text("Player: ", Palette.MUTED.color())
                    .append(Component.text(player.getUsername(), Palette.SUCCESS.color()).clickEvent(ClickEvent.copyToClipboard(player.getUsername())))
                    .append(Component.newline())
                    .append(Component.text("UUID: ", Palette.MUTED.color()))
                    .append(Component.text(player.getUuid().toString(), Palette.SUCCESS.color()).clickEvent(ClickEvent.copyToClipboard(player.getUuid().toString())))
                    .append(Component.newline())
                    .append(Component.text("Instance: ", Palette.MUTED.color()))
                    .append(Component.text(player.getInstance().getUuid().toString(), Palette.SUCCESS.color()).hoverEvent(getInstanceStats(player.getInstance())).clickEvent(ClickEvent.copyToClipboard(player.getInstance().getUuid().toString())));
        }
    }

    private static final class InstanceStatsCommand extends Command {
        public static final ArgumentUUID INSTANCE_UUID = ArgumentType.UUID("target");

        public InstanceStatsCommand() {
            super("instance");

            InstanceManager instanceManager = MinecraftServer.getInstanceManager();


            setDefaultExecutor((sender, _) -> {
                Component message = Component.text("Instance (", Palette.MUTED.color())
                        .append(Component.text(instanceManager.getInstances().size(), Palette.SUCCESS.color()))
                        .append(Component.text(") ", Palette.MUTED.color()))
                        ;

                if (instanceManager.getInstances().size() <= 10) {
                    message = message.append(Component.newline());
                    boolean first = true;
                    for (Instance instance : instanceManager.getInstances()) {
                        if (!first) {
                            message = message.append(Component.text(", ", Palette.MUTED.color()));
                        }
                        first = false;
                        message = message.append(Component.text(instance.getUuid().toString(), Palette.SUCCESS.color()).hoverEvent(getInstanceStats(instance)).clickEvent(ClickEvent.copyToClipboard(instance.getUuid().toString())));
                    }
                } else {
                    message = message.append(Component.newline())
                            .append(Component.text("Too many instances to display", Palette.ERROR.color()));
                }

                sender.sendMessage(message);
            });

            addSyntax((sender, context) -> {
                UUID instanceUuid;
                try {
                     instanceUuid = context.get(INSTANCE_UUID);
                }catch (NullPointerException ex){
                    sender.sendMessage(Component.text("Please specify an instance UUID", Palette.ERROR.color()));
                    return;
                }

                Instance instance = instanceManager.getInstance(instanceUuid);
                if (instance == null) {
                    sender.sendMessage(Component.text("Instance not found", Palette.ERROR.color()));
                    return;
                }

                sender.sendMessage(getInstanceStats(instance));
            }, INSTANCE_UUID);

        }

    }

    static Component getInstanceStats(Instance instance) {
        return Component.text("Instance UUID: ", Palette.MUTED.color())
                .append(Component.text(instance.getUuid().toString(), Palette.SUCCESS.color()))
                .append(Component.newline())
                .append(Component.text("Players: ", Palette.MUTED.color()))
                .append(Component.text(instance.getPlayers().size(), Palette.SUCCESS.color()))
                .append(Component.newline())
                .append(Component.text("Entities: ", Palette.MUTED.color()))
                .append(Component.text(instance.getEntities().stream().filter(ent -> !(ent instanceof Player)).count(), Palette.SUCCESS.color()))
                .append(Component.newline())
                .append(Component.text("Chunks Loaded: ", Palette.MUTED.color()))
                .append(Component.text(instance.getChunks().size(), Palette.SUCCESS.color()));
    }

}
