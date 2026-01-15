package club.tesseract.minestom.utils.instance.polar;

import club.tesseract.minestom.utils.entity.EntityData;
import club.tesseract.minestom.utils.entity.NoPhysicsEntity;
import lombok.extern.slf4j.Slf4j;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.event.instance.InstanceRegisterEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.component.CustomData;
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
            Pos location = entityNBT.getPosition().withView(entityNBT.getDirection());
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
            entityNBT.nbt().keySet().forEach(key -> {
                log.debug("Polar Paper World Access: Entity NBT: {} = {}", key, entityNBT.nbt().get(key));
            });
        } catch (IOException ex) {
            log.warn("Polar Paper World Access: Failed to read entity NBT", ex);
        }
    }

    static class OpenEntity extends NoPhysicsEntity {

        public OpenEntity(EntityType entityType, UUID uuid) {
            super(entityType, uuid);

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
}
