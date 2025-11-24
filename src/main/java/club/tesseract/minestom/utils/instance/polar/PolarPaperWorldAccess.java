package club.tesseract.minestom.utils.instance.polar;

import club.tesseract.minestom.utils.entity.NoPhysicsEntity;
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

    @Override
    public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer userData) {
        if(userData == null) return;
        byte FEATURE_VERSION = userData.read(NetworkBuffer.BYTE);
        if(FEATURE_VERSION != CURRENT_FEATURE_VERSION){
            log.warn("Polar Paper World Access: Unsupported feature version: {}", FEATURE_VERSION);
        }
        entities.addAll(readEntities(chunk.getInstance(), userData));
    }


    List<EntityData> readEntities(Instance instance, NetworkBuffer userData){
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
                try {
                    EntityNBT entityNBT = entityData.getEntityNBTObject();
                    EntityType entityType = entityNBT.getEntityType();
                    if(entityType == null) continue;
                    UUID uniqueId = entityNBT.getUniqueId().orElse(UUID.randomUUID());
                    OpenEntity entity = new OpenEntity(entityType, uniqueId);
                    Pos location = entityNBT.getVec().withView(entityNBT.getDirection());
                    boolean noGravity = entityNBT.nbt.getBoolean("NoGravity", false);
                    if(noGravity) {
                        entity.setNoGravity(true);
                    }
                    entityNBT.getCustomName().ifPresent(customName -> entity.set(DataComponents.CUSTOM_NAME, customName));
                    boolean customNameVisible = entityNBT.CustomNameVisible();
                    entity.setCustomNameVisible(customNameVisible);


                    if(entityType == EntityType.MARKER && entityNBT.nbt().contains("data")){
                        entity.set(DataComponents.CUSTOM_DATA, new CustomData(entityNBT.nbt().getCompound("data")));
                    }

                    {
                        Map<String, MetadataDef.Entry<?>> metadataEntryMap = getMetadataEntryMap();
                        entityNBT.nbt().keySet().forEach(key -> {
                            String upperKey = key.toLowerCase(Locale.ROOT);
                            MetadataDef.Entry<?> entry = metadataEntryMap.get(upperKey);
                            if(entry == null)return;

                        });
                    }

                    if(instance.isRegistered()) {
                        entity.setInstance(instance, location);
                    }else{
                        instance.eventNode().addListener(InstanceRegisterEvent.class, event -> {
                            entity.setInstance(event.getInstance(), location);
                        });
                    }
                    entityNBT.nbt.keySet().forEach(key -> {
                        log.debug("Polar Paper World Access: Entity NBT: {} = {}", key, entityNBT.nbt.get(key));
                    });
                } catch (IOException ex) {
                    log.warn("Polar Paper World Access: Failed to read entity NBT", ex);
                }
            }catch(Exception ex){
                log.warn("Polar Paper World Access: Failed to read entity", ex);
            }
        }
        return entityDataList;
    }

    public static class OpenEntity extends NoPhysicsEntity {

        public OpenEntity(EntityType entityType, UUID uuid) {
            super(entityType, uuid);

        }

        public <T> void setEntry(MetadataDef.Entry<@NotNull T> entry, T value){
            this.metadata.set(entry, value);
        }

    }

    static Map<String, MetadataDef.Entry<?>> getMetadataEntryMap(){
        Class<?>[] classes = MetadataDef.class.getDeclaredClasses();
        Map<String, MetadataDef.Entry<?>> map = new HashMap<>();
        for (Class<?> aClass : classes) {
            Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                try {
                    MetadataDef.Entry<?> entry = (MetadataDef.Entry<?>) field.get(null);
                    map.put(field.getName(), entry);
                }catch (IllegalAccessException e){
                    log.warn("Failed to access metadata entry {}", field.getName());
                }
            }
        }
        return map;
    }



    record EntityNBT(CompoundBinaryTag nbt){

        Optional<UUID> getUniqueId(){
            BinaryTag tag = nbt.get("uuid");
            if(!(tag instanceof IntArrayBinaryTag intArrayBinaryTag)) return Optional.empty();
            return Optional.of(UUIDUtils.fromNbt(intArrayBinaryTag));
        }

        Optional<Component> getCustomName(){
            BinaryTag input = nbt.get("CustomName");
            if(input == null) return Optional.empty();
            final Transcoder<@NotNull BinaryTag> coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
            return Optional.ofNullable(Codec.COMPONENT.decode(coder, input).orElse(null));
        }

        boolean CustomNameVisible(){
            return nbt.getBoolean("CustomNameVisible", false);
        }

        EntityType getEntityType(){
            return EntityType.fromKey(Key.key(nbt.getString("id", EntityType.SMALL_FIREBALL.key().asString())));
        }

        Pos getVec(){
            ListBinaryTag tagList = nbt.getList("Pos", BinaryTagTypes.DOUBLE);
            double x = tagList.getDouble(0);
            double y = tagList.getDouble(1);
            double z = tagList.getDouble(2);
            return new Pos(x, y, z);
        }

        Pos getDirection(){
            ListBinaryTag tagList = nbt.getList("Rotation", BinaryTagTypes.FLOAT);
            Pos pos = Pos.ZERO;
            pos = pos.withYaw(tagList.getFloat(0));
            pos = pos.withPitch(tagList.getFloat(1));
            return pos;
        }

    }

    record EntityData(double x, double y, double z, float yaw, float pitch, byte[] data){

        public CompoundBinaryTag getEntityNBT() throws IOException {
            if(data.length == 0) return CompoundBinaryTag.empty();
            return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(data));
        }

        public EntityNBT getEntityNBTObject() throws IOException {
            return new EntityNBT(getEntityNBT());
        }

    }
}
