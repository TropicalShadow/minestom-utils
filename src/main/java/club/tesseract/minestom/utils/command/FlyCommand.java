package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.GenericArguments;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import club.tesseract.minestom.utils.event.PlayerChangeFlyEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;

public class FlyCommand extends Command {

    public FlyCommand() {
        super("fly");

        setCondition(ExtraConditions.hasPermission("gamesdk.fly"));

        setDefaultExecutor((sender, context) ->{
            if(!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("You must be a player to use this command!", NamedTextColor.RED));
                return;
            }

            boolean newFlyState = !player.isAllowFlying();
            PlayerChangeFlyEvent flyEvent = new PlayerChangeFlyEvent(player, newFlyState);
            EventDispatcher.call(flyEvent);
            if (flyEvent.isCancelled()) {
                player.sendMessage(Component.text("Flight change cancelled!", NamedTextColor.RED));
                return;
            }

            player.setAllowFlying(newFlyState);
            player.setFlying(newFlyState);
            player.sendMessage(Component.text("You have " + (newFlyState ? "Activated" : "Deactivated") + " flight!", newFlyState ? NamedTextColor.GREEN : NamedTextColor.RED));
        });

        addSyntax((sender, context) ->{
            Player player = context.get(GenericArguments.playerArgs).findFirstPlayer(sender);
            if(player == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return;
            }

            boolean newFlyState = !player.isAllowFlying();
            PlayerChangeFlyEvent flyEvent = new PlayerChangeFlyEvent(player, newFlyState);
            EventDispatcher.call(flyEvent);
            if (flyEvent.isCancelled()) {
                sender.sendMessage(Component.text("Flight change for " + player.getUsername() + " cancelled!", NamedTextColor.RED));
                return;
            }

            player.setAllowFlying(newFlyState);
            player.setFlying(newFlyState);
            sender.sendMessage(Component.text(player.getUsername() + " has " + (newFlyState ? "Activated" : "Deactivated") + " flight!", newFlyState ? NamedTextColor.GREEN : NamedTextColor.RED));
        }, GenericArguments.playerArgs);

    }
}
