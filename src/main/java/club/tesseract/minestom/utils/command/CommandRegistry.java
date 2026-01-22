package club.tesseract.minestom.utils.command;

import net.minestom.server.MinecraftServer;

public final class CommandRegistry {

    private CommandRegistry() {}


    public static void registerServer(){
        MinecraftServer.getCommandManager().register(
                new StatsCommand(),
                new StopCommand(),
                new SudoCommand(),
                new TpsCommand()
        );
    }

    public static void registerPlayer(){
        MinecraftServer.getCommandManager().register(
                new TeleportCommand(),
                new GameModeCommand(),
                new FlyCommand(),
                new HealCommand(),
                new NukeCommand(),
                new PlaySoundCommand(),
                new GiveItemCommand(),
                new ClearItemCommand()
        );
    }

    public static void registerAll(){
        registerServer();
        registerPlayer();
    }



}
