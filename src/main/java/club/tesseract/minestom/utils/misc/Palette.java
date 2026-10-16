package club.tesseract.minestom.utils.misc;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The colour roles used for player-facing text, as a minimal mirror of ReleaseSplash's {@code :theme} {@code Role}.
 *
 * <p><b>Source of truth:</b> ReleaseSplash's {@code theme} module ({@code net.bridgesplash.theme.Role}). This library
 * is published independently and cannot depend on it, and a drift test cannot span the two repos, so the values here
 * are copied by hand and pinned by {@code PaletteTest}. When a hex changes in {@code Role}, change it here too, update
 * the test, and release a new version. Current hexes:</p>
 * <pre>
 * TEXT #EEEEEE   MUTED #ABABAB   SUBTLE #808080
 * BRAND #55DDFF  SUCCESS #6AE295 WARNING #FFBA43 ERROR #F87164 INFO #78B9F3 REWARD #DECF5F
 * PARTY #7BB0FF  FLEET #79D7CF   STAFF #E77DB7   DM #CBA3EF
 * TEAM_1 #F84C4B TEAM_2 #1E84E7  TEAM_3 #F6DD00  TEAM_4 #00D0DD
 * ACCENT #FFB366 GEM #F097D8     SILVER #CACACA  BRONZE #DD844A
 * </pre>
 *
 * <p>Each role is also a MiniMessage tag named {@link #tag()} (see {@link #resolver()}).</p>
 *
 * @since 0.3.0
 */
public enum Palette {
    TEXT("#EEEEEE"), MUTED("#ABABAB"), SUBTLE("#808080"),
    BRAND("#55DDFF"), SUCCESS("#6AE295"), WARNING("#FFBA43"), ERROR("#F87164"), INFO("#78B9F3"), REWARD("#DECF5F"),
    PARTY("#7BB0FF"), FLEET("#79D7CF"), STAFF("#E77DB7"), DM("#CBA3EF"),
    TEAM_1("#F84C4B"), TEAM_2("#1E84E7"), TEAM_3("#F6DD00"), TEAM_4("#00D0DD"),
    ACCENT("#FFB366"), GEM("#F097D8"), SILVER("#CACACA"), BRONZE("#DD844A");

    private final String hex;
    private final TextColor color;

    Palette(String hex) {
        this.hex = hex;
        this.color = TextColor.fromHexString(hex);
    }

    public @NotNull String hex() {
        return hex;
    }

    public @NotNull TextColor color() {
        return color;
    }

    /** MiniMessage tag name: lower case, e.g. {@code error}, {@code team_1}. */
    public @NotNull String tag() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Resolver registering every role as a colour tag, e.g. {@code <success>text</success>}. */
    public static @NotNull TagResolver resolver() {
        TagResolver.Builder builder = TagResolver.builder();
        for (Palette role : values()) builder.tag(role.tag(), Tag.styling(role.color));
        return builder.build();
    }

    /**
     * Load gauge colour: {@link #SUCCESS} at 0, {@link #WARNING} at 0.5, {@link #ERROR} at 1 (RGB blend).
     *
     * @param badness how bad the load is; clamped to 0..1
     */
    public static @NotNull TextColor gauge(double badness) {
        float t = (float) Math.max(0.0, Math.min(1.0, Double.isNaN(badness) ? 1.0 : badness));
        return t < 0.5f
                ? TextColor.lerp(t * 2f, SUCCESS.color, WARNING.color)
                : TextColor.lerp((t - 0.5f) * 2f, WARNING.color, ERROR.color);
    }
}
