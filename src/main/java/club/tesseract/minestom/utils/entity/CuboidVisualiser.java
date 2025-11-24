package club.tesseract.minestom.utils.entity;

import club.tesseract.minestom.utils.math.position.BoundingBoxes;
import lombok.extern.slf4j.Slf4j;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class CuboidVisualiser extends NoPhysicsEntity {

    private final Area.Cuboid area;

    public CuboidVisualiser(Area.Cuboid area) {
        super(EntityType.BLOCK_DISPLAY);
        this.area = area;
        editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(Block.BLACK_STAINED_GLASS);
            Vec scale = area.max().sub(area.min()).asVec();
            meta.setScale(scale);
        });
    }

    @Override
    public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
        Pos pos = spawnPosition.sub(BoundingBoxes.getCenter(area));
        return super.setInstance(instance, pos);
    }
}
