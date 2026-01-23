package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.CommandCategory;
import club.tesseract.minestom.utils.command.CommandMetadata;
import club.tesseract.minestom.utils.command.args.ArgumentSound;
import club.tesseract.minestom.utils.command.args.PlayerArgument;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

@CommandMetadata(
        categories = {CommandCategory.ADMIN, CommandCategory.GENERIC, CommandCategory.VANILLA},
        description = "Plays a sound to a player or players"
)
public class PlaySoundCommand extends Command {

    public static final ArgumentSound SOUND_ARGUMENT = new ArgumentSound("sound");
    public static final ArgumentFloat PITCH_ARGUMENT = ArgumentType.Float("pitch");
    public static final PlayerArgument PLAYER_ARGUMENT = new PlayerArgument("players", false);

    static {
        PITCH_ARGUMENT.setDefaultValue(1.0f);
        PITCH_ARGUMENT.between(-1f, 1f);
        SOUND_ARGUMENT.setDefaultValue(SoundEvent.BLOCK_NOTE_BLOCK_HARP);
    }

    public PlaySoundCommand() {
        super("playsound", "sound");


        setCondition(ExtraConditions.hasPermission("gamesdk.command.playsound"));

        setDefaultExecutor((sender, _) -> sender.sendMessage("Usage: /playsound <sound> <pitch>"));


        addSyntax((sender, context) ->{
            if(!(sender instanceof Player player)){
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            SoundEvent soundEvent = context.get(SOUND_ARGUMENT);
            if (soundEvent == null) {
                player.sendMessage("Unknown sound event. Please provide a valid sound.");
                return;
            }

            player.sendMessage("Playing sound: " + soundEvent.name());
            player.playSound(Sound.sound(soundEvent, Sound.Source.MASTER, 1.0f, 1.0f));
        }, SOUND_ARGUMENT);

        addSyntax((sender, context) ->{
            if(!(sender instanceof Player player)){
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            SoundEvent soundEvent = context.get(SOUND_ARGUMENT);
            if (soundEvent == null) {
                player.sendMessage("Unknown sound event. Please provide a valid sound.");
                return;
            }

            Float pitch = context.get(PITCH_ARGUMENT);
            if (pitch == null) {
                pitch = 1.0f; // Default pitch
            }

            player.sendMessage("Playing sound: " + soundEvent.name());
            player.playSound(Sound.sound(soundEvent, Sound.Source.MASTER, 1.0f, pitch));
        }, SOUND_ARGUMENT, PITCH_ARGUMENT);

        addSyntax((sender, context) ->{
            SoundEvent soundEvent = context.get(SOUND_ARGUMENT);
            if (soundEvent == null) {
                sender.sendMessage("Unknown sound event. Please provide a valid sound.");
                return;
            }

            Float pitch = context.get(PITCH_ARGUMENT);
            if (pitch == null) {
                pitch = 1.0f; // Default pitch
            }

            EntityFinder finder = context.get(PLAYER_ARGUMENT);
            if (finder == null) {
                sender.sendMessage("No players found. Please specify a player.");
                return;
            }

            List<Entity> entities = finder.find(sender);
            if (entities.isEmpty()) {
                sender.sendMessage("No players found. Please specify a player.");
                return;
            }

            for (Entity entity : entities) {
                if (entity instanceof Player targetPlayer) {
                    targetPlayer.playSound(Sound.sound(soundEvent, Sound.Source.MASTER, 1.0f, pitch));
                } else {
                    sender.sendMessage("Entity " + entity.getUuid() + " is not a player.");
                }
            }
            sender.sendMessage("Playing sound: " + soundEvent.name() + " to specified " + entities.size() + " players.");
        }, SOUND_ARGUMENT, PITCH_ARGUMENT, PLAYER_ARGUMENT);

    }


}
