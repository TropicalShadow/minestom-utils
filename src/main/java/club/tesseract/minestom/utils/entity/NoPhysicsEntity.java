package club.tesseract.minestom.utils.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Entity that does not have physics.
 * @author TropicalShadow
 * @since 0.0.1
 * @see Entity
 */
public class NoPhysicsEntity extends Entity {


    public NoPhysicsEntity(@NotNull EntityType type, @NotNull UUID uuid) {
        super(type, uuid);
        super.setNoGravity(true);
        super.hasPhysics = false;
    }

    public NoPhysicsEntity(@NotNull EntityType type) {
        this(type, UUID.randomUUID());
    }
}