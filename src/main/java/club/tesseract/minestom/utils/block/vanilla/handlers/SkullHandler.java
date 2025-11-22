package club.tesseract.minestom.utils.block.vanilla.handlers;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class SkullHandler implements BlockHandler {

    @Getter
    public static final SkullHandler INSTANCE = new SkullHandler();
    public static final Key KEY = Key.key("minecraft:skull");
    private static final List<Tag<?>> TAGS = List.of(
            Tag.String("custom_name"),
            Tag.NBT("SkullOwner"),
            Tag.String("ExtraType")
    );

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAGS;
    }

    @Override
    public @NotNull Key getKey() {
        return KEY;
    }
}
