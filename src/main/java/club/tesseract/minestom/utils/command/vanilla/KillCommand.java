package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.CommandCategory;
import club.tesseract.minestom.utils.command.CommandMetadata;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

@CommandMetadata(
        categories = {CommandCategory.ADMIN, CommandCategory.PLAYER, CommandCategory.VANILLA},
        description = "Kills an entity or entities"
)
public class KillCommand extends Command {

    private static final ArgumentEntity TARGET_ARGUMENT = ArgumentType.Entity("target").onlyPlayers(false).singleEntity(false);
    public static final Tag<Boolean> KILLABLE_TAG = Tag.Boolean("killable").defaultValue(true);

    public KillCommand() {
        super("kill");

        setDefaultExecutor((sender, _) -> sender.sendMessage("Usage: /kill <player>"));

        addSyntax((sender, context) -> {
            final EntityFinder finder = context.get(TARGET_ARGUMENT);
            List<Entity> entities = finder.find(sender);
            List<LivingEntity> livingEntities = entities
                    .stream()
                    .filter(entity -> !entity.hasTag(KILLABLE_TAG) || Boolean.TRUE.equals(entity.getTag(KILLABLE_TAG)))
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> (LivingEntity) entity)
                    .toList();
            long foundEntities = livingEntities.size();
            livingEntities.forEach(LivingEntity::kill);
            sender.sendMessage(Component.textOfChildren(
                    Component.text("Killed"),
                    Component.space(),
                    Component.text(foundEntities, NamedTextColor.GREEN),
                    Component.space(),
                    Component.text(foundEntities == 1 ? "entity." : "entities.")
            ));

        }, TARGET_ARGUMENT);
    }
}
