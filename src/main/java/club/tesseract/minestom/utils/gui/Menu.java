package club.tesseract.minestom.utils.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Utility class for creating menus
 * {@link Menu#close()} should be called when the menu is no longer needed to prevent memory leaks
 * @author Mudkjp
 * @see Inventory
 * @since 0.0.1
 */
public abstract class Menu {
    public static final Tag<@NotNull Menu> MENU_TAG = Tag.Transient("menu");

    protected final Player player;
    protected final Inventory inventory;
    protected final List<EventListener<? extends @NotNull InventoryEvent>> listeners;
    protected final HashMap<Integer, EventListener<? extends @NotNull InventoryEvent>> clickHandlers = new HashMap<>();
    private transient boolean closing = false;

    protected Menu(@NotNull Player player, @NotNull InventoryType type, @NotNull String title) {
        this(player, type, Component.translatable(title));
    }

    protected Menu(@NotNull Player player, @NotNull InventoryType type, @NotNull Component title) {
        this.player = player;
        this.inventory = new Inventory(type, title);
        this.listeners = new ArrayList<>();
        this.inventory.setTag(MENU_TAG, this);
    }

    public void open(@NotNull Player player) {
        player.openInventory(this.inventory);
    }

    @NotNull
    public final Player getPlayer() {
        return this.player;
    }

    @NotNull
    public final Inventory getInventory() {
        return this.inventory;
    }

    protected final void set(int slot, ItemStack itemStack, BiConsumer<Player, Click> handler) {
        this.inventory.setItemStack(slot, itemStack);
        if(this.clickHandlers.containsKey(slot)) {
            var oldListener = this.clickHandlers.get(slot);
            this.inventory.eventNode().removeListener(oldListener);
            this.listeners.remove(oldListener);
        }
        var listener = this.eventListener(slot, handler);
        this.clickHandlers.put(slot, listener);
        this.inventory.eventNode().addListener(listener);
        this.listeners.add(listener);
    }

    protected final void set(int slot, ItemStack itemStack) {
        this.set(slot, itemStack, (_, _) -> {});
    }

    protected final void add(ItemStack itemStack) {
        this.inventory.addItemStack(itemStack);
    }

    protected final void clear() {
        this.listeners.forEach(this.inventory.eventNode()::removeListener);
        this.inventory.clear();
    }

    protected void close() {
        if(this.closing)return;
        this.closing = true;
        MinecraftServer.getSchedulerManager().scheduleNextTick(() ->{
            this.inventory.getViewers().forEach(Player::closeInventory);
            clear();
        });
    }

    private EventListener<@NotNull InventoryPreClickEvent> eventListener(int slot, BiConsumer<Player, Click> handler) {
        return EventListener.of(InventoryPreClickEvent.class, event -> {
            if (event.getSlot() == slot) {
                event.setCancelled(true);
                handler.accept(event.getPlayer(), event.getClick());
            }
        });
    }
}