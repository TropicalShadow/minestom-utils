package club.tesseract.minestom.utils.event;

import lombok.Getter;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerChangeFlyEvent implements PlayerInstanceEvent, CancellableEvent {

    private final Player player;
    @Getter
    private final boolean newFlyState;

    private boolean cancelled;


    public PlayerChangeFlyEvent(@NotNull Player player, boolean newFlyState) {
        this.player = player;
        this.newFlyState = newFlyState;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

}
