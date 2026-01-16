package club.tesseract.minestom.utils.entity.npc;

import club.tesseract.minestom.utils.entity.ai.goal.LookAtPlayerGoal;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDespawnEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.EntityEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public abstract class NPC extends EntityCreature {
    public static final Tag<@NotNull Consumer<InteractEvent<EntityAttackEvent>>> LEFT_CLICK_TAG = Tag.Transient("left-click-consumer");
    public static final Tag<@NotNull Consumer<InteractEvent<PlayerEntityInteractEvent>>> RIGHT_CLICK_TAG = Tag.Transient("right-click-consumer");

    private static final EventNode<EntityEvent> EVENT_NODE = EventNode.type("npc-events", EventFilter.ENTITY);
    private static final HashMap<Integer, NPC> NON_PLAYABLE_CHARACTERS = new HashMap<>();
    private static final Sound DEFAULT_SOUND = Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(2).build();
    private final Tag<Sound> INTERACT_SOUND_TAG = Tag.Transient("npc-interact-sound");

    static {
        EVENT_NODE.addListener(EntityDespawnEvent.class, event -> {
            if (!(event.getEntity() instanceof NPC npc)) return;
            if(!NON_PLAYABLE_CHARACTERS.containsKey(npc.getEntityId())){
                return;
            }
            NON_PLAYABLE_CHARACTERS.remove(npc.getEntityId());
        });

        EVENT_NODE.addListener(EntityAttackEvent.class, event -> {
            if (!(event.getEntity() instanceof Player)) return;
            if (!(event.getTarget() instanceof NPC npc)) return;
            if(!NON_PLAYABLE_CHARACTERS.containsKey(event.getTarget().getEntityId())){
                return;
            }
            npc.handle(event);
        });

        EVENT_NODE.addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getHand() == PlayerHand.OFF) return; // avoid duplicate call
            if (!(event.getTarget() instanceof NPC npc)) return;
            if(!NON_PLAYABLE_CHARACTERS.containsKey(event.getTarget().getEntityId())){
                return;
            }
            npc.handle(event);
        });
    }

    public NPC(EntityType entityType) {
        super(entityType);
    }


    public Component getName(){
        return this.get(DataComponents.CUSTOM_NAME);
    }

    public void lookClose(){
        lookClose(15);
    }

    public void lookClose(int range){
        addAIGroup(
                List.of(new LookAtPlayerGoal(this)),
                List.of(new ClosestEntityTarget(this, range, entity -> entity instanceof Player))
        );
    }

    public void speak(Audience audience, Component text) {
        Component message = Component.textOfChildren(
                getName().colorIfAbsent(NamedTextColor.YELLOW),
                Component.space(),
                text.colorIfAbsent(NamedTextColor.GRAY)
        );

        audience.sendMessage(message);
    }

    public void setInteract(@Nullable Consumer<InteractEvent<PlayerEntityInteractEvent>> consumer){
        if(consumer == null) {
            this.removeTag(RIGHT_CLICK_TAG);
            return;
        }
        this.setTag(RIGHT_CLICK_TAG, consumer);
    }

    public void setAttack(@Nullable Consumer<InteractEvent<EntityAttackEvent>> consumer){
        if(consumer == null) {
            this.removeTag(LEFT_CLICK_TAG);
            return;
        }
        this.setTag(LEFT_CLICK_TAG, consumer);
    }

    public void setInteractSound(@Nullable Sound sound){
        if(sound == null) {
            this.removeTag(INTERACT_SOUND_TAG);
            return;
        }
        this.setTag(INTERACT_SOUND_TAG, sound);
    }

    public void handle(@NotNull EntityAttackEvent event) {
        if (event.getTarget() != this) return;
        if (!(event.getEntity() instanceof Player player)) return;


        final Sound playerSound = this.getTag(INTERACT_SOUND_TAG.defaultValue(DEFAULT_SOUND));
        player.playSound(playerSound, event.getTarget());

        if (this.hasTag(LEFT_CLICK_TAG))
            this.getTag(LEFT_CLICK_TAG).accept(new InteractEvent<>(event, this, player));
    }

    public void handle(@NotNull PlayerEntityInteractEvent event) {
        if (event.getTarget() != this) return;
        if (event.getHand() != PlayerHand.MAIN) return; // Prevent duplicating event
        final Player player = event.getPlayer();


        player.playSound(Sound.sound()
                .type(SoundEvent.BLOCK_NOTE_BLOCK_PLING)
                .pitch(2)
                .build(), event.getTarget());

        if (this.hasTag(RIGHT_CLICK_TAG))
            this.getTag(RIGHT_CLICK_TAG).accept(new InteractEvent<>(event, this, player));
    }

    @Override
    public CompletableFuture<Void> setInstance(@NonNull Instance instance, @NonNull Pos spawnPosition) {
        register();
        return super.setInstance(instance, spawnPosition);
    }

    public void register(){
        register(this);
    }

    /**
     * Register an event to track interactions with NPCs.
     *
     * @see #unregister(NPC...)
     * @param npcs a list of NPCs to register
     */
    public static void register(NPC... npcs){
        if(EVENT_NODE.getParent() == null){
            GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();
            eventHandler.addChild(EVENT_NODE);
        }

        for (NPC npc : npcs) {
            if(NON_PLAYABLE_CHARACTERS.containsKey(npc.getEntityId())){
                log.warn("NPC {} already registered", npc.getEntityId());
                continue;
            }
            NON_PLAYABLE_CHARACTERS.put(npc.getEntityId(), npc);
        }
    }

    /**
     * Removes the NPC from the registry
     *
     * @see #register(NPC...)
     * @param npcs a list of NPCs to unregister
     */
    public static void unregister(NPC... npcs){
        for (NPC npc : npcs) {
            NON_PLAYABLE_CHARACTERS.remove(npc.getEntityId());
        }
    }

    public static Collection<NPC> getNPCs(){
        return Collections.unmodifiableCollection(NON_PLAYABLE_CHARACTERS.values());
    }

    public record InteractEvent<T extends EntityInstanceEvent>(
            @NotNull T event,
            @NotNull NPC npc,
            @NotNull Player interacted
    ) {

    }
}

