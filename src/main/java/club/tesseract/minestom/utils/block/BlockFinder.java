package club.tesseract.minestom.utils.block;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.instance.block.Block;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
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
        final @Subst("white") Colour changedColour = Colour.fromNamedTextColor(namedColour);
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
            String colourName = matcher.group(1);
            String blockType = matcher.group(2);
            return Block.fromKey(Key.key("minecraft", colour.key + "_" + blockType));
        }
        return defaultBlock;
    }

    @Getter
    public enum Colour{
        WHITE(0, "white"),
        ORANGE(1, "orange"),
        MAGENTA(2, "magenta"),
        LIGHT_BLUE(3, "light_blue"),
        YELLOW(4, "yellow"),
        LIME(5, "lime"),
        PINK(6, "pink"),
        GRAY(7, "gray"),
        LIGHT_GRAY(8, "light_gray"),
        CYAN(9, "cyan"),
        PURPLE(10, "purple"),
        BLUE(11, "blue"),
        BROWN(12, "brown"),
        GREEN(13, "green"),
        RED(14, "red"),
        ;

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

        public static @Nullable Colour fromNamedTextColor(NamedTextColor namedTextColor){
            String name = NamedTextColor.NAMES.key(namedTextColor);
            for (Colour value : values()) {
                if(value.key.equals(name)){
                    return value;
                }
            }
            return null;
        }


    }

}
