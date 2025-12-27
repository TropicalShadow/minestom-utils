package club.tesseract.minestom.utils.instance.polar;

import club.tesseract.minestom.utils.entity.EntityData;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EntityCreator {

    void apply(@NotNull Instance instance, @NotNull EntityData data);

}
