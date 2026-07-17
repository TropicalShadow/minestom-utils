package club.tesseract.minestom.utils.permission;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PermissionNode {
    public final ConcurrentMap<String, PermissionNode> children = new ConcurrentHashMap<>();

    @Setter
    @Getter
    TriState value = TriState.DEFAULT;
}