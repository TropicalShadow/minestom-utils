package club.tesseract.minestom.utils.misc.lang;

import net.kyori.adventure.text.Component;
import club.tesseract.minestom.utils.misc.Palette;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.PrintFormat;

public enum MiniMessageHelper {
    ;
    /**
     * Shared parser: the standard tags, every {@link Palette} role as a tag, and the legacy severity tags
     * {@code danger} ({@link Palette#ERROR}), {@code info}, {@code success} and {@code warning}, which are aliases of
     * the same-named roles that also put their old glyph in front ({@code × • ✓ !}).
     * Plain {@code <error>} has no glyph.
     */
    public static final MiniMessage MINI_MESSAGE = create();

    private static MiniMessage create(TagResolver... extra) {
        return MiniMessage.builder()
                .editTags(builder -> {
                    builder.resolver(Palette.resolver());
                    builder.tag("danger", glyphTag(Palette.ERROR, "×"));
                    builder.tag("info", glyphTag(Palette.INFO, "•"));
                    builder.tag("success", glyphTag(Palette.SUCCESS, "✓"));
                    builder.tag("warning", glyphTag(Palette.WARNING, "!"));
                    for (TagResolver resolver : extra) builder.resolver(resolver);
                })
                .build();
    }

    /** Hex form, not the role tag: the legacy names {@code info} etc. are themselves tags and would recurse. */
    private static Tag glyphTag(Palette role, String glyph) {
        return Tag.preProcessParsed("<" + role.hex() + ">" + glyph + " ");
    }

    /**
     * A parser like {@link #MINI_MESSAGE} that also knows {@code resolvers} (later resolvers win over earlier ones).
     * Build it once and reuse it.
     */
    public static MiniMessage withResolvers(TagResolver... resolvers) {
        return create(resolvers);
    }

    /** Every palette role as a MiniMessage tag, for adding to a parser of your own. */
    public static TagResolver roleTags() {
        return Palette.resolver();
    }

    /** Like {@link #toComponent(String)} with extra placeholders or tags for this call only. */
    public static Component toComponentWith(@Language("MiniMessage") String string, TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize("<!i>" + string, resolvers);
    }

    MiniMessageHelper() {
        throw new IllegalStateException("Utility class");
    }


    public static Component toComponent(@Language("MiniMessage") String string) {
        return MINI_MESSAGE.deserialize("<!i>" + string); // !i strips italics, (menus, items, etc)
    }

    /**
     * Formats {@code template} with {@code args} via {@link String#format}, then deserializes as MiniMessage.
     *
     * <p><b>Warning:</b> args are embedded as raw strings before MiniMessage parsing. If any arg contains
     * MiniMessage tags (e.g. player-controlled input like {@code <red>text</red>}), those tags will be
     * interpreted. This is intentional — callers must sanitize untrusted input with
     * {@link MiniMessage#escapeTags(String)} before passing it as an arg if tag injection is undesirable.</p>
     */
    public static Component toComponent(@Language("MiniMessage") @PrintFormat String template, Object... args) {
        return toComponent(String.format(template, args));
    }
}
