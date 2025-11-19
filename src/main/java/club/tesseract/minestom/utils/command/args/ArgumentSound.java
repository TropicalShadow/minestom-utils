package club.tesseract.minestom.utils.command.args;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentRegistry;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.sound.SoundEvent;
import org.intellij.lang.annotations.RegExp;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ArgumentSound extends ArgumentRegistry<SoundEvent> {

    static final @RegExp String NAMESPACE_PATTERN = "[a-z0-9_\\-.]+";
    static final @RegExp String VALUE_PATTERN = "[a-z0-9_\\-./]+";
    static final String FULL_REGEX = "(?:(" + NAMESPACE_PATTERN + ":)?|:)" + VALUE_PATTERN;

    Pattern SOUND_PATTERN = Pattern.compile(FULL_REGEX);
    Pattern SOUND_PATTERN_WITHOUT_NAMESPACE = Pattern.compile(VALUE_PATTERN);

    public ArgumentSound(@NotNull String id) {
        super(id);
        setSuggestionCallback((commandSender, commandContext, suggestion) ->{
            String nullByte = "\u0000";
            String rawInput = commandContext.getInput();
            String commandName = commandContext.getCommandName();
            String rawKey = rawInput.replaceFirst("^" + commandName + " ", "").replace(nullByte, "").strip();
            Stream<? extends SoundEvent> filteredStreamOfKeys;
            boolean isBlank = rawKey.isBlank();
            if(isBlank){
                filteredStreamOfKeys = SoundEvent.values().stream();
            }else {
                filteredStreamOfKeys = SoundEvent.values().stream().filter(entry -> entry.key().value().startsWith(rawKey.replaceFirst("^minecraft:", "")));
            }

            boolean startsWithMinecraft = rawKey.startsWith("minecraft:");

            List<? extends SoundEvent> soundEvents = filteredStreamOfKeys.toList();
            if(soundEvents.isEmpty()){
                SoundEvent.values().stream()
                        .filter(entry -> entry.key().value().contains(rawKey.replaceFirst("^minecraft:", "")))
                        .forEach(soundEvent -> {
                            SuggestionEntry suggestionEntry;
                            if(!startsWithMinecraft){
                                if(soundEvent.key().namespace().equalsIgnoreCase("minecraft")){
                                    suggestionEntry = new SuggestionEntry(soundEvent.key().value(), Component.text(soundEvent.key().asString()));
                                }else{
                                    suggestionEntry = new SuggestionEntry(soundEvent.key().asString(), Component.text(soundEvent.key().asString()));
                                }
                            }else{
                                suggestionEntry = new SuggestionEntry(soundEvent.key().asString(), Component.text(soundEvent.key().asString()));
                            }
                            suggestion.addEntry(suggestionEntry);
                        });
                return;
            }

            soundEvents.forEach(entry ->{
                String keyString = entry.key().asString();
                SuggestionEntry suggestionEntry;
                if(!startsWithMinecraft){
                    if(entry.key().namespace().equalsIgnoreCase("minecraft")){
                        suggestionEntry = new SuggestionEntry(entry.key().value(), Component.text(keyString));
                    }else{
                        suggestionEntry = new SuggestionEntry(entry.key().asString(), Component.text(keyString));
                    }
                }else{
                    suggestionEntry = new SuggestionEntry(entry.key().asString(), Component.text(keyString));
                }
                suggestion.addEntry(suggestionEntry);
            });
        });
    }




    @Override
    public ArgumentParserType parser() {
        return ArgumentParserType.RESOURCE_LOCATION;
    }

    @Override
    public SoundEvent getRegistry(@KeyPattern @NotNull String value) {
        Matcher fullNamespace = SOUND_PATTERN.matcher(value);
        Matcher withoutNamespace = SOUND_PATTERN_WITHOUT_NAMESPACE.matcher(value);
        if(fullNamespace.find()){
            @Subst("minecraft:entity.mule.eat") String keyString = fullNamespace.group();
            return SoundEvent.fromKey(Key.key(keyString));
        } else if (withoutNamespace.find()) {
            @Subst("minecraft:entity.mule.eat") String keyString = withoutNamespace.group();
            return SoundEvent.fromKey(Key.key(keyString));
        }

        return null;
    }


}
