package club.tesseract.minestom.utils.block;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.instance.block.Block;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockFinder {

    private static final @Subst("white") Pattern BLOCK_REGEX = Pattern.compile("^(white|orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue|brown|green|red|black)_([a-z_]+)$");

    private BlockFinder(){

    }


    /**
     * Used to derive the coloured variants of a certain block type
     * i.e. Wool (RED_WOOL, BLUE_WOOL, etc)
     *
     * @param colour
     * @param defaultBlock
     * @return Block
     */
    public static Block fromColour(TextColor colour, Block defaultBlock){
        final NamedTextColor namedColour = NamedTextColor.nearestTo(colour);
        final @Subst("white") Colour changedColour = Colour.fromOrDefault(namedColour, Colour.BLACK);
        if(changedColour == null){
            return defaultBlock;
        }
        return fromColour(changedColour, defaultBlock);
    }

    public static Block fromColour(@NotNull Colour colour, Block defaultBlock){
        Key key = defaultBlock.key();
        if(!key.namespace().equals("minecraft")){
            return defaultBlock; // nu uh to modded or non minecraft block
        }

        String blockName = key.value().toLowerCase(Locale.ROOT);
        Matcher matcher = BLOCK_REGEX.matcher(blockName);
        boolean success = matcher.find();
        if(success){
            String blockType = matcher.group(2);
            return Block.fromKey(Key.key("minecraft", colour.key + "_" + blockType));
        }
        return defaultBlock;
    }

    @Getter
    public enum Colour{
        BLACK(0, "black"),
        WHITE(1, "white"),
        ORANGE(2, "orange"),
        MAGENTA(3, "magenta"),
        LIGHT_BLUE(4, "light_blue"),
        YELLOW(5, "yellow"),
        LIME(6, "lime"),
        PINK(7, "pink"),
        GRAY(8, "gray"),
        LIGHT_GRAY(9, "light_gray"),
        CYAN(10, "cyan"),
        PURPLE(11, "purple"),
        BLUE(12, "blue"),
        BROWN(13, "brown"),
        GREEN(14, "green"),
        RED(15, "red"),
        ;
        private static final Map<NamedTextColor, Colour> FROM_NAMED;

        static {
            Map<NamedTextColor, Colour> map = new IdentityHashMap<>();

            map.put(NamedTextColor.BLACK, BLACK);
            map.put(NamedTextColor.WHITE, WHITE);

            map.put(NamedTextColor.GOLD, ORANGE);
            map.put(NamedTextColor.YELLOW, YELLOW);

            map.put(NamedTextColor.LIGHT_PURPLE, MAGENTA);
            map.put(NamedTextColor.DARK_PURPLE, PURPLE);

            map.put(NamedTextColor.BLUE, BLUE);
            map.put(NamedTextColor.DARK_BLUE, BLUE);

            map.put(NamedTextColor.AQUA, CYAN);
            map.put(NamedTextColor.DARK_AQUA, CYAN);

            map.put(NamedTextColor.GREEN, LIME);
            map.put(NamedTextColor.DARK_GREEN, GREEN);

            map.put(NamedTextColor.RED, RED);
            map.put(NamedTextColor.DARK_RED, RED);

            map.put(NamedTextColor.GRAY, LIGHT_GRAY);
            map.put(NamedTextColor.DARK_GRAY, GRAY);

            FROM_NAMED = Collections.unmodifiableMap(map);
        }

        private final int id;
        private final String key;

        Colour(int id, String key){
            this.id = id;
            this.key = key;
        }


        public static @Nullable Colour fromId(int id){
            for (Colour value : values()) {
                if(value.id == id){
                    return value;
                }
            }
            return null;
        }

        public static Optional<Colour> from(NamedTextColor color) {
            return Optional.ofNullable(FROM_NAMED.get(color));
        }

        public static Colour fromOrDefault(NamedTextColor color, Colour fallback) {
            return FROM_NAMED.getOrDefault(color, fallback);
        }


    }

}
