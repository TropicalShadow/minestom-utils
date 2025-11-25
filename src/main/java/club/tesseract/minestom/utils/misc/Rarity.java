package club.tesseract.minestom.utils.misc;



import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Enum representing the rarity of an item
 * @author TropicalShadow
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum Rarity {
    COMMON(NamedTextColor.WHITE),
    UNCOMMON(NamedTextColor.GREEN),
    RARE(NamedTextColor.BLUE),
    EPIC(NamedTextColor.DARK_PURPLE),
    LEGENDARY(NamedTextColor.GOLD),
    MYTHIC(NamedTextColor.LIGHT_PURPLE),
    SPECIAL(NamedTextColor.RED),
    VERY_SPECIAL(NamedTextColor.RED),
    ADMIN(NamedTextColor.DARK_RED),
    ;

    private final @NotNull TextColor color;

    /**
     * Returns the next rarity in the enum. (highest ordinal)
     * If the current rarity is the highest, returns the current rarity.
     * @return Rarity
     */
    @NotNull
    public Rarity upgrade() {
        return values()[Math.min(this.ordinal() + 1, values().length - 1)];
    }

    /**
     * Returns the previous rarity in the enum. (lowest ordinal)
     * If the current rarity is the lowest, returns the current rarity.
     * @return Rarity
     */
    @NotNull
    public Rarity downgrade() {
        if (this.ordinal() - 1 < 0)
            return this;
        return values()[this.ordinal() - 1];
    }

    /**
     * check if the current rarity is greater than or equal to the given rarity.
     * @param rarity Rarity to check against
     * @return boolean is current the rarity greater than other
     */
    public boolean isAtLeast(@NotNull Rarity rarity) {
        return ordinal() >= rarity.ordinal();
    }

    /**
     * Converts the rarity to a displayable component.
     * Using {@link Enum#name()} for the name, replacing underscores with spaces, and bolding the name.
     * @see #getDisplayCapitalized()
     * @return Component representation of the rarity
     */
    @NotNull
    public Component getDisplay() {
        return Component.text(name().replace("_", " "), color, TextDecoration.BOLD).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Converts the rarity to a displayable component, capitalizing the first letter.
     * Using {@link Enum#name()} for the name, replacing underscores with spaces, and bolding the name.
     * @see #getDisplay()
     * @return Component representation of the rarity, capitalized
     */
    @NotNull
    public Component getDisplayCapitalized() {
        return Component.text(name().charAt(0) + name().toLowerCase().replaceAll("_", " ").substring(1));
    }

    /**
     * Returns the rarity associated with the given string, if it exists.
     * Case-insensitive.
     * @param string String representation of the rarity
     * @return Optional of the rarity, if it exists.
     */
    @NotNull
    public static Optional<Rarity> getRarity(@NotNull String string) {
        try {
            return Optional.of(Rarity.valueOf(string.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

}