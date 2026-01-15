package club.tesseract.minestom.utils.instance.polar;

import club.tesseract.minestom.utils.entity.EntityData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public record EntityNBT(CompoundBinaryTag nbt){

    @NotNull
    public Optional<UUID> getUniqueId(){
        BinaryTag tag = nbt.get("uuid");
        if(!(tag instanceof IntArrayBinaryTag intArrayBinaryTag)) return Optional.empty();
        return Optional.of(UUIDUtils.fromNbt(intArrayBinaryTag));
    }

    @NotNull
    public Optional<Component> getCustomName(){
        BinaryTag input = nbt.get("CustomName");
        if(input == null) return Optional.empty();
        final Transcoder<@NotNull BinaryTag> coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
        return Optional.ofNullable(Codec.COMPONENT.decode(coder, input).orElse(null));
    }

    public boolean CustomNameVisible(){
        return nbt.getBoolean("CustomNameVisible", false);
    }

    @UnknownNullability
    public EntityType getEntityType(){
        return EntityType.fromKey(Key.key(nbt.getString("id", EntityType.SMALL_FIREBALL.key().asString())));
    }

    @NotNull
    public CustomData customData(){
        if(nbt.contains("data")){
            return new CustomData(nbt.getCompound("data"));
        }
        return CustomData.EMPTY;
    }

    @NotNull
    public Pos getPosition(){
        ListBinaryTag tagList = nbt.getList("Pos", BinaryTagTypes.DOUBLE);
        double x = tagList.getDouble(0);
        double y = tagList.getDouble(1);
        double z = tagList.getDouble(2);
        return new Pos(x, y, z);
    }

    @NotNull
    public Pos getDirection(){
        ListBinaryTag tagList = nbt.getList("Rotation", BinaryTagTypes.FLOAT);
        Pos pos = Pos.ZERO;
        pos = pos.withYaw(tagList.getFloat(0));
        pos = pos.withPitch(tagList.getFloat(1));
        return pos;
    }

    @NotNull
    public static EntityNBT fromEntityData(EntityData data) throws IOException {
        return new EntityNBT(data.getEntityNBT());
    }

}
