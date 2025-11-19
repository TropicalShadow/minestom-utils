package club.tesseract.minestom.utils.entity;

import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.network.player.ResolvableProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a mannequin npc
 * @author TropicalShadow
 * @since 0.0.1
 * @see EntityCreature
 * @see MannequinMeta
 * @see EntityType#MANNEQUIN
 */
public class MannequinNPC extends EntityCreature {
    public MannequinNPC() {
        super(EntityType.MANNEQUIN);
        this.editEntityMeta(MannequinMeta.class, meta->{
            meta.setImmovable(true);
        });
    }

    public MannequinNPC(@Nullable ResolvableProfile skin, @Nullable Component name) {
        this();
        if(skin != null) setSkin(skin);
        if(name != null) setName(name);
    }

    public void copyOf(@NotNull Player player){
        setSkin(player.get(DataComponents.PROFILE));
        setName(player.getName());
    }

    public void setName(@NotNull Component name){
        setName(name, null);
    }

    public void setName(@NotNull Component name, @Nullable Component above){
        setCustomNameVisible(true);
        this.editEntityMeta(MannequinMeta.class, meta->{
            meta.setDescription(above == null ? null : name);
        });

        set(DataComponents.CUSTOM_NAME, above == null ? name : above);
    }

    public void setSkin(@Nullable ResolvableProfile profile){
        this.editEntityMeta(MannequinMeta.class, meta-> meta.setProfile(Objects.requireNonNullElse(profile, ResolvableProfile.EMPTY)));
    }

    public MannequinNPC duplicate(){
        AtomicReference<MannequinNPC> npc = new AtomicReference<>();
        this.editEntityMeta(MannequinMeta.class, meta->{
            MannequinNPC mannequinNPC = new MannequinNPC(meta.getProfile(), get(DataComponents.CUSTOM_NAME));
            mannequinNPC.editEntityMeta(MannequinMeta.class, meta1->{
                meta1.setImmovable(meta.isImmovable());
                meta1.setDescription(meta.getDescription());
            });
            npc.set(mannequinNPC);
        });

        MannequinNPC mannequin = npc.get();
        mannequin.tagHandler().updateContent(this.tagHandler().asCompound());

        return mannequin;
    }
}
