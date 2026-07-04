package club.tesseract.minestom.utils.permission;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PermissionNode {
    public final ConcurrentMap<String, PermissionNode> children = new ConcurrentHashMap<>();
    TriState value = TriState.DEFAULT;
}