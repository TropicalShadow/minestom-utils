package club.tesseract.minestom.utils.command;

import lombok.Getter;

@Getter
public enum CommandCategory {
    SERVER("Server Management"),
    PLAYER("Player Commands"),
    GENERIC("Generic Commands"),
    ADMIN("Administration"),
    WORLD("World Management"),
    VANILLA("Vanilla Commands")
    ;

    private final String displayName;

    CommandCategory(String displayName) {
        this.displayName = displayName;
    }

}