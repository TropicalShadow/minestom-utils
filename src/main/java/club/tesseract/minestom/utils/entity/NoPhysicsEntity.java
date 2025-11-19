package club.tesseract.minestom.utils.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Entity that does not have physics.
 * @author TropicalShadow
 * @since 0.0.1
 * @see Entity
 */
public class NoPhysicsEntity extends Entity {

    public NoPhysicsEntity(@NotNull EntityType type) {
        super(type);

        super.setNoGravity(true);
        super.hasPhysics = false;
    }
}