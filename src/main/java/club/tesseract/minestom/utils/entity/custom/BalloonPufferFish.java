package club.tesseract.minestom.utils.entity.custom;

import club.tesseract.minestom.utils.entity.ai.EntityTargetSelector;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.EntityAIGroupBuilder;
import net.minestom.server.entity.ai.goal.FollowTargetGoal;
import net.minestom.server.entity.ai.goal.RandomLookAroundGoal;
import net.minestom.server.entity.ai.goal.RandomStrollGoal;
import net.minestom.server.entity.metadata.water.fish.PufferfishMeta;
import net.minestom.server.entity.pathfinding.followers.FlyingNodeFollower;
import net.minestom.server.entity.pathfinding.generators.FlyingNodeGenerator;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.time.Duration;


public class BalloonPufferFish extends EntityCreature {

    public BalloonPufferFish(Player player) {
        super(EntityType.PUFFERFISH);
        super.setNoGravity(true);
        super.hasPhysics = false;
        editEntityMeta(PufferfishMeta.class, meta ->{
            meta.setState(PufferfishMeta.State.FULLY_PUFFED);
        });

        this.setLeashHolder(player);


        addAIGroup(new EntityAIGroupBuilder()
                .addGoalSelector(new RandomStrollGoal(this, 1))
                .addGoalSelector(new RandomLookAroundGoal(this, 10))
                .addGoalSelector(new FollowTargetGoal(this, Duration.ofMillis(500)))
                .addTargetSelector(new EntityTargetSelector(this, player))
                .build());

        this.getNavigator().setNodeGenerator(FlyingNodeGenerator::new);
        this.getNavigator().setNodeFollower(()-> new FlyingNodeFollower(this));
        player.eventNode().addListener(PlayerDisconnectEvent.class, ignored -> {
            this.remove();
        });
    }


    @Override
    protected void remove(boolean permanent) {
        super.remove(permanent);
    }
}