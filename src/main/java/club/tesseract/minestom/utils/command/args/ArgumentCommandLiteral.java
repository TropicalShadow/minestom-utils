package club.tesseract.minestom.utils.command.args;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandDispatcher;
import net.minestom.server.command.builder.CommandResult;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.command.builder.arguments.ArgumentCommand.INVALID_COMMAND_ERROR;

public class ArgumentCommandLiteral extends Argument<@NotNull CommandResult> {

    public ArgumentCommandLiteral(String id) {
        super(id, true, true);


        setSuggestionCallback((sender, context, suggestion) ->{
           MinecraftServer.getCommandManager().getDispatcher().getCommands().forEach(command -> {
               suggestion.addEntry(new SuggestionEntry(command.getName()));
           });
        });
    }

    @Override
    public CommandResult parse(@NotNull CommandSender sender, String input) throws ArgumentSyntaxException {
        final String commandString = input.trim();
        CommandDispatcher dispatcher = MinecraftServer.getCommandManager().getDispatcher();
        CommandResult result = dispatcher.parse(sender, commandString);

        if (result.getType() != CommandResult.Type.SUCCESS)
            throw new ArgumentSyntaxException("Invalid command", input, INVALID_COMMAND_ERROR);

        return result;
    }

    @Override
    public ArgumentParserType parser() {
        return null;
    }
}
