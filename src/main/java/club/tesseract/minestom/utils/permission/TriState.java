package club.tesseract.minestom.utils.permission;

public enum TriState {
    TRUE(true),
    FALSE(false),
    DEFAULT(false);

    private final boolean explicit;

    TriState(boolean explicit) {
        this.explicit = explicit;
    }

    public static TriState of(boolean explicit) {
        return explicit ? TRUE : FALSE;
    }

    public static TriState of(Boolean explicit) {
        if (explicit == null) {
            return DEFAULT;
        }
        return of(explicit);
    }

    public boolean asBoolean() {
        return explicit;
    }


}
