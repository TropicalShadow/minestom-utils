package club.tesseract.minestom.utils.command.condition;

import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.condition.Conditions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A builder for creating complex command conditions using logical operations.
 * @author TropicalShadow
 * @see Condition#builder(CommandCondition)
 * @see CommandCondition
 * @see Conditions
 */
public final class ConditionBuilder {

    private CommandCondition current;

    ConditionBuilder(@NotNull CommandCondition base) {
        this.current = Objects.requireNonNull(base, "base");
    }

    public @NotNull ConditionBuilder and(@NotNull CommandCondition... conditions) {
        current = Conditions.all(merge(current, conditions));
        return this;
    }

    public @NotNull ConditionBuilder or(@NotNull CommandCondition... conditions) {
        current = Conditions.any(merge(current, conditions));
        return this;
    }

    public @NotNull ConditionBuilder not() {
        current = Conditions.not(current);
        return this;
    }

    public @NotNull Condition build() {
        return new Condition(current);
    }

    private static CommandCondition[] merge(
            @NotNull CommandCondition current,
            @NotNull CommandCondition[] conditions
    ) {
        Objects.requireNonNull(conditions, "conditions");
        CommandCondition[] merged = new CommandCondition[conditions.length + 1];
        merged[0] = current;
        System.arraycopy(conditions, 0, merged, 1, conditions.length);
        return merged;
    }
}
