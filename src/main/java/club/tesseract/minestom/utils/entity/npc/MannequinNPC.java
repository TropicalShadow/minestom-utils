package club.tesseract.minestom.utils.entity.npc;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MannequinNPC extends NPC {

    private static final Tag<@NotNull Boolean> HAS_PREFIX = Tag.Boolean("has-prefix");
    private static final Component NPC_TAG = Component.text("[NPC]", NamedTextColor.YELLOW);

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

    public MannequinNPC clone(){
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


    public void copyOf(Player player){
        setSkin(player.getSkin());
        setName(player.getName());
    }

    public void setName(Component name){
        setName(name, null);
    }

    public void setNPCPrefix(boolean activate){
        setTag(HAS_PREFIX, activate);
        Component component = get(DataComponents.CUSTOM_NAME);
        Component description = ((MannequinMeta)this.getEntityMeta()).getDescription();

        setName(activate ? component : description, activate ? NPC_TAG : component);
    }

    public void setName(Component name, @Nullable Component above){
        setCustomNameVisible(true);
        this.editEntityMeta(MannequinMeta.class, meta->{
            meta.setDescription(above == null ? null : name);
        });

        boolean hasPrefix;
        if(hasTag(HAS_PREFIX)){
            hasPrefix = getTag(HAS_PREFIX);
        }else{
            hasPrefix = false;
        }

        set(DataComponents.CUSTOM_NAME,  hasPrefix ? NPC_TAG : above == null ? name : above);
    }

    public void setSkin(@NotNull ResolvableProfile profile){
        editEntityMeta(MannequinMeta.class, meta-> meta.setProfile(profile));
    }

    public void setSkin(@NotNull PlayerSkin skin){
        setSkin(new ResolvableProfile(skin));
    }

    @Override
    public Component getName(){
        if(hasTag(HAS_PREFIX) && getTag(HAS_PREFIX)){
            return ((MannequinMeta)this.getEntityMeta()).getDescription();
        }
        return get(DataComponents.CUSTOM_NAME);
    }

    public void updateNPCForAudience(Audience audience, Component displayName, @Nullable Component aboveName, PlayerSkin playerSkin){
        Map<Integer, Metadata.Entry<?>> entries = Map.<Integer, Metadata.Entry<?>>of(
                MetadataDef.Mannequin.DESCRIPTION.index(), Metadata.OptComponent(displayName),
                MetadataDef.Player.CUSTOM_NAME.index(), Metadata.OptComponent(aboveName),
                MetadataDef.Player.CUSTOM_NAME_VISIBLE.index(), Metadata.Boolean(true),
                MetadataDef.Mannequin.PROFILE.index(), Metadata.ResolvableProfile(new ResolvableProfile(playerSkin))
        );


        PacketSendingUtils.sendPacket(audience, new EntityMetaDataPacket(this.getEntityId(), entries));
    }

}
