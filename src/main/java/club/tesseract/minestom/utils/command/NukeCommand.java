package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.condition.Condition;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import club.tesseract.minestom.utils.entity.custom.NukeTNT;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandMetadata(
        categories = {CommandCategory.ADMIN},
        description = "Spawns a tnt which removes a large radius of the blocks around"
)
public class NukeCommand extends Command {

    // 1. Nuke (delete all blocks in x radius), 2. Nuke replace (delete all blocks in x radius, after y seconds replace)
    // animate tnt dropping into the air on cmd run for dramatic effect

    public NukeCommand() {
        super("nuke");

        setCondition(Condition
                .builder(Conditions::playerOnly)
                .and(ExtraConditions.hasPermission("gamesdk.command.nuke"))
                .build());

        setDefaultExecutor(this::nukeDefault);
    }


    void nukeDefault(CommandSender sender, CommandContext context){
        if(!(sender instanceof Player player)){
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        createNukeEntity(player);
    }


    Entity createNukeEntity(Player player){
        // get player forward vector
        Vec forwardVector = player.getPosition().withPitch(-25).direction();
        forwardVector = forwardVector.normalize();

        return createNukeEntity(player.getInstance(), player.getPosition(), forwardVector.mul(10));
    }

    Entity createNukeEntity(@NotNull Instance instance, @NotNull Pos position, @Nullable Vec velocity){
        NukeTNT entity = new NukeTNT(true,60);
        entity.setInstance(instance, position);

        if(velocity != null) {
            entity.setVelocity(velocity);
        }
        return entity;
    }


}
