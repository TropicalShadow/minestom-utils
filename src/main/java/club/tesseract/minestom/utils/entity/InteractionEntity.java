package club.tesseract.minestom.utils.entity;

import club.tesseract.minestom.utils.math.position.BoundingBoxes;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;


public class InteractionEntity extends NoPhysicsEntity{

    private final EventNode<@NotNull PlayerEvent> PLAYER_MOVE_EVENT_NODE = EventNode.type("interaction-entity-player-move", EventFilter.PLAYER);

    public InteractionEntity(@NotNull BoundingBox boundingBox) {
        super(EntityType.INTERACTION);
        super.setBoundingBox(boundingBox);
    }

    void onEnter(Player player) {
        player.sendMessage("entered");
    }

    void onExit(Player player) {
        player.sendMessage("exited");
    }

    @Override
    public void spawn() {
        PLAYER_MOVE_EVENT_NODE.addListener(PlayerMoveEvent.class, event ->{
            Pos oldPos = event.getPlayer().getPosition();
            Pos newPos = event.getNewPosition();
            ///  TODO - figure this shit out
            BoundingBox boundingBox = this.getBoundingBox();
            BoundingBox playerBoundingBox = event.getPlayer().getBoundingBox();

            boolean intersects = BoundingBoxes.touchesOrOverlaps(boundingBox, Vec.ZERO, playerBoundingBox, newPos);
            boolean previouslyIntersected = BoundingBoxes.touchesOrOverlaps(boundingBox, Vec.ZERO, playerBoundingBox, oldPos);
            if(intersects && !previouslyIntersected) {
                this.onEnter(event.getPlayer());
            }
            else if(!intersects && previouslyIntersected) {
                this.onExit(event.getPlayer());
            }
        });
        MinecraftServer.getGlobalEventHandler().addChild(PLAYER_MOVE_EVENT_NODE);
    }

    @Override
    protected void despawn() {
        MinecraftServer.getGlobalEventHandler().removeChild(PLAYER_MOVE_EVENT_NODE);
    }
}
