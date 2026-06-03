package club.tesseract.minestom.test;

import club.tesseract.minestom.test.instance.DummyInstance;
import club.tesseract.minestom.utils.command.CommandRegistry;
import club.tesseract.minestom.utils.entity.InteractionEntity;
import club.tesseract.minestom.utils.entity.MarkerData;
import club.tesseract.minestom.utils.entity.npc.MannequinNPC;
import club.tesseract.minestom.utils.entity.npc.NPC;
import club.tesseract.minestom.utils.entity.player.MinestomPlayer;
import club.tesseract.minestom.utils.instance.dimension.FullBrightDimension;
import club.tesseract.minestom.utils.metrics.SparkManager;
import club.tesseract.minestom.utils.misc.lang.LangUtils;
import club.tesseract.minestom.utils.misc.lang.MiniMessageHelper;
import club.tesseract.minestom.utils.permission.PermissionHolder;
import club.tesseract.minestom.utils.permission.lp.LuckpermsPermission;
import club.tesseract.minestom.utils.permission.lp.LuckpermsPermissionHolder;
import fr.ghostrider584.axiom.AxiomMinestom;
import net.kyori.adventure.text.Component;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.InstanceRegisterEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

public final class TestServer {

    private static final Logger log = LoggerFactory.getLogger(TestServer.class);
    private static boolean isLuckpermissionWorking = false;

    static void main() {
        final MinecraftServer server = MinecraftServer.init(new Auth.Online());
        FullBrightDimension.getInstance();
        CommandRegistry.registerAll();
        AxiomMinestom.initialize();
        final SparkManager spark = new SparkManager();
        try {
            final LuckpermsPermission perms = new LuckpermsPermission();
            MinecraftServer.getSchedulerManager().buildShutdownTask(perms::shutdown);
            isLuckpermissionWorking = true;
        } catch (NoClassDefFoundError e) {
            log.error("LuckPerms failed to initialize, permissions will not work", e);
        }
        LangUtils.registerLang("demo", "demo.demo", Locale.ENGLISH);

        InstanceContainer container = new DummyInstance();
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.getPlayer().setRespawnPoint(new Pos(0, 70, 0));
            event.setSpawningInstance(container);
            Area.Cuboid cuboid = Area.cuboid(Vec.ZERO, new Vec(1,2,1));
            InteractionEntity entity = new InteractionEntity(cuboid);
            entity.setInstance(container, new Pos(0, 69, 5));
        }).addListener(AddEntityToInstanceEvent.class, event -> {
            if(event.getEntity().getEntityType() != EntityType.MARKER) return;

            event.getEntity().scheduleNextTick(ent ->{
                MarkerData data = MarkerData.createFrom(ent);
                if(data.name() == null)return;
                log.info("Marker {} created", data.name());
                data.createDisplay(event.getInstance());
            });
        }).addListener(PlayerSpawnEvent.class, event -> {
            event.getPlayer().sendMessage(MiniMessageHelper.toComponent("<gold>hello, %s", "world"));
            event.getPlayer().sendMessage(Component.translatable("demo.welcome").arguments(Objects.requireNonNullElse(event.getPlayer().getDisplayName(), Component.text(event.getPlayer().getUsername()))));
            if (event.getPlayer() instanceof PermissionHolder permissionHolder) {
                permissionHolder.onPermissionUserLoad();
                permissionHolder.setPermission("*", true);
                event.getPlayer().setPermissionLevel(4);
                event.getPlayer().refreshCommands();
                event.getPlayer().sendMessage(Component.text("You have been forced op, max permissions & operator level"));
            }
        }).addListener(PlayerDisconnectEvent.class, event -> {
            Component goodbye = Component.translatable("demo.goodbye").arguments(Objects.requireNonNullElse(event.getPlayer().getDisplayName(), Component.text(event.getPlayer().getUsername())));
            PacketGroupingAudience.of(MinecraftServer.getConnectionManager().getOnlinePlayers()).sendMessage(goodbye);
        });


        NPC funnyNPC = new MannequinNPC(null, Component.text("Steve"));
        funnyNPC.setAttack((event ) ->{
            event.interacted().sendMessage(Component.text("You attacked the npc!"));
        });
        funnyNPC.setInteract((event)->{
            event.interacted().sendMessage(Component.text("You interacted with the npc!"));
        });

        container.eventNode().addListener(InstanceRegisterEvent.class, _ -> funnyNPC.setInstance(container, new Vec(0, 70, 0)));

        MinecraftServer.getInstanceManager().registerInstance(container);

        MinecraftServer.getCommandManager().register(new DebugCommand());
        MinecraftServer.getCommandManager().register(new ParticleCreatorCommand());

        if (isLuckpermissionWorking) {
            MinestomPlayer.register(LuckpermsPermissionHolder::new);
        } else {
            MinestomPlayer.register();
        }
        spark.enable();

        MinecraftServer.getSchedulerManager().buildShutdownTask(spark::shutdown);
        server.start("0.0.0.0", 25565);
    }

}
