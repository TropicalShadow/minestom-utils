package club.tesseract.minestom.utils.misc.lang;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.PrintFormat;
import org.intellij.lang.annotations.RegExp;
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


    public static Component toComponent(@Language("MiniMessage") String string) {
        return MINI_MESSAGE.deserialize("<!i>" + string); // !i strips italics, (menus, items, etc)
    }

    public static Component toComponent(@Language("MiniMessage") @PrintFormat String template, Object... args) {
        return toComponent(String.format(template, args)); // did I cook or is this cooked?
    }


}
