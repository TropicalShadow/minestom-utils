package club.tesseract.minestom.utils.misc.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.PrintFormat;

public enum MiniMessageHelper {
    ;
    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .editTags(builder -> {
                builder.tag("danger", Tag.preProcessParsed("<red>× "));
                builder.tag("info", Tag.preProcessParsed("<gray>• "));
                builder.tag("success", Tag.preProcessParsed("<green>✓ "));
                builder.tag("warning", Tag.preProcessParsed("<yellow>! "));
            })
            .build();

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
