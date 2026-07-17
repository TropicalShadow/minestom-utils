package club.tesseract.minestom.utils.permission.group;

import club.tesseract.minestom.utils.permission.PermissionNode;
import club.tesseract.minestom.utils.permission.TriState;
import lombok.Getter;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionGroup {

    private final String id;

    public String getId() {
        return id;
    }

    @Getter
    private final String name;
    @Getter
    private final int weight;

    @Getter
    private final PermissionNode root = new PermissionNode();

    @Getter
    private final Set<PermissionGroup> parents = ConcurrentHashMap.newKeySet();

    private final Map<String, String> meta = new ConcurrentHashMap<>();

    public PermissionGroup(String name, int weight) {
        this(name, name, weight);
    }

    public PermissionGroup(String id, String name, int weight) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        PermissionGroupRegistry.register(this);
    }


    public void setPermission(String permission, TriState value) {
        PermissionNode node = root;
        for (String part : permission.toLowerCase(Locale.ROOT).split("\\.")) {
            node = node.children.computeIfAbsent(part, k -> new PermissionNode());
        }
        node.setValue(value);
    }

    public void inherit(PermissionGroup parent) {
        parents.add(parent);
    }

    public void setMeta(String key, String value) {
        meta.put(key.toLowerCase(), value);
    }

    public String getMeta(String key) {
        return meta.get(key.toLowerCase());
    }

    public Map<String, String> getMetaMap() {
        return meta;
    }
}