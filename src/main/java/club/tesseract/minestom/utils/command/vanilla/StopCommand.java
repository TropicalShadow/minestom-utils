
package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.CommandCategory;
import club.tesseract.minestom.utils.command.CommandMetadata;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

@CommandMetadata(
        categories = {CommandCategory.ADMIN, CommandCategory.SERVER, CommandCategory.VANILLA},
        description = "Stops the server"
)
public class StopCommand extends Command {

    private static final Component STOP_MESSAGE = Component.text("Stopping the server...", NamedTextColor.YELLOW);

    public StopCommand() {
        super("stop");

        setCondition(ExtraConditions.orOp(ExtraConditions.hasPermission("minecraft.command.stop")));

        setDefaultExecutor((_, _) -> {
            PacketGroupingAudience.of(MinecraftServer.getConnectionManager().getOnlinePlayers()).sendMessage(STOP_MESSAGE);
            Thread.startVirtualThread(MinecraftServer::stopCleanly);
        });

    }
}
