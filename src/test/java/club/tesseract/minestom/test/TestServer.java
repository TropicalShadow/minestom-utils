package club.tesseract.minestom.test;

import club.tesseract.minestom.test.instance.PolarInstance;
import club.tesseract.minestom.utils.command.CommandRegistry;
import club.tesseract.minestom.utils.entity.player.MinestomPlayer;
import club.tesseract.minestom.utils.instance.dimension.FullBrightDimension;
import club.tesseract.minestom.utils.permission.LuckpermsPermission;
import lombok.extern.slf4j.Slf4j;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class TestServer {

    private static final Logger log = LoggerFactory.getLogger(TestServer.class);

    static void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());
        FullBrightDimension.getInstance();
        CommandRegistry.registerAll();
        LuckpermsPermission perms = new LuckpermsPermission();

        InstanceContainer container = new PolarInstance();
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
