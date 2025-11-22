package club.tesseract.minestom.test;

import club.tesseract.minestom.utils.entity.BoundingBoxVisualiser;
import club.tesseract.minestom.utils.entity.MarkerData;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentEntityType;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.component.CustomData;

public class DebugCommand extends Command {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DebugCommand.class);
    private static final ArgumentEntityType ENTITY_TYPE = new ArgumentEntityType("entityType");

    public DebugCommand() {
        super("debug", "db");

        setDefaultExecutor(this::run);
        addSyntax((sender, context) -> {
            if(!(sender instanceof Player player))return;
            EntityType entityType = context.get(ENTITY_TYPE);
            EntityF entity = new EntityF(entityType);
            entity.setInstance(player.getInstance(), player.getPosition());
        }, ENTITY_TYPE);
    }


    void run(CommandSender sender, CommandContext context){
        if(!(sender instanceof Player player))return;

        InstanceContainer container = (InstanceContainer) player.getInstance();
        container.getEntities().stream()
                .filter(entity -> entity.getEntityType() == EntityType.MARKER)
                .forEach(entity -> {
            MarkerData data = MarkerData.createFrom(entity);
            if(data.name() == null)return;
            CustomData cdata = entity.get(DataComponents.CUSTOM_DATA);
            String internalData = cdata.nbt().getString("data");
            sender.sendMessage(internalData);
        });

        player.sendMessage("Debug command ran");
    }

    class EntityF extends Entity {
        public EntityF(EntityType entityType) {
            super(entityType);

            this.metadata.getEntries().forEach((id, entry) -> {
                log.info(id + " " + entry.getClass().getSimpleName());
            });
        }
    }
}
