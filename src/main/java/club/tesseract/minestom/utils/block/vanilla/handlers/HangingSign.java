package club.tesseract.minestom.utils.block.vanilla.handlers;


import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class HangingSign implements BlockHandler {

    @Getter
    private static final HangingSign INSTANCE = new HangingSign();
    public static final Key KEY = Key.key("hanging_sign");

    private static final List<Tag<?>> TAGS = List.of(
            Tag.Boolean("is_waxed"),
            Tag.NBT("front_text"),
            Tag.NBT("back_text")
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
