package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.CommandCategory;
import club.tesseract.minestom.utils.command.CommandMetadata;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import club.tesseract.minestom.utils.command.sender.CommandBlockSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec2;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec3;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import net.minestom.server.utils.location.RelativeVec;

import java.util.List;
import java.util.Optional;
import java.util.Locale;

@CommandMetadata(
        categories = {CommandCategory.ADMIN, CommandCategory.PLAYER, CommandCategory.WORLD, CommandCategory.VANILLA},
        description = "Teleports a player to another player or location"
)
public class TeleportCommand extends Command {

    ArgumentEntity targetArgument = ArgumentType.Entity("targets").singleEntity(false);
    ArgumentEntity destinationArgument = ArgumentType.Entity("destination").singleEntity(true);

    ArgumentRelativeVec2 viewArgument = ArgumentType.RelativeVec2("view");
    ArgumentRelativeVec3 locationArgument = ArgumentType.RelativeVec3("relativeVec3");


    public TeleportCommand() {
        super("teleport", "tp");

        setCondition(ExtraConditions.hasPermission("minecraft.command.tp"));

        setDefaultExecutor((sender, _) -> sender.sendMessage("Usage: /teleport <player> [player]"));

        // Entity base teleportation: sender -> destination entity
        addSyntax((sender, context) -> {
            if (!(sender instanceof Entity entity)) {
                sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                return;
            }

            EntityFinder destination = context.get(destinationArgument);
            List<Entity> found = destination.find(sender);
            if (found.isEmpty()) {
                sender.sendMessage(Component.translatable("commands.teleport.invalidPosition").color(NamedTextColor.RED));
                return;
            }

            Entity dest = found.getFirst();
            // teleport the sender entity to dest
            if (entity.getInstance() != dest.getInstance()) {
                entity.setInstance(dest.getInstance(), dest.getPosition());
            } else {
                entity.teleport(dest.getPosition());
            }

            // send success message for single teleport (Mojang style: name, name)
            Component entityName = getNameForEntity(entity);
            Component destinationName = getNameForEntity(dest);
            sender.sendMessage(Component.translatable("commands.teleport.success.entity.single", entityName, destinationName));
        }, destinationArgument);

        // sender -> relative location + view
        addSyntax((sender, context) -> {
            if (!(sender instanceof Entity entity)) {
                sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                return;
            }
            RelativeVec relativeVec = context.get(locationArgument);
            RelativeVec viewVec = context.get(viewArgument);

            Vec vec = relativeVec.from(entity);
            Vec view = viewVec.fromView(entity);
            Pos pos = vec.asPos().withDirection(view.asPos());

            entity.teleport(pos);

            // success message (Mojang style: name, x, y, z)
            Component entityName = getNameForEntity(entity);
            sender.sendMessage(Component.translatable("commands.teleport.success.location.single", entityName, Component.text(formatDouble(pos.x())), Component.text(formatDouble(pos.y())), Component.text(formatDouble(pos.z()))));
        }, locationArgument, viewArgument);

        // targets -> destination entity
        addSyntax((sender, context) -> {
            List<Entity> targets = context.get(this.targetArgument).find(sender);
            Optional<Entity> destinationOpt = Optional.ofNullable(context.get(destinationArgument).findFirstEntity(sender));

            if (targets.isEmpty()) {
                sender.sendMessage(Component.text("Invalid targets", NamedTextColor.RED));
                return;
            }
            if (destinationOpt.isEmpty()) {
                sender.sendMessage(Component.translatable("commands.teleport.invalidPosition").color(NamedTextColor.RED));
                return;
            }

            Entity destination = destinationOpt.get();
            Component destinationName = destination instanceof Player player ? player.getName() : Component.translatable("entity.minecraft." + destination.getEntityType().name().toLowerCase());

            for (Entity entity : targets) {
                if (entity.getInstance() != destination.getInstance()) {
                    // switch instance then continue teleporting other targets
                    entity.setInstance(destination.getInstance(), destination.getPosition());
                    continue;
                }
                entity.teleport(destination.getPosition());
            }

            if (targets.size() > 1) {
                // Mojang style: (count, destinationName)
                sender.sendMessage(Component.translatable("commands.teleport.success.entity.multiple", Component.text(targets.size()), destinationName));
            } else {
                Entity target = targets.getFirst();
                Component targetName = target instanceof Player player ? player.getName() : Component.translatable("entity.minecraft." + target.getEntityType().name().toLowerCase());
                sender.sendMessage(Component.translatable("commands.teleport.success.entity.single", targetName, destinationName));
            }
        }, targetArgument, destinationArgument);

        // targets -> relative block position
        addSyntax((sender, context) -> {
            RelativeVec blockVec = context.get(locationArgument);
            Vec vec;
            if (sender instanceof Player player) {
                vec = blockVec.from(player);
            } else if (sender instanceof CommandBlockSender commandBlockSender) {
                vec = blockVec.from(commandBlockSender.getVec().asPos());
            } else {
                vec = blockVec.from(Pos.ZERO);
            }

            EntityFinder finder = context.get(targetArgument);
            List<Entity> entities = finder.find(sender);
            if (entities.isEmpty()) return;
            Pos pos = vec.asPos();

            int count = 0;
            Entity last = null;
            for (Entity e : entities) {
                if (e.getInstance() != (sender instanceof Entity sEntity ? sEntity.getInstance() : null) && sender instanceof Entity s) {
                    e.setInstance(s.getInstance(), pos);
                } else {
                    e.teleport(pos);
                }
                count++;
                last = e;
            }

            if (count == 1) {
                Component targetName = getNameForEntity(last);
                sender.sendMessage(Component.translatable("commands.teleport.success.location.single", targetName, Component.text(formatDouble(pos.x())), Component.text(formatDouble(pos.y())), Component.text(formatDouble(pos.z()))));
            } else {
                sender.sendMessage(Component.translatable("commands.teleport.success.location.multiple", Component.text(count), Component.text(formatDouble(pos.x())), Component.text(formatDouble(pos.y())), Component.text(formatDouble(pos.z()))));
            }
        }, targetArgument, locationArgument);

        // targets -> relative location + view
        addSyntax((sender, context) -> {
            RelativeVec relativeVec = context.get(locationArgument);
            RelativeVec viewVec = context.get(viewArgument);
            Vec vec;
            Vec view;
            if (sender instanceof Player player) {
                vec = relativeVec.from(player);
                view = viewVec.fromView(player);
            } else if (sender instanceof CommandBlockSender commandBlockSender) {
                vec = relativeVec.from(commandBlockSender.getVec().asPos());
                view = viewVec.fromView(commandBlockSender.getVec().asPos());
            } else {
                vec = relativeVec.from(Pos.ZERO);
                view = viewVec.fromView(Pos.ZERO);
            }

            EntityFinder finder = context.get(targetArgument);
            List<Entity> entities = finder.find(sender);
            if (entities.isEmpty()) return;

            Pos pos = vec.asPos().withDirection(view.asPos());

            int count = 0;
            Entity last = null;
            for (Entity e : entities) {
                if (e.getInstance() != (sender instanceof Entity sEntity ? sEntity.getInstance() : null) && sender instanceof Entity s) {
                    e.setInstance(s.getInstance(), pos);
                } else {
                    e.teleport(pos);
                }
                count++;
                last = e;
            }

            if (count == 1) {
                Component targetName = getNameForEntity(last);
                sender.sendMessage(Component.translatable("commands.teleport.success.location.single", targetName, Component.text(formatDouble(pos.x())), Component.text(formatDouble(pos.y())), Component.text(formatDouble(pos.z()))));
            } else {
                sender.sendMessage(Component.translatable("commands.teleport.success.location.multiple", Component.text(count), Component.text(formatDouble(pos.x())), Component.text(formatDouble(pos.y())), Component.text(formatDouble(pos.z()))));
            }
        }, targetArgument, locationArgument, viewArgument);
    }

    private static Component getNameForEntity(Entity e) {
        if (e instanceof Player player) return player.getName();
        return Component.translatable("entity.minecraft." + e.getEntityType().name().toLowerCase());
    }

    private static String formatDouble(double value) {
        // Mojang shows coordinates with 2 decimal places for readability
        return String.format(Locale.ROOT, "%.2f", value);
    }
}