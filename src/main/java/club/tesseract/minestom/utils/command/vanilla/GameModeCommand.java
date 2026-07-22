package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.CommandCategory;
import club.tesseract.minestom.utils.command.CommandMetadata;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerGameModeRequestEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@CommandMetadata(
        categories = {CommandCategory.ADMIN, CommandCategory.PLAYER, CommandCategory.VANILLA},
        description = "Changes your gamemode"
)
public class GameModeCommand extends Command {

    private final static EventNode<PlayerEvent> eventNode = EventNode.type("gamemode-command-events", EventFilter.PLAYER);

    static{
        eventNode.addListener(PlayerGameModeRequestEvent.class, event ->{
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
    }

    public static class ShortcutGameModeCommand extends Command {

        public ShortcutGameModeCommand(String name, GameMode gameMode) {
            super(name);
            setup(gameMode);
        }

        private void setup(GameMode gameMode) {
            String gameModeName = gameMode.name().toLowerCase(Locale.ROOT);
            String formattedPermission = "minecraft.command.gamemode.%s".formatted(gameModeName);

            setCondition(ExtraConditions.orOp(
                    ExtraConditions.hasPermission("minecraft.command.gamemode"),
                    ExtraConditions.hasPermission(formattedPermission)
            ));

            setDefaultExecutor((sender, context) -> {
                if (!(sender instanceof Player player)) return;

                Component gameModeComponent = Component.translatable("gameMode.%s".formatted(gameModeName)).color(NamedTextColor.WHITE);
                Component successSelf = Component.translatable("commands.gamemode.success.self", gameModeComponent).color(NamedTextColor.GRAY);
                Component failedSelf = Component.translatable("debug.creative_spectator.error").color(NamedTextColor.GRAY);

                if (player.setGameMode(gameMode)) {
                    player.sendMessage(successSelf);
                } else {
                    player.sendMessage(failedSelf);
                }
            });
        }
    }

    public static List<Command> getShortcutCommands() {
        return List.of(
                new ShortcutGameModeCommand("gms", GameMode.SURVIVAL),
                new ShortcutGameModeCommand("gm0", GameMode.SURVIVAL),
                new ShortcutGameModeCommand("gmc", GameMode.CREATIVE),
                new ShortcutGameModeCommand("gm1", GameMode.CREATIVE),
                new ShortcutGameModeCommand("gma", GameMode.ADVENTURE),
                new ShortcutGameModeCommand("gm2", GameMode.ADVENTURE),
                new ShortcutGameModeCommand("gmsp", GameMode.SPECTATOR),
                new ShortcutGameModeCommand("gm3", GameMode.SPECTATOR)
        );
    }

    private static class ExactGameModeCommand extends Command {

        public ExactGameModeCommand(GameMode gameMode) {
            super(gameMode.name().toLowerCase(Locale.ROOT));

            String formattedPermission = "minecraft.command.gamemode.%s".formatted(getName());

            setCondition(ExtraConditions.orOp(
                    ExtraConditions.hasPermission("minecraft.command.gamemode"),
                    ExtraConditions.hasPermission(formattedPermission)
            ));

            EntityFinder defaultEntityFinder = new EntityFinder();
            defaultEntityFinder.setTargetSelector(EntityFinder.TargetSelector.SELF);
            var playerArgument = ArgumentType
                    .Entity("player")
                    .onlyPlayers(true)
                    .setDefaultValue(defaultEntityFinder);

            addSyntax((sender, context) -> {
                EntityFinder playerFinder = context.get(playerArgument);
                applyGameMode(sender, playerFinder.find(sender), gameMode);
            }, playerArgument);

        }

    }

    private static void applyGameMode(CommandSender sender, List<Entity> entities, GameMode gameMode) {
        String gameModeName = gameMode.name().toLowerCase(Locale.ROOT);
        Component gameModeComponent = Component.translatable("gameMode.%s".formatted(gameModeName)).color(NamedTextColor.WHITE);
        Component successSelf = Component.translatable("commands.gamemode.success.self", gameModeComponent).color(NamedTextColor.GRAY);
        Component failedSelf = Component.translatable("debug.creative_spectator.error").color(NamedTextColor.GRAY);

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
    }

    private static GameMode gameModeFromAlias(String alias) {
        return switch (alias.toLowerCase(Locale.ROOT)) {
            case "s", "survival", "0" -> GameMode.SURVIVAL;
            case "c", "creative", "1" -> GameMode.CREATIVE;
            case "a", "adventure", "2" -> GameMode.ADVENTURE;
            case "sp", "spectator", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    public GameModeCommand() {
        super("gamemode");

        setCondition(ExtraConditions.orOp(ExtraConditions.hasPermission("minecraft.command.gamemode")));

        for (GameMode gameMode : GameMode.values()) {
            addSubcommand(new ExactGameModeCommand(gameMode));
        }

        var modeArgument = ArgumentType.Word("mode")
                .from("s", "survival", "0", "c", "creative", "1", "a", "adventure", "2", "sp", "spectator", "3");

        EntityFinder defaultEntityFinder = new EntityFinder();
        defaultEntityFinder.setTargetSelector(EntityFinder.TargetSelector.SELF);
        var playerArgument = ArgumentType
                .Entity("player")
                .onlyPlayers(true)
                .setDefaultValue(defaultEntityFinder);

        addSyntax((sender, context) -> {
            GameMode gameMode = gameModeFromAlias(context.get(modeArgument));
            if (gameMode == null) return;

            String formattedPermission = "minecraft.command.gamemode.%s".formatted(gameMode.name().toLowerCase(Locale.ROOT));
            if (sender instanceof Player player &&
                    !ExtraConditions.hasPermission("minecraft.command.gamemode").canUse(player, null) &&
                    !ExtraConditions.hasPermission(formattedPermission).canUse(player, null)) {
                sender.sendMessage(Component.translatable("debug.creative_spectator.error").color(NamedTextColor.GRAY));
                return;
            }

            applyGameMode(sender, context.get(playerArgument).find(sender), gameMode);
        }, modeArgument, playerArgument);

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

}