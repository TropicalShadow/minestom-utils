package club.tesseract.minestom.utils.entity.ai.goal;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.GoalSelector;

public final class LookAtPlayerGoal extends GoalSelector {
    private Entity target;

    public LookAtPlayerGoal(EntityCreature entityCreature) {
        super(entityCreature);
    }

    @Override
    public boolean shouldStart() {
        target = findTarget();
        return target != null;
    }

    @Override
    public void start() {
    }

    @Override
    public void tick(long time) {
        if (entityCreature.getDistanceSquared(target) > 225 ||
                entityCreature.getInstance() != target.getInstance()) {
            target = null;
            return;
        }

        entityCreature.lookAt(target);
    }

    @Override
    public boolean shouldEnd() {
        return target == null;
    }

    @Override
    public void end() {
    }
}

