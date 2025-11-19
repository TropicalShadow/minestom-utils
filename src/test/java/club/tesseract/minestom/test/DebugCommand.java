package club.tesseract.minestom.test;

import club.tesseract.minestom.utils.entity.custom.BalloonPufferFish;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;

public class DebugCommand extends Command {

    public DebugCommand() {
        super("debug", "db");

        setDefaultExecutor(this::run);
    }


    void run(CommandSender sender, CommandContext context){
        if(!(sender instanceof Player player))return;

        BalloonPufferFish pufferFish = new BalloonPufferFish(player);
        pufferFish.setInstance(player.getInstance(), player.getPosition());
        player.sendMessage("Debug command ran");
    }
}
