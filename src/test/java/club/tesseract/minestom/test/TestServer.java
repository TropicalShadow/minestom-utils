package club.tesseract.minestom.test;

import club.tesseract.minestom.test.instance.PolarInstance;
import club.tesseract.minestom.utils.command.CommandRegistry;
import club.tesseract.minestom.utils.entity.InteractionEntity;
import club.tesseract.minestom.utils.entity.MarkerData;
import club.tesseract.minestom.utils.entity.player.MinestomPlayer;
import club.tesseract.minestom.utils.instance.dimension.FullBrightDimension;
import club.tesseract.minestom.utils.permission.LuckpermsPermission;
import fr.ghostrider584.axiom.AxiomMinestom;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestServer {

    private static final Logger log = LoggerFactory.getLogger(TestServer.class);

    static void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());
        FullBrightDimension.getInstance();
        CommandRegistry.registerAll();
        AxiomMinestom.initialize();
        LuckpermsPermission perms = new LuckpermsPermission();

        InstanceContainer container = new PolarInstance();
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.getPlayer().setRespawnPoint(new Pos(0, 70, 0));
            event.setSpawningInstance(container);
            Area.Cuboid cuboid = Area.cuboid(Vec.ZERO, new Vec(1,2,1));
            InteractionEntity entity = new InteractionEntity(cuboid);
            entity.setInstance(container, new Pos(0, 69, 1));
        }).addListener(AddEntityToInstanceEvent.class, event -> {
            if(event.getEntity().getEntityType() != EntityType.MARKER) return;

            event.getEntity().scheduleNextTick(ent ->{
                MarkerData data = MarkerData.createFrom(ent);
                if(data.name() == null)return;
                log.info("Marker {} created", data.name());
                data.createDisplay(event.getInstance());
            });
        });


        MinecraftServer.getInstanceManager().registerInstance(container);

        MinecraftServer.getCommandManager().register(new DebugCommand());

        MinestomPlayer.register();
        MinecraftServer.getSchedulerManager().buildShutdownTask(perms::shutdown);
        server.start("0.0.0.0", 25565);
    }

}
