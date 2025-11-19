package club.tesseract.minestom.utils.permission.event;

import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record PlayerPermissionsRecalculateEvent(Player player, UserDataRecalculateEvent event) implements PlayerEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull UserDataRecalculateEvent event() {
        return event;
    }
}
