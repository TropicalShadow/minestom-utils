package club.tesseract.minestom.utils.instance.dimension;


import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

/**
 * Dimension that is always full bright.
 * @author TropicalShadow
 * @since 0.0.1
 * @see DimensionType
 */
public final class FullBrightDimension {

    private static FullBrightDimension instance;

    @Getter
    private final @NotNull RegistryKey<@NotNull DimensionType> registryKeyDimension;
    @Getter
    private final @NotNull DimensionType type;


    private FullBrightDimension() {
        type = DimensionType.builder().ambientLight(1.0f).skylight(true).build();
        registryKeyDimension = MinecraftServer.getDimensionTypeRegistry().register(getKey(), type);
    }


    @NotNull
    Key getKey(){
        return Key.key("fullbright");
    }

    /**
     * Returns the singleton instance of the full bright dimension.
     * @return FullBrightDimension
     */
    @NotNull
    public static FullBrightDimension getInstance() {
        if (instance == null) {
            instance = new FullBrightDimension();
        }
        return instance;
    }

}
