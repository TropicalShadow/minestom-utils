package club.tesseract.minestom.utils.entity;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a bounding box visualiser
 * @author TropicalShadow
 * @since 0.0.1
 * @see NoPhysicsEntity
 * @see BlockDisplayMeta
 * @see BoundingBox
 */
public class BoundingBoxVisualiser extends NoPhysicsEntity {

    private final BoundingBox box;

    public BoundingBoxVisualiser(BoundingBox box) {
        super(EntityType.BLOCK_DISPLAY);
        this.box = box;

        this.editEntityMeta(BlockDisplayMeta.class, meta->{
            meta.setBlockState(Block.RED_STAINED_GLASS);
            meta.setScale(new Vec(box.width(), box.height(), box.depth()));
        });

        setBoundingBox(box);
    }

    @Override
    public @NotNull CompletableFuture<Void> setInstance(@NotNull Instance instance) {
        return super.setInstance(instance, box.relativeStart());
    }
}
