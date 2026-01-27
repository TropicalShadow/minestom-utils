package club.tesseract.minestom.utils.misc.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.intellij.lang.annotations.Pattern;
import org.slf4j.helpers.MessageFormatter;

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


    public static Component toComponent(String string) {
        return MINI_MESSAGE.deserialize("<!i>" + string); // !i strips italics, (menus, items, etc)
    }

    public static Component toComponent(@Pattern("\\{}") String template, Object... args) {
        return toComponent(MessageFormatter.arrayFormat(template, args).getMessage()); // did I cook or is this cooked?
    }


}
