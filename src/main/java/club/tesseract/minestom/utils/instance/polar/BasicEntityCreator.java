package club.tesseract.minestom.utils.instance.polar;

import club.tesseract.minestom.utils.entity.EntityData;
import club.tesseract.minestom.utils.entity.NoPhysicsEntity;
import lombok.extern.slf4j.Slf4j;
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
import net.minestom.server.instance.Instance;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;


@Slf4j
public class BasicEntityCreator implements EntityCreator{


    @Override
    public void apply(@NotNull Instance instance, @NotNull EntityData data) {
        try {
            EntityNBT entityNBT = EntityNBT.fromEntityData(data);
            EntityType entityType = entityNBT.getEntityType();
            if(entityType == null) return;
            UUID uniqueId = entityNBT.getUniqueId().orElse(UUID.randomUUID());
            OpenEntity entity = new OpenEntity(entityType, uniqueId);
            Pos location = entityNBT.getVec().withView(entityNBT.getDirection());
            boolean noGravity = entityNBT.nbt().getBoolean("NoGravity", false);
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
    }

    static class OpenEntity extends NoPhysicsEntity {

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

        static EntityNBT fromEntityData(EntityData data) throws IOException {
            return new EntityNBT(data.getEntityNBT());
        }

    }


}
