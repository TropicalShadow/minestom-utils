package club.tesseract.minestom.utils.instance.polar;

import club.tesseract.minestom.utils.entity.EntityData;
import club.tesseract.minestom.utils.entity.NoPhysicsEntity;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.hollowcube.polar.PolarWorldAccess;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.event.instance.InstanceRegisterEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;


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
        if(userData == null) return;
        byte FEATURE_VERSION = userData.read(NetworkBuffer.BYTE);
        if(FEATURE_VERSION != CURRENT_FEATURE_VERSION){
            log.warn("Polar Paper World Access: Unsupported feature version: {}", FEATURE_VERSION);
        }
        entities.addAll(readEntities(userData));

        final Instance instance = chunk.getInstance();
        if(instance.isRegistered()){
            instance.scheduleNextTick(this::registerEntities);
            return;
        }
        instance.eventNode().addListener(InstanceRegisterEvent.class, event -> {
            event.getInstance().scheduleNextTick(this::registerEntities);
        });

    }

    void registerEntities(@NotNull Instance instance){
        for(EntityData data : entities){
            entityCreator.apply(instance, data);
        }
        entities.clear();
    }

    List<EntityData> readEntities(NetworkBuffer userData){
        int entities = userData.read(NetworkBuffer.VAR_INT);
        if(entities == 0) return List.of();
        log.debug("Polar Paper World Access: {} entities", entities);
        List<EntityData> entityDataList = new ArrayList<>(entities);
        for(int i = 0; i < entities; i++){
            try {
                double x = userData.read(NetworkBuffer.DOUBLE);
                double y = userData.read(NetworkBuffer.DOUBLE);
                double z = userData.read(NetworkBuffer.DOUBLE);
                float yaw = userData.read(NetworkBuffer.FLOAT);
                float pitch = userData.read(NetworkBuffer.FLOAT);
                byte[] data = userData.read(NetworkBuffer.BYTE_ARRAY);
                EntityData entityData = new EntityData(x, y, z, yaw, pitch, data);
                entityDataList.add(entityData);
            }catch(Exception ex){
                log.warn("Polar Paper World Access: Failed to read entity", ex);
            }
        }
        return entityDataList;
    }

}
