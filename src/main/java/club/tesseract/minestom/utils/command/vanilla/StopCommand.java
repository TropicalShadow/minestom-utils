
package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.CommandCategory;
import club.tesseract.minestom.utils.command.CommandMetadata;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

@CommandMetadata(
        categories = {CommandCategory.ADMIN, CommandCategory.SERVER, CommandCategory.VANILLA},
        description = "Stops the server"
)
public class StopCommand extends Command {
    public StopCommand() {
        super("stop");

        setCondition(ExtraConditions.hasPermission("minecraft.command.stop"));

        setDefaultExecutor((sender, _) -> {
            if(sender instanceof Player player){
                if(player.getPermissionLevel() < 4){
                    player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return;
                }
            }

            sender.sendMessage(Component.text("Stopping the server...", NamedTextColor.YELLOW));
            Thread.startVirtualThread(MinecraftServer::stopCleanly);
        });

    }
}
