package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.PlayerArgument;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class StatsCommand extends Command {
    public StatsCommand() {
        super("stats");

        setCondition(ExtraConditions.hasPermission("gamesdk.command.stats"));

        addSubcommand(new InstanceStatsCommand());
        addSubcommand(new PlayerStatsCommand());
    }


    private static final class PlayerStatsCommand extends Command {
        private static final PlayerArgument PLAYER_ARGUMENT = new PlayerArgument("target", true);

        public PlayerStatsCommand() {
            super("player");

            ConnectionManager connectionManager = MinecraftServer.getConnectionManager();

            setDefaultExecutor((sender, context) -> {
                Component message = Component.text("Players Online (", NamedTextColor.GRAY)
                        .append(Component.text(connectionManager.getOnlinePlayers().size(), NamedTextColor.GREEN))
                        .append(Component.text(") ", NamedTextColor.GRAY));

                Player[] players = connectionManager.getOnlinePlayers().stream().limit(10).toArray(Player[]::new);
                if (players.length > 0) {
                    message = message.append(Component.newline());
                    boolean first = true;
                    for (Player player : players) {
                        if (!first) {
                            message = message.append(Component.text(", ", NamedTextColor.GRAY));
                        }
                        first = false;
                        message = message.append(Component.text(player.getUsername(), NamedTextColor.GREEN).hoverEvent(getPlayerStats(player))
                                .clickEvent(ClickEvent.copyToClipboard(player.getUuid().toString())));

                    }
                } else {
                    message = message.append(Component.newline())
                            .append(Component.text("No players online", NamedTextColor.RED));
                }

                sender.sendMessage(message);
            });


            addSyntax((sender, context) -> {
                Optional<Player> targetPlayer = PLAYER_ARGUMENT.getPlayer(sender, context);
                if (targetPlayer.isEmpty()) {
                    sender.sendMessage(Component.text("Please specify a player", NamedTextColor.RED));
                    return;
                }

                sender.sendMessage(getPlayerStats(targetPlayer.get()));
            }, PLAYER_ARGUMENT);
        }

        Component getPlayerStats(Player player) {
            return Component.text("Player: ", NamedTextColor.GRAY)
                    .append(Component.text(player.getUsername(), NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(player.getUsername())))
                    .append(Component.newline())
                    .append(Component.text("UUID: ", NamedTextColor.GRAY))
                    .append(Component.text(player.getUuid().toString(), NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(player.getUuid().toString())))
                    .append(Component.newline())
                    .append(Component.text("Instance: ", NamedTextColor.GRAY))
                    .append(Component.text(player.getInstance().getUuid().toString(), NamedTextColor.GREEN).hoverEvent(getInstanceStats(player.getInstance())).clickEvent(ClickEvent.copyToClipboard(player.getInstance().getUuid().toString())));
        }
    }

    private static final class InstanceStatsCommand extends Command {
        public static final ArgumentUUID INSTANCE_UUID = ArgumentType.UUID("target");

        public InstanceStatsCommand() {
            super("instance");

            InstanceManager instanceManager = MinecraftServer.getInstanceManager();


            setDefaultExecutor((sender, context) -> {
                Component message = Component.text("Instance (", NamedTextColor.GRAY)
                        .append(Component.text(instanceManager.getInstances().size(), NamedTextColor.GREEN))
                        .append(Component.text(") ", NamedTextColor.GRAY))
                        ;

                if (instanceManager.getInstances().size() <= 10) {
                    message = message.append(Component.newline());
                    boolean first = true;
                    for (Instance instance : instanceManager.getInstances()) {
                        if (!first) {
                            message = message.append(Component.text(", ", NamedTextColor.GRAY));
                        }
                        first = false;
                        message = message.append(Component.text(instance.getUuid().toString(), NamedTextColor.GREEN).hoverEvent(getInstanceStats(instance)).clickEvent(ClickEvent.copyToClipboard(instance.getUuid().toString())));
                    }
                } else {
                    message = message.append(Component.newline())
                            .append(Component.text("Too many instances to display", NamedTextColor.RED));
                }

                sender.sendMessage(message);
            });

            addSyntax((sender, context) -> {
                Player player = (Player) sender;
                UUID instanceUuid;
                try {
                     instanceUuid = context.get(INSTANCE_UUID);
                }catch (NullPointerException ex){
                    player.sendMessage(Component.text("Please specify an instance UUID", NamedTextColor.RED));
                    return;
                }

                Instance instance = instanceManager.getInstance(instanceUuid);
                if (instance == null) {
                    player.sendMessage(Component.text("Instance not found", NamedTextColor.RED));
                    return;
                }

                player.sendMessage(getInstanceStats(instance));
            }, INSTANCE_UUID);

        }

    }

    static Component getInstanceStats(Instance instance) {
        return Component.text("Instance UUID: ", NamedTextColor.GRAY)
                .append(Component.text(instance.getUuid().toString(), NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Players: ", NamedTextColor.GRAY))
                .append(Component.text(instance.getPlayers().size(), NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Entities: ", NamedTextColor.GRAY))
                .append(Component.text(instance.getEntities().stream().filter(ent -> !(ent instanceof Player)).count(), NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Chunks Loaded: ", NamedTextColor.GRAY))
                .append(Component.text(instance.getChunks().size(), NamedTextColor.GREEN));
    }

}
