package club.tesseract.minestom.utils.permission;

import org.jetbrains.annotations.Nullable;

public record SetPermissionResult(
        boolean success,
        @Nullable String message,
        @Nullable Object extra
) {
}
