package club.tesseract.minestom.test;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ParticleCreatorCommand extends Command {

    private static final ArgumentEnum<ShapeType> PARTICLE_ARGUMENT = ArgumentType.Enum("particle", ShapeType.class);
    private static final ArgumentInteger SIZE = ArgumentType.Integer("size");
    private static final ArgumentLiteral CANCEL = ArgumentType.Literal("cancel");
    private static final ArgumentLiteral POSITION = ArgumentType.Literal("stop");
    private static final AtomicReference<Pos> basePos = new AtomicReference<>();
    private final ArrayList<Task> runningTask = new ArrayList<>();


    public ParticleCreatorCommand() {
        super("particlecreator", "pc");

        addSyntax(this::run, PARTICLE_ARGUMENT);
        addSyntax(this::run, PARTICLE_ARGUMENT, SIZE);
        addSyntax(this::position, POSITION);
        addSyntax(this::cancel, CANCEL);
        setDefaultExecutor(this::run);
    }

    void position(CommandSender sender, CommandContext context){
        if (!(sender instanceof Player player)) return;

        Pos base = player.getPosition().add(0, -0.2 + player.getEyeHeight(), 0);
        basePos.set(base);
        sender.sendMessage("Particle base position set.");
    }

    void cancel(CommandSender sender, CommandContext context){
        runningTask.forEach(Task::cancel);
        runningTask.clear();
    }

    void run(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) return;

        ShapeType shapeType = context.getOrDefault(PARTICLE_ARGUMENT, ShapeType.PENTAGON);
        int size = context.getOrDefault(SIZE, 5);

        Task task = MinecraftServer.getSchedulerManager()
                .buildTask(ParticleCreatorCommand.tickBuilder(player, context, shapeType, size))
                .repeat(TaskSchedule.tick(1))
                .schedule();

        runningTask.add(task);
        sender.sendMessage("Particle task started.");
    }

    enum ShapeType {
        POLYGON_2D,
        PENTAGON,
        SPHERE,
        CUBE,
        CYLINDER,
        CYLINDER_2D,
        TORUS
    }

    static Runnable tickBuilder(Player player, CommandContext context, ShapeType shapeType, double scale) {
        final AtomicInteger frame = new AtomicInteger();
        final double yOffset = -0.2;
        final int particlesPerTick = 360;

        return () -> {
            if (!player.isOnline()) {
                return;
            }

            if(basePos.get() == null) {
                Pos base = player.getPosition().add(0, yOffset + player.getEyeHeight(), 0);
                basePos.set(base);
            }


            Pos base = basePos.get();
            // Draw multiple particles per tick
            for (int i = 0; i < particlesPerTick; i++) {
                int f = frame.getAndIncrement();

                Vec particlePos = switch (shapeType) {
                    case POLYGON_2D -> render2DPolygon(f, scale, 6, 40);
                    case SPHERE -> renderSphere(f, scale);
                    case CUBE -> renderCube(f, scale);
                    case CYLINDER -> renderCylinder(f, scale);
                    case CYLINDER_2D -> render2DCylinder(f, scale);
                    case TORUS -> renderTorus(f, scale, scale * 0.3);
                    case PENTAGON -> pentagonStar(f, scale, 40);
                    default -> Vec.ZERO;
                };

                sendParticle(player, base, particlePos.rotateFromView(base), NamedTextColor.DARK_PURPLE);
            }
        };
    }

    static Vec render2DPolygon(int f, double radius, int sides, int pointsPerEdge) {
        int framesPerLoop = pointsPerEdge * sides;
        f = Math.floorMod(f, framesPerLoop);

        int edge = f / pointsPerEdge;
        int step = f % pointsPerEdge;
        double t = step / (double) pointsPerEdge;

        double angleA = (2 * Math.PI / sides) * edge;
        double angleB = (2 * Math.PI / sides) * (edge + 1);

        double x = radius * ((1 - t) * Math.cos(angleA) + t * Math.cos(angleB));
        double z = radius * ((1 - t) * Math.sin(angleA) + t * Math.sin(angleB));

        return new Vec(x, 0, z);
    }

    static Vec pentagonStar(int f, double radius, int pointsPerEdge) {
        int points = 5;
        int framesPerLoop = pointsPerEdge * points;
        f = Math.floorMod(f, framesPerLoop);

        int edge = f / pointsPerEdge;
        int step = f % pointsPerEdge;
        double t = step / (double) pointsPerEdge;

        double angleA = (2 * Math.PI / points) * edge;
        double angleB = (2 * Math.PI / points) * ((edge + 2) % points);

        double x = radius * ((1 - t) * Math.cos(angleA) + t * Math.cos(angleB));
        double z = radius * ((1 - t) * Math.sin(angleA) + t * Math.sin(angleB));

        return new Vec(x, 0, z);
    }

    static Vec renderSphere(int f, double radius) {
        int pointsPerCircle = 30;
        int numCircles = 15;
        int framesPerLoop = pointsPerCircle * numCircles;

        f = Math.floorMod(f, framesPerLoop);

        int circleIndex = f / pointsPerCircle;
        int pointIndex = f % pointsPerCircle;

        double phi = Math.PI * circleIndex / (numCircles - 1); // 0 to PI
        double theta = 2 * Math.PI * pointIndex / pointsPerCircle; // 0 to 2PI

        double x = radius * Math.sin(phi) * Math.cos(theta);
        double y = radius * Math.cos(phi);
        double z = radius * Math.sin(phi) * Math.sin(theta);

        return new Vec(x, y, z);
    }

    static Vec renderCube(int f, double size) {
        int pointsPerEdge = 20;
        int numEdges = 12;
        int framesPerLoop = pointsPerEdge * numEdges;

        f = Math.floorMod(f, framesPerLoop);

        int edgeIndex = f / pointsPerEdge;
        int step = f % pointsPerEdge;
        double t = step / (double) pointsPerEdge;

        double[][] edgeVertices = {
                // Bottom face
                {-size, -size, -size,  size, -size, -size},
                { size, -size, -size,  size, -size,  size},
                { size, -size,  size, -size, -size,  size},
                {-size, -size,  size, -size, -size, -size},
                // Top face
                {-size,  size, -size,  size,  size, -size},
                { size,  size, -size,  size,  size,  size},
                { size,  size,  size, -size,  size,  size},
                {-size,  size,  size, -size,  size, -size},
                // Vertical edges
                {-size, -size, -size, -size,  size, -size},
                { size, -size, -size,  size,  size, -size},
                { size, -size,  size,  size,  size,  size},
                {-size, -size,  size, -size,  size,  size}
        };

        double[] edge = edgeVertices[edgeIndex];
        double x = (1 - t) * edge[0] + t * edge[3];
        double y = (1 - t) * edge[1] + t * edge[4];
        double z = (1 - t) * edge[2] + t * edge[5];

        return new Vec(x, y, z);
    }

    static Vec renderCylinder(int f, double radius) {
        int pointsPerCircle = 40;
        int numLevels = 20;
        int framesPerLoop = pointsPerCircle * numLevels;

        f = Math.floorMod(f, framesPerLoop);

        int level = f / pointsPerCircle;
        int pointIndex = f % pointsPerCircle;

        double theta = 2 * Math.PI * pointIndex / pointsPerCircle;
        double y = (radius * 2) * (level / (double) (numLevels - 1)) - radius;

        double x = radius * Math.cos(theta);
        double z = radius * Math.sin(theta);

        return new Vec(x, y, z);
    }

    static Vec render2DCylinder(int f, double radius) {
        int pointsPerCircle = 360;
        f = Math.floorMod(f, pointsPerCircle);

        int pointIndex = f % pointsPerCircle;

        double theta = 2 * Math.PI * pointIndex / pointsPerCircle;
        double z = radius * Math.sin(theta);

        double x = radius * Math.cos(theta);

        return new Vec(x, 0, z);
    }

    static Vec renderTorus(int f, double majorRadius, double minorRadius) {
        int pointsPerCircle = 20;
        int numCircles = 30;
        int framesPerLoop = pointsPerCircle * numCircles;

        f = Math.floorMod(f, framesPerLoop);

        int circleIndex = f / pointsPerCircle;
        int pointIndex = f % pointsPerCircle;

        double u = 2 * Math.PI * circleIndex / numCircles;
        double v = 2 * Math.PI * pointIndex / pointsPerCircle;

        double x = (majorRadius + minorRadius * Math.cos(v)) * Math.cos(u);
        double y = minorRadius * Math.sin(v);
        double z = (majorRadius + minorRadius * Math.cos(v)) * Math.sin(u);

        return new Vec(x, y, z);
    }

    static void sendParticle(Player player, Pos base, double x, double y, double z) {
        sendParticle(player, base, x, y, z, NamedTextColor.WHITE);
    }

    static void sendParticle(Player player, Pos base, Vec delta, RGBLike color) {
        sendParticle(player, base, delta.x(), delta.y(), delta.z(), color);
    }

    static void sendParticle(Player player, Pos base, double x, double y, double z, RGBLike color) {
        player.sendPacket(new ParticlePacket(
                Particle.DUST.withColor(color),
                true,
                true,
                base.add(new Vec(x, y, z)),
                Vec.ZERO,
                0f,
                1
        ));
    }



}
