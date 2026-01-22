package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentItemStack;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Equippable;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GiveItemCommand extends Command {


    private static final ArgumentEntity TARGETS_ARGUMENT = ArgumentType.Entity("targets").onlyPlayers(false).singleEntity(false);
    private static final ArgumentItemStack ITEM_MATERIAL_ARGUMENT = ArgumentType.ItemStack("item");
    private static final ArgumentInteger COUNT_ARGUMENT = ArgumentType.Integer("count");

    public GiveItemCommand() {
        super("give");

        setCondition(ExtraConditions.hasPermission("minecraft.command.give"));

        setDefaultExecutor((sender, _) -> sender.sendMessage("Usage: /give <targets> <item> [count]"));

        addSyntax((sender, context) -> {
            final EntityFinder finder = context.get(TARGETS_ARGUMENT);
            final ItemStack itemStack = context.get(ITEM_MATERIAL_ARGUMENT);

            giveItems(sender, finder, itemStack.withAmount(1));
        }, TARGETS_ARGUMENT, ITEM_MATERIAL_ARGUMENT);

        addSyntax((sender, context) -> {
            final EntityFinder finder = context.get(TARGETS_ARGUMENT);
            final ItemStack itemStack = context.get(ITEM_MATERIAL_ARGUMENT);
            final int count = context.get(COUNT_ARGUMENT);

            giveItems(sender, finder, itemStack.withAmount(count));
        });
    }


    private void giveItems(@NotNull CommandSender sender, @NotNull EntityFinder finder, @NotNull ItemStack itemStack){
        List<Entity> entities = finder.find(sender);
        long success = entities
                .stream()
                .filter(entity -> entity instanceof EquipmentHandler)
                .filter(entity -> giveItem(entity, itemStack))
                .count();

        if (entities.isEmpty()) {
            sender.sendMessage("No entities found");
            return;
        }

        if (success == 0) {
            sender.sendMessage("No valid targets found.");
            return;
        }

        boolean isMultiple = entities.size() > 1;
        Entity first = entities.getFirst();

        Component response = Component.textOfChildren(
                Component.text("Given "),
                isMultiple ? Component.textOfChildren(
                        Component.text(success),
                        Component.text("/"),
                        Component.text(entities.size()),
                        Component.space(),
                        Component.text("players")
                ) : getName(first),
                Component.space(),
                getName(itemStack)
        );

        sender.sendMessage(response);
    }

    /**
     * Get username / display name if player,
     * Get custom name / entity name if entity
     *
     * @param entity named creature
     * @return a component with the entity name
     */
    private Component getName(Entity entity) {
        if (entity instanceof Player player) {
            return player.getName();
        }
        if (entity.has(DataComponents.CUSTOM_NAME)) {
            return entity.get(DataComponents.CUSTOM_NAME);
        }
        return Component.text(entity.getEntityType().name());
    }

    private Component getName(ItemStack itemStack) {
        if (itemStack.has(DataComponents.CUSTOM_NAME)) {
            return itemStack.get(DataComponents.CUSTOM_NAME);
        }
        final Key key = itemStack.material().key();
        return Component.translatable("item." + key.value() + ".name");
    }

    /**
     * @param target    entity with inventory or EquipmentHandler
     * @param itemStack item to provide
     * @apiNote Entity must extend {@link EquipmentHandler}
     */
    boolean giveItem(Entity target, ItemStack itemStack) {
        if(target instanceof Player player){
            boolean success = player.getInventory().addItemStack(itemStack);
            if(success) return true;
        }

        if(target instanceof EquipmentHandler equipmentHandler){
            if(itemStack.has(DataComponents.EQUIPPABLE)){
                Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
                assert equippable != null;
                if(equippable.slot() != EquipmentSlot.MAIN_HAND && equippable.slot() != EquipmentSlot.OFF_HAND){
                    ItemStack activeEquipment = equipmentHandler.getEquipment(equippable.slot());
                    if(activeEquipment.isAir()){
                        equipmentHandler.setEquipment(equippable.slot(), itemStack);
                        return true;
                    }
                }
            }

            if(equipmentHandler.getItemInMainHand().isAir()){
                equipmentHandler.setItemInMainHand(itemStack);
                return true;
            }
            if(equipmentHandler.getItemInOffHand().isAir()){
                equipmentHandler.setItemInOffHand(itemStack);
                return true;
            }
        }

        return false;
    }
}
