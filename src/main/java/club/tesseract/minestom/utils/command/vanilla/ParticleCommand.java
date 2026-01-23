package club.tesseract.minestom.utils.command.vanilla;

import club.tesseract.minestom.utils.command.args.PlayerArgument;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentParticle;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec3;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.List;

public class ParticleCommand extends Command {

    private static final ArgumentParticle PARTICLE_ARGUMENT = ArgumentType.Particle("particle");
    private static final ArgumentRelativeVec3 POSITION_ARGUMENT = ArgumentType.RelativeVec3("pos");
    private static final ArgumentRelativeVec3 DELTA_ARGUMENT = ArgumentType.RelativeVec3("delta"); // TODO - consider the subtlety to having RelativeVec3 vs Vec3 :think:
    private static final ArgumentInteger COUNT_ARGUMENT = ArgumentType.Integer("count");
    private static final ArgumentFloat SPEED_ARGUMENT = ArgumentType.Float("speed");
    private static final PlayerArgument VIEWERS_ARGUMENT = new PlayerArgument("viewers", false);

    public ParticleCommand() {
        super("particle");

        setCondition(ExtraConditions.orOp(ExtraConditions.hasPermission("minecraft.command.particle")));

        // /particle <particle>
        addSyntax(this::executeSimple, PARTICLE_ARGUMENT);

        // /particle <particle> <pos>
        addSyntax(this::executeWithPos, PARTICLE_ARGUMENT, POSITION_ARGUMENT);

        // /particle <particle> <pos> <delta> <speed> <count>
        addSyntax(this::executeNormal, PARTICLE_ARGUMENT, POSITION_ARGUMENT, DELTA_ARGUMENT, SPEED_ARGUMENT, COUNT_ARGUMENT);

        // /particle <particle> <pos> <delta> <speed> <count> force
        var forceArg = ArgumentType.Literal("force");
        addSyntax(this::executeForce, PARTICLE_ARGUMENT, POSITION_ARGUMENT, DELTA_ARGUMENT, SPEED_ARGUMENT, COUNT_ARGUMENT, forceArg);

        // /particle <particle> <pos> <delta> <speed> <count> force <viewers>
        addSyntax(this::executeForceViewers, PARTICLE_ARGUMENT, POSITION_ARGUMENT, DELTA_ARGUMENT, SPEED_ARGUMENT, COUNT_ARGUMENT, forceArg, VIEWERS_ARGUMENT);

        // /particle <particle> <pos> <delta> <speed> <count> normal
        var normalArg = ArgumentType.Literal("normal");
        addSyntax(this::executeNormal2, PARTICLE_ARGUMENT, POSITION_ARGUMENT, DELTA_ARGUMENT, SPEED_ARGUMENT, COUNT_ARGUMENT, normalArg);

        // /particle <particle> <pos> <delta> <speed> <count> normal <viewers>
        addSyntax(this::executeNormalViewers, PARTICLE_ARGUMENT, POSITION_ARGUMENT, DELTA_ARGUMENT, SPEED_ARGUMENT, COUNT_ARGUMENT, normalArg, VIEWERS_ARGUMENT);
    }

    private void executeSimple(CommandSender sender, CommandContext context) {
        Particle particle = context.get("particle");
        Pos pos = sender instanceof Player ? ((Player) sender).getPosition() : Pos.ZERO;

        Collection<Player> players = getAllPlayers(sender);
        sendParticles(sender, particle, pos, Vec.ZERO, 0.0f, 0, false, players);
    }

    private void executeWithPos(CommandSender sender, CommandContext context) {
        Particle particle = context.get("particle");

        // TODO - consider cmd blocks
        Pos pos = Pos.ZERO;
        if(sender instanceof Player player) {
            pos = context.get(POSITION_ARGUMENT).from(player).asPos();
        }

        Collection<Player> players = getAllPlayers(sender);
        sendParticles(sender, particle, pos, Vec.ZERO, 0.0f, 0, false, players);
    }

    private void executeNormal(CommandSender sender, CommandContext context) {
        executeParticles(sender, context, false, getAllPlayers(sender));
    }

    private void executeForce(CommandSender sender, CommandContext context) {
        executeParticles(sender, context, true, getAllPlayers(sender));
    }

    private void executeForceViewers(CommandSender sender, CommandContext context) {
        List<Player> viewers = VIEWERS_ARGUMENT.getPlayers(sender, context);

        executeParticles(sender, context, true, viewers);
    }

    private void executeNormal2(CommandSender sender, CommandContext context) {
        executeParticles(sender, context, false, getAllPlayers(sender));
    }

    private void executeNormalViewers(CommandSender sender, CommandContext context) {
        List<Player> viewers = VIEWERS_ARGUMENT.getPlayers(sender, context);

        executeParticles(sender, context, false, viewers);
    }

    private void executeParticles(CommandSender sender, CommandContext context, boolean force, Collection<Player> viewers) {
        Particle particle = context.get("particle");
        // TODO - consider cmd blocks
        Pos pos = Pos.ZERO;
        if(sender instanceof Player player) {
            pos = context.get(POSITION_ARGUMENT).from(player).asPos();
        }
        Vec delta = context.get("delta");
        float speed = context.get("speed");
        int count = context.get("count");

        sendParticles(sender, particle, pos, delta, speed, count, force, viewers);
    }

    private void sendParticles(CommandSender sender, Particle particle, Pos pos,
                               Vec delta, float speed, int count, boolean force,
                               Collection<Player> viewers) {
        if (viewers.isEmpty()) {
            sender.sendMessage(Component.translatable("commands.particle.failed"));
            return;
        }

        int result = 0;
        for (Player player : viewers) {
            // In Minestom, we send the particle packet directly
            // The 'force' parameter in vanilla controls render distance
            // Minestom doesn't have this exact concept, but we can still send
            player.sendPacket(new ParticlePacket(
                    particle,
                    force,
                    force,
                    pos.x(), pos.y(), pos.z(),
                    (float) delta.x(), (float) delta.y(), (float) delta.z(),
                    speed,
                    count
            ));
            result++;
        }

        if (result > 0) {
            sender.sendMessage(Component.translatable("commands.particle.success", NamedTextColor.GRAY).arguments(Component.text(particle.key().asString())));
            return;
        }
        sender.sendMessage(Component.translatable("commands.particle.failed", NamedTextColor.RED));
    }

    private Collection<Player> getAllPlayers(CommandSender sender) {
        if (sender instanceof Player player) {
            var instance = player.getInstance();
            if (instance != null) {
                return instance.getPlayers();
            }
        }
        return List.of();
    }
}