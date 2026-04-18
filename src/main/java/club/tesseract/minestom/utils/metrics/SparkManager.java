package club.tesseract.minestom.utils.metrics;

import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import lombok.Getter;
import me.lucko.spark.minestom.SparkMinestom;

import java.nio.file.Path;

@Getter
public final class SparkManager {

    private static final Path directory = Path.of("spark");
    public SparkMinestom spark = null;

    public void enable() {
        if (spark != null) return;
        spark = SparkMinestom.builder(directory)
                .commands(true)
                .permissionHandler((sender, permission) -> ExtraConditions.orOp(ExtraConditions.hasPermission(permission)).canUse(sender, null))
                .enable();
    }

    public void shutdown() {
        if (spark == null) return;
        spark.shutdown();
    }


}
