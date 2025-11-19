package club.tesseract.minestom.utils.permission;

import club.tesseract.minestom.utils.msic.PermissionHolder;
import club.tesseract.minestom.utils.permission.config.HoconConfigurationAdapter;
import club.tesseract.minestom.utils.permission.event.PlayerPermissionsRecalculateEvent;
import me.lucko.luckperms.common.config.generic.adapter.EnvironmentVariableConfigAdapter;
import me.lucko.luckperms.common.config.generic.adapter.MultiConfigurationAdapter;
import me.lucko.luckperms.common.config.generic.adapter.SystemPropertyConfigAdapter;
import me.lucko.luckperms.minestom.CommandRegistry;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import me.lucko.luckperms.minestom.LuckPermsMinestom;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class LuckpermsPermission {

    private static final EventNode<@NotNull PlayerEvent> eventNode = EventNode.type("permission-events", EventFilter.PLAYER);

    final LuckPerms luckPerms;
    LPMinestomPlugin luckPermsPlugin;

    public LuckpermsPermission() {
        Path luckPermsPath = Path.of("luckperms");
        luckPerms = LuckPermsMinestom.builder(luckPermsPath)
                .commandRegistry(CommandRegistry.minestom())
                .configurationAdapter(plugin -> {
                    MultiConfigurationAdapter config = new MultiConfigurationAdapter(plugin, new SystemPropertyConfigAdapter(plugin),  new EnvironmentVariableConfigAdapter(plugin), new HoconConfigurationAdapter(plugin));
                    luckPermsPlugin = plugin;
                    return config;
                })
                .enable();
        luckPerms.getEventBus().subscribe(UserDataRecalculateEvent.class, this::recalculatePlayerEvent);
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, this::playerDataLoad);
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    public void shutdown() {
        this.luckPermsPlugin.disable();
        MinecraftServer.getGlobalEventHandler().removeChild(eventNode);
    }

    void playerDataLoad(AsyncPlayerConfigurationEvent event){
        if(!event.isFirstConfig())return;
        if(!(event.getPlayer() instanceof PermissionHolder permissionHolder)) return;
        permissionHolder.onPermissionUserLoad();
    }

    void recalculatePlayerEvent(UserDataRecalculateEvent event) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(() ->{
            Optional<Player> playerOptional = getOnlinePlayerFromLuckPermsUser(event.getUser());

            playerOptional.ifPresent((player) ->{
                if(!(player instanceof PermissionHolder permissionHolder)) {
                    return;
                }
                permissionHolder.onPermissionRecalculate();
                PlayerPermissionsRecalculateEvent recalculateEvent = new PlayerPermissionsRecalculateEvent(player, event);
                EventDispatcher.call(recalculateEvent);
            } );
        });
    }

    private Optional<Player> getOnlinePlayerFromLuckPermsUser(User user) {
        UUID playerId = user.getUniqueId();
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        return Optional.ofNullable(player);
    }


    public static Optional<LuckPerms> getLuckPerms() {
        try {
            return Optional.of(LuckPermsProvider.get());
        }catch (IllegalStateException ex){
            return Optional.empty();
        }
    }
}
