package club.tesseract.minestom.utils.misc.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.utils.StringUtils;

import java.util.*;

// CREDIT TO MR [MINIKLOON](https://gist.github.com/Minikloon/e6a7679d171b90dc4e0731db46d77c84)
public class ComponentHelper {

    private static final TextComponent UNSUPPORTED = Component.text("ERROR WRAPPING").color(NamedTextColor.DARK_RED);

    public static List<Component> wrapLoreLines(final Component component, final int length) {

        if (!(component instanceof final TextComponent text)) {
            return Collections.singletonList(component);
        }

        final List<Component> wrapped = new ArrayList<>();
        final List<TextComponent> parts = flattenTextComponents(text);

        Component currentLine = Component.empty();
        int lineLength = 0;

        for (final TextComponent part : parts) {

            final Style style = part.style();
            final String content = part.content();
            final String[] words = content.split("(?<=\\s)|(?=\\n)");

            for (final String word : words) {

                if (word.isEmpty()) {
                    continue;
                }

                final int wordLength = word.length();
                final int totalLength = lineLength + wordLength;
                if (totalLength > length || word.contains("\n")) {
                    wrapped.add(currentLine);
                    currentLine = Component.empty().style(style);
                    lineLength = 0;
                }

                if (!word.equals("\n")) {
                    currentLine = currentLine.append(Component.text(word).style(style));
                    lineLength += wordLength;
                }
            }
        }

        if (lineLength > 0) {
            wrapped.add(currentLine);
        }

        return wrapped;
    }

    private static List<TextComponent> flattenTextComponents(final TextComponent component) {

        final List<TextComponent> flattened = new ArrayList<>();
        final Style style = component.style();
        final Style enforcedState = enforceStyleStates(style);
        final TextComponent first = component.style(enforcedState);

        final Stack<TextComponent> toCheck = new Stack<>();
        toCheck.add(first);

        while (!toCheck.empty()) {

            final TextComponent parent = toCheck.pop();
            final String content = parent.content();
            if (!content.isEmpty()) {
                flattened.add(parent);
            }

            final List<Component> children = parent.children();
            final List<Component> reversed = children.reversed();
            for (final Component child : reversed) {
                if (child instanceof final TextComponent text) {
                    final Style parentStyle = parent.style();
                    final Style textStyle = text.style();
                    final Style merge = parentStyle.merge(textStyle);
                    final TextComponent childComponent = text.style(merge);
                    toCheck.add(childComponent);
                } else {
                    toCheck.add(UNSUPPORTED);
                }
            }
        }
        return flattened;
    }

    private static Style enforceStyleStates(final Style style) {
        final Style.Builder builder = style.toBuilder();
        final Map<TextDecoration, TextDecoration.State> map = style.decorations();
        map.forEach((decoration, state) -> {
            if (state == TextDecoration.State.NOT_SET) {
                builder.decoration(decoration, false);
            }
        });
        return builder.build();
    }

    public static List<Component> wrap(Component component, int length) {
        if (!(component instanceof TextComponent text)) {
            return Collections.singletonList(component);
        }

        List<Component> wrapped = new ArrayList<>();

        List<TextComponent> parts = flatten(text);
        Component currentLine = Component.empty();
        int lineLength = 0;
        for (int i = 0; i < parts.size(); i++) {
            TextComponent part = parts.get(i);
            Style style = part.style();
            String content = part.content();

            TextComponent nextPart = i == parts.size() - 1 ? null : parts.get(i + 1);
            boolean join = nextPart != null && (part.content().endsWith(" ") || nextPart.content().startsWith(" "));

            StringBuilder lineBuilder = new StringBuilder();

            String[] words = content.split(" ");
            words = Arrays.stream(words)
                    .flatMap(word -> Arrays.stream(word.splitWithDelimiters("\n", -1)))
                    .toArray(String[]::new);
            for (int j = 0; j < words.length; j++) {
                String word = words[j];
                boolean lastWord = j == words.length - 1;
                if (word.isEmpty()) continue;
                boolean isLongEnough = lineLength != 0 && lineLength + word.length() > length;
                int newLines = StringUtils.countMatches(word, '\n') + (isLongEnough ? 1 : 0);
                for (int k = 0; k < newLines; ++k) {
                    String endOfLine = lineBuilder.toString();

                    currentLine = currentLine.append(Component.text(endOfLine).style(style));
                    wrapped.add(currentLine);

                    lineLength = 0;
                    currentLine = Component.empty().style(style);
                    lineBuilder = new StringBuilder();
                }
                boolean addSpace = (!lastWord || join) && !word.endsWith("\n");
                String cleanWord = word.replace("\n", "");
                lineBuilder.append(cleanWord).append(addSpace ? " " : "");
                lineLength += word.length() + 1;
            }
            String endOfComponent = lineBuilder.toString();
            if (!endOfComponent.isEmpty()) {
                currentLine = currentLine.append(Component.text(endOfComponent).style(style));
            }
        }

        if (lineLength > 0) {
            wrapped.add(currentLine);
        }

        return wrapped;
    }

    private static List<TextComponent> flatten(TextComponent component) {
        List<TextComponent> flattened = new ArrayList<>();

        Style enforcedState = enforceStates(component.style());
        component = component.style(enforcedState);

        Stack<TextComponent> toCheck = new Stack<>();
        toCheck.add(component);

        while (!toCheck.empty()) {
            TextComponent parent = toCheck.pop();
            if (!parent.content().isEmpty()) {
                flattened.add(parent);
            }

            for (Component child : parent.children().reversed()) {
                if (child instanceof TextComponent text) {
                    Style style = parent.style();
                    style = style.merge(child.style());
                    toCheck.add(text.style(style));
                } else {
                    toCheck.add(unsupported());
                }
            }
        }
        return flattened;
    }

    private static Style enforceStates(Style style) {
        Style.Builder builder = style.toBuilder();
        style.decorations().forEach((decoration, state) -> {
            if (state == TextDecoration.State.NOT_SET) {
                builder.decoration(decoration, false);
            }
        });
        return builder.build();
    }

    private static TextComponent unsupported() {
        return Component.text("!CANNOT WRAP!").color(NamedTextColor.DARK_RED);
    }
}