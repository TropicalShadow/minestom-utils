package club.tesseract.minestom.utils.feature.listener;


import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;


public record ItemDropFeature() implements EventListener<ItemDropEvent> {

    private static final Duration ITEM_DECAY_DURATION = Duration.of(5, ChronoUnit.MINUTES);

    @Override
    public @NotNull Class<ItemDropEvent> eventType() {
        return ItemDropEvent.class;
    }

    @Override
    public @NotNull Result run(ItemDropEvent event) {
        Pos playerPosition = event.getPlayer().getPosition();
        ItemEntity entity = new ItemEntity(event.getItemStack());
        entity.setPickupDelay(40, TimeUnit.SERVER_TICK);
        entity.setInstance(event.getPlayer().getInstance(), playerPosition.add(0.0D, event.getPlayer().getEyeHeight() - 0.3f, 0.0D));
        entity.setVelocity(playerPosition.direction().mul(6.0D));
        entity.scheduleRemove(ITEM_DECAY_DURATION);
        return EventListener.Result.SUCCESS;
    }
}
