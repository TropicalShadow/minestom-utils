package club.tesseract.minestom.utils.command.args;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PlayerArgument extends ArgumentEntity {

    public PlayerArgument(String id) {
        this(id, true);
    }

    public PlayerArgument(String id, boolean singleEntity) {
        super(id);

        onlyPlayers(true);
        singleEntity(singleEntity);
    }


    @NotNull
    public Optional<Player> getPlayer(CommandSender sender, CommandContext context) {
        EntityFinder finder = context.get(this);
        if (finder == null) {
            return Optional.empty();
        }

        var entities = finder.find(sender);
        if (entities.isEmpty()) {
            return Optional.empty();
        }

        return entities.getFirst() instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    @NotNull
    public List<Player> getPlayers(CommandSender sender, CommandContext context) {
        EntityFinder finder = context.get(this);
        if (finder == null) {
            return List.of();
        }

        var entities = finder.find(sender);
        if (entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .toList();
    }
}
