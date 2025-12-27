package club.tesseract.minestom.utils.entity.custom;

import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.other.PrimedTntMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.batch.BatchOption;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.block.BlockIterator;
import net.minestom.server.utils.time.TimeUnit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NukeTNT extends LivingEntity {

    private final double percentageSubTnt = 0.6;
    @Getter
    private final boolean replace;
    private final int fuseTime;
    private Vec nukeCenter = Vec.ZERO;

    /**
     * @param replace  should replace blocks after explosion
     * @param fuseTime ticks
     */
    public NukeTNT(boolean replace, int fuseTime) {
        super(EntityType.TNT);
        this.replace = replace;
        this.fuseTime = fuseTime;
        int mainFuse = (int) (fuseTime * (1 - percentageSubTnt));
        editEntityMeta(PrimedTntMeta.class, meta -> {
            meta.setFuseTime(mainFuse);// ticks
        });
        getAttribute(Attribute.SCALE).setBaseValue(50);
        scheduleRemove(mainFuse, TimeUnit.SERVER_TICK);
    }

    public NukeTNT() {
        this(false, 60);
    }


    @Override
    protected void despawn() {
        // generate circle, send more tnt
        int subFuse = (int) (fuseTime * percentageSubTnt);
        nukeCenter = this.position.asVec();
        BlockIterator blockIterator = new BlockIterator(this.position, 0, 25);
        boolean found = false;
        while (blockIterator.hasNext() && !found) {
            Point blockPoint = blockIterator.next();
            if (!instance.getBlock(blockPoint).isAir()) {
                found = true;
                nukeCenter = blockPoint.asVec();
            }
        }
        UUID instanceId = instance.getUuid();

        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            Instance instance = MinecraftServer.getInstanceManager().getInstance(instanceId);
            if (instance == null) return TaskSchedule.stop();
            AbsoluteBlockBatch blockBatch = explodeRadius(nukeCenter, 50);
            blockBatch.apply(instance, (inverse) -> {
                if (replace) {
                    MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                        Instance refetchInstance = MinecraftServer.getInstanceManager().getInstance(instanceId);
                        if (refetchInstance == null) return TaskSchedule.stop();
                        inverse.apply(refetchInstance, null);
                        return TaskSchedule.stop();
                    }, TaskSchedule.tick(40));
                }
            });

            return TaskSchedule.stop();
        }, TaskSchedule.tick(subFuse));

        for (int i = 0; i < 8; i++) {
            Entity tinyNuke = new Entity(EntityType.TNT);

            tinyNuke.editEntityMeta(PrimedTntMeta.class, meta -> {
                meta.setFuseTime(subFuse);// ticks
            });

            Vec velocity = new Vec(1, 0, 0);
            Vec finalVel = velocity.rotateFromView(((float) 360 / 8) * i, 0);
            tinyNuke.setInstance(this.getInstance(), this.getPosition());
            tinyNuke.setVelocity(finalVel.normalize().mul(7));
            tinyNuke.scheduleRemove(subFuse, TimeUnit.SERVER_TICK);
        }

    }

    static AbsoluteBlockBatch explodeRadius(Vec explosionLoc, int radius) {
        AbsoluteBlockBatch batch = new AbsoluteBlockBatch(new BatchOption().setCalculateInverse(true));

        Area.Sphere sphere = Area.sphere(explosionLoc, radius);
        sphere.iterator().forEachRemaining(vec -> {
            batch.setBlock(vec, Block.AIR);
        });

        return batch;
    }

}
