package club.tesseract.minestom.utils.permission.cache;


import club.tesseract.minestom.utils.permission.TriState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionCache {

    public final Map<String, TriState> permissions = new ConcurrentHashMap<>();

    public String prefix;
    public String suffix;
}