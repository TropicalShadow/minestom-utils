package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.GenericArguments;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

@CommandMetadata(
        categories = {CommandCategory.PLAYER, CommandCategory.ADMIN},
        description = "Heals yourself or other entity(s)"
)
public class HealCommand extends Command {

    private static final ArgumentEntity ENTITY_ARGUMENT = ArgumentType.Entity("targets").onlyPlayers(false).singleEntity(false);

    public HealCommand() {
        super("heal");

        setCondition(ExtraConditions.hasPermission("gamesdk.command.heal"));

        setDefaultExecutor((sender, _) ->{
            if(!(sender instanceof Player player)){
                sender.sendMessage("/heal <player>");
                return;
            }
            player.heal();
            player.sendMessage(Component.text("You have been healed!", NamedTextColor.GREEN));
        });

        addSyntax((sender, context) -> {
            EntityFinder finder = context.get(ENTITY_ARGUMENT);
            List<Entity> entities = finder.find(sender);
            List<LivingEntity> livingEntity = entities.stream().filter(entity -> entity instanceof LivingEntity).map(entity -> (LivingEntity) entity).toList();

            livingEntity.forEach(LivingEntity::heal);
            sender.sendMessage(Component.text("Healed %s entities(s)".formatted(livingEntity.size()), NamedTextColor.GREEN));
        }, ENTITY_ARGUMENT);

    }
}
