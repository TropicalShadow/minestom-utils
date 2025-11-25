package club.tesseract.minestom.utils.metrics;

import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import me.lucko.spark.minestom.SparkMinestom;

import java.nio.file.Path;

public final class SparkManager {

    private static final Path directory = Path.of("spark");
    public static final SparkMinestom spark = SparkMinestom.builder(directory)
            .commands(true)
            .permissionHandler((sender, permission) -> ExtraConditions.hasPermission(permission).canUse(sender, null)) // allows all command senders to execute all commands
            .enable();


    public void shutdown() {
        spark.shutdown();
    }

}
