package club.tesseract.minestom.utils.block.vanilla.handlers;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class PlayerHead implements BlockHandler {
    @Getter
    private final static PlayerHead INSTANCE = new PlayerHead();

    public final static Key KEY = Key.key("minecraft:skull");
    private final static List<Tag<?>> TAGS = List.of(
            Tag.NBT("profile")
    );

    @Override
    public @NotNull Key getKey() {
        return KEY;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return TAGS;
    }

}
