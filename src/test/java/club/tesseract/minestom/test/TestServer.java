package club.tesseract.minestom.test;

import club.tesseract.minestom.utils.command.CommandRegistry;
import club.tesseract.minestom.utils.entity.player.MinestomPlayer;
import club.tesseract.minestom.utils.instance.dimension.FullBrightDimension;
import club.tesseract.minestom.utils.permission.LuckpermsPermission;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;

public final class TestServer {

    static void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());
        FullBrightDimension.getInstance();
        CommandRegistry.registerAll();
        LuckpermsPermission perms = new LuckpermsPermission();

        InstanceContainer container = new DummyInstance();
        MinecraftServer.getInstanceManager().registerInstance(container);
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.getPlayer().setRespawnPoint(new Pos(0, 70, 0));
            event.setSpawningInstance(container);
        });

        MinecraftServer.getCommandManager().register(new DebugCommand());

        MinestomPlayer.register();
        MinecraftServer.getSchedulerManager().buildShutdownTask(perms::shutdown);
        server.start("0.0.0.0", 25565);
    }

}
