package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCommandEvent;
import net.minestom.server.event.player.PlayerGameModeRequestEvent;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class GameModeCommand extends Command {

    private static class ExactGameModeCommand extends Command {

        public ExactGameModeCommand(GameMode gameMode) {
            super(gameMode.name().toLowerCase(Locale.ROOT));

            EntityFinder defaultEntityFinder = new EntityFinder();
            defaultEntityFinder.setTargetSelector(EntityFinder.TargetSelector.SELF);
            var playerArgument = ArgumentType
                    .Entity("player")
                    .onlyPlayers(true)
                    .setDefaultValue(defaultEntityFinder);

            String formattedPermission = "minecraft.command.gamemode.%s".formatted(getName());
            Component failedSelf = Component.translatable("debug.creative_spectator.error").color(NamedTextColor.GRAY);

            addSyntax((sender, context) -> {
                EntityFinder playerFinder = context.get(playerArgument);
                List<Entity> entities = playerFinder.find(sender);

                if (!ExtraConditions.hasPermission("minecraft.command.gamemode").canUse(sender, null) &&
                        !ExtraConditions.hasPermission(formattedPermission).canUse(sender, null)) {
                    sender.sendMessage(failedSelf);
                    return;
                }

                Component gameModeComponent = Component.translatable("gameMode.%s".formatted(getName())).color(NamedTextColor.WHITE);
                Component successSelf = Component.translatable("commands.gamemode.success.self", gameModeComponent).color(NamedTextColor.GRAY);
                boolean collectiveSenderReply = entities.size() > 1;
                AtomicInteger successfullyChanges = new AtomicInteger(0);
                entities.forEach(entity -> {
                    if (!(entity instanceof Player player)) return;

                    if(player.setGameMode(gameMode)){
                        if (entity == sender) {
                            sender.sendMessage(successSelf);
                        } else {
                            player.sendMessage(
                                    Component.translatable("gameMode.changed")
                                            .arguments(gameModeComponent)
                                            .color(NamedTextColor.GRAY)
                            );

                            if(!collectiveSenderReply) {
                                sender.sendMessage(Component.translatable(
                                        "commands.gamemode.success.other",
                                        Objects.requireNonNullElse(
                                                player.getDisplayName(),
                                                player.getName()
                                        ),
                                        gameModeComponent));
                            }
                        }
                        successfullyChanges.incrementAndGet();
                    }else {
                        if(entity == sender) {
                            sender.sendMessage(failedSelf);
                        }
                    }
                });
                if(collectiveSenderReply){
                    sender.sendMessage(Component.translatable(
                            "commands.gamemode.success.other",
                            Component.text(successfullyChanges.get()).append(Component.translatable("entity.minecraft.player")),
                            gameModeComponent
                    ));
                }
            }, playerArgument);

        }

    }

    public GameModeCommand() {
        super("gamemode");
        MinecraftServer.getGlobalEventHandler().addListener(PlayerCommandEvent.class, event -> {
            if (!event.getCommand().contains(" "))
                switch (event.getCommand()) {
                    case "gmc", "gm1" -> event.setCommand("gamemode creative");
                    case "gms", "gm0" -> event.setCommand("gamemode survival");
                    case "gmsp", "gm3" -> event.setCommand("gamemode spectator");
                    case "gma", "gm2" -> event.setCommand("gamemode adventure");
                }
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerGameModeRequestEvent.class, event ->{
            Player player = event.getPlayer();
            GameMode gameMode = event.getRequestedGameMode();
            String gameModeName = gameMode.name().toLowerCase(Locale.ROOT);

            String formattedPermission = "minecraft.command.gamemode.%s".formatted(gameModeName);
            Component gameModeComponent = Component.translatable("gameMode.%s".formatted(gameModeName)).color(NamedTextColor.WHITE);
            Component successSelf = Component.translatable("commands.gamemode.success.self", gameModeComponent).color(NamedTextColor.GRAY);
            Component failedSelf = Component.translatable("debug.creative_spectator.error").color(NamedTextColor.GRAY);

            if (!ExtraConditions.hasPermission("minecraft.command.gamemode").canUse(player, null) &&
                    !ExtraConditions.hasPermission(formattedPermission).canUse(player, null)) {
                player.sendMessage(failedSelf);
                return;
            }

            if(player.setGameMode(gameMode)){
                player.sendMessage(successSelf);
            }else{
                player.sendMessage(failedSelf);
            }
        });

        for (GameMode gameMode: GameMode.values()) {
            addSubcommand(new ExactGameModeCommand(gameMode));
        }

    }

}