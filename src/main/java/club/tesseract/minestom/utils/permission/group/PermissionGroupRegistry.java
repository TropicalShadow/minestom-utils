package club.tesseract.minestom.utils.permission.group;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionGroupRegistry {

    private static final Map<String, PermissionGroup> GROUPS = new ConcurrentHashMap<>();

    private PermissionGroupRegistry() {
    }

    public static void register(PermissionGroup group) {
        GROUPS.put(group.getId().toLowerCase(), group);
    }

    public static void registerAll(Collection<PermissionGroup> groups) {
        for (PermissionGroup group : groups) {
            register(group);
        }
    }

    @Nullable
    public static PermissionGroup get(String id) {
        return GROUPS.get(id.toLowerCase());
    }
}
