package club.tesseract.minestom.utils.command.condition;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A Command Condition use alongside ConditionBuilder to stream-line conditioning
 *
 * @author tropicalshadow
 * @see ConditionBuilder
 * @see CommandCondition
 */
public final class Condition implements CommandCondition {

    private final CommandCondition delegate;

    Condition(@NotNull CommandCondition delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static @NotNull ConditionBuilder builder() {
        return new ConditionBuilder(ExtraConditions.alwaysTrue());
    }

    public static @NotNull ConditionBuilder builder(@NotNull CommandCondition base) {
        return new ConditionBuilder(base);
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender, String commandString) {
        return delegate.canUse(sender, commandString);
    }
}
