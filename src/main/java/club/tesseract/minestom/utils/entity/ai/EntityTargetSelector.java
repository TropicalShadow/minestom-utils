package club.tesseract.minestom.utils.entity.ai;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.TargetSelector;
import org.jetbrains.annotations.Nullable;

public class EntityTargetSelector extends TargetSelector {

    private final Entity target;

    public EntityTargetSelector(EntityCreature entityCreature, Entity target) {
        super(entityCreature);
        this.target = target;
    }

    @Override
    public @Nullable Entity findTarget() {
        return target;
    }
}
