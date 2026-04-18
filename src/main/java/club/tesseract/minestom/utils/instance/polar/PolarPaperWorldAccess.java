package club.tesseract.minestom.utils.instance.polar;

import club.tesseract.minestom.utils.entity.EntityData;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.event.instance.InstanceRegisterEvent;
import net.minestom.server.event.instance.InstanceUnregisterEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@Slf4j
public class PolarPaperWorldAccess implements PolarWorldAccess {

    private static final byte CURRENT_FEATURE_VERSION = 2;
    private final List<EntityData> entities = new ArrayList<>();
    @Setter
    private EntityCreator entityCreator;

    public PolarPaperWorldAccess(EntityCreator entityCreator) {
        this.entityCreator = entityCreator;
    }

    public PolarPaperWorldAccess() {
        this(new BasicEntityCreator());
    }


    @Override
    public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer userData) {
        if (userData == null) return;
        byte FEATURE_VERSION = userData.read(NetworkBuffer.BYTE);
        if (FEATURE_VERSION != CURRENT_FEATURE_VERSION) {
            log.warn("Polar Paper World Access: Unsupported feature version: {}", FEATURE_VERSION);
        }

        List<EntityData> entities = readEntities(userData);
        final Instance instance = chunk.getInstance();
        if (instance.isRegistered()) {
            instance.scheduleNextTick(registerEntities(entities));
            return;
        }
        instance.eventNode().addListener(InstanceRegisterEvent.class, event -> {
                    event.getInstance().scheduleNextTick(registerEntities(entities));
                })
                .addListener(InstanceUnregisterEvent.class, _ -> this.entities.clear());
    }

    Consumer<Instance> registerEntities(List<EntityData> entities) {
        return instance -> {
            for (EntityData data : entities) {
                entityCreator.apply(instance, data);
            }
            this.entities.addAll(entities);
        };
    }

    List<EntityData> readEntities(NetworkBuffer userData) {
        int entities = userData.read(NetworkBuffer.VAR_INT);
        if (entities == 0) return List.of();
        log.debug("Polar Paper World Access: {} entities", entities);
        List<EntityData> entityDataList = new ArrayList<>(entities);
        for (int i = 0; i < entities; i++) {
            try {
                double x = userData.read(NetworkBuffer.DOUBLE);
                double y = userData.read(NetworkBuffer.DOUBLE);
                double z = userData.read(NetworkBuffer.DOUBLE);
                float yaw = userData.read(NetworkBuffer.FLOAT);
                float pitch = userData.read(NetworkBuffer.FLOAT);
                byte[] data = userData.read(NetworkBuffer.BYTE_ARRAY);
                EntityData entityData = new EntityData(x, y, z, yaw, pitch, data);
                entityDataList.add(entityData);
            } catch (Exception ex) {
                log.warn("Polar Paper World Access: Failed to read entity", ex);
            }
        }
        return entityDataList;
    }

}
