package club.tesseract.minestom.utils.permission.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record PlayerPermissionsRecalculateEvent(Player player) implements PlayerEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

}
