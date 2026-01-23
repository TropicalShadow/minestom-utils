package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.GenericArguments;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandResult;
import net.minestom.server.command.builder.arguments.ArgumentCommand;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.Optional;

@CommandMetadata(
        categories = {CommandCategory.ADMIN},
        description = "Executes a command as another player"
)
public final class SudoCommand extends Command {

    private static final ArgumentCommand SUDO_COMMAND = ArgumentType.Command("command");

    public SudoCommand() {
        super("sudo");

        setCondition(ExtraConditions.hasPermission("gamesdk.command.sudo"));

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /sudo <player> <command>"));

        addSyntax((sender, context) -> {
            CommandResult cmd =  context.get(SUDO_COMMAND);
            Optional<Player> player = GenericArguments.playerArgs.getPlayer(sender, context);
            if(player.isEmpty()) return;

            MinecraftServer.getCommandManager().execute(player.get(), cmd.getInput());
            sender.sendMessage(Component.text("Executed command as %s".formatted(player.get().getUsername()), NamedTextColor.GREEN));
        }, GenericArguments.playerArgs, SUDO_COMMAND);
    }
}
