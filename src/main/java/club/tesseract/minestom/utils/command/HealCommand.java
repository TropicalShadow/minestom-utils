package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.GenericArguments;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

import java.util.List;

public class HealCommand extends Command {
    public HealCommand() {
        super("heal");

        setCondition(ExtraConditions.hasPermission("gamesdk.command.heal"));

        setDefaultExecutor((sender, context) ->{
            if(!(sender instanceof Player player)){
                sender.sendMessage("/heal <player>");
                return;
            }
            player.heal();
            player.sendMessage(Component.text("You have been healed!", NamedTextColor.GREEN));
        });

        addSyntax((sender, context) -> {
            List<Player> players = GenericArguments.playerArgs.getPlayers(sender, context);
            players.forEach(Player::heal);
            sender.sendMessage(Component.text("Healed %s player(s)".formatted(players.size()), NamedTextColor.GREEN));
        }, GenericArguments.playerArgs);

    }
}
