package club.tesseract.minestom.utils.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryOpenEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for chest-style menus shown to one player.
 * <p>
 * Subclasses draw their contents in {@link #render()} with {@link #set(int, ItemStack, ClickHandlers)} and open the
 * menu with {@link #show()}. {@link #refresh()} clears and re-draws.
 * <p>
 * Click model: every click is cancelled while the menu is open, on handled and unhandled menu slots, in the player's
 * own inventory (including shift-click from it), number keys, drag and double click. The handler set for a slot then
 * runs. {@link #allowPlayerInventory(boolean)} lets clicks that stay inside the player's own inventory through; clicks
 * that would move items into or out of the menu stay cancelled.
 * <p>
 * Double clicks are cancelled but run no handler. A vanilla double click also sends two plain left clicks first, so a
 * handler can still run twice in quick succession: keep handlers idempotent (or guard purchases).
 * <p>
 * Lifecycle: the menu-slot click listener lives as long as the inventory and only ever cancels, so the menu's slots
 * can never be taken even if the menu outlives its other listeners. The close listener and the player-inventory
 * listeners are attached when the menu's inventory opens and removed when it closes (by the player, by
 * {@link #close()}, or because the player opened another inventory). {@link #onClose(Player)} runs once each time.
 * A menu belongs to one player: {@link #open(Player)} refuses any other player.
 *
 * @author Mudkjp
 * @see Inventory
 * @since 0.0.1
 */
public abstract class Menu {
    public static final Tag<@NotNull Menu> MENU_TAG = Tag.Transient("menu");

    protected final Player player;
    protected final Inventory inventory;

    private final Map<Integer, ClickHandlers> handlers = new HashMap<>();
    private final EventListener<InventoryPreClickEvent> menuClickListener;
    private final EventListener<InventoryCloseEvent> closeListener;
    private final EventListener<InventoryPreClickEvent> playerInventoryClickListener;
    private final EventListener<InventoryOpenEvent> replacedListener;

    private boolean allowPlayerInventory = false;
    private boolean attached = false;
    private boolean closed = false;
    private boolean closing = false;

    protected Menu(@NotNull Player player, @NotNull InventoryType type, @NotNull String title) {
        this(player, type, Component.translatable(title));
    }

    protected Menu(@NotNull Player player, @NotNull InventoryType type, @NotNull Component title) {
        this.player = player;
        this.inventory = new Inventory(type, title);
        this.inventory.setTag(MENU_TAG, this);

        this.menuClickListener = EventListener.of(InventoryPreClickEvent.class, this::onMenuClick);
        // Never removed: dies with the inventory, so every click on the menu's slots is always cancelled.
        this.inventory.eventNode().addListener(this.menuClickListener);
        this.closeListener = EventListener.of(InventoryCloseEvent.class, event -> {
            if (event.getPlayer() == this.player) detach();
        });
        this.playerInventoryClickListener = EventListener.of(InventoryPreClickEvent.class, this::onPlayerInventoryClick);
        this.replacedListener = EventListener.of(InventoryOpenEvent.class, event -> {
            if (event.getInventory() != this.inventory && !event.isCancelled()) detach();
        });
        // Lives as long as the inventory: re-attaches whenever this inventory is opened, however it is opened.
        this.inventory.eventNode().addListener(InventoryOpenEvent.class, event -> {
            if (event.getPlayer() == this.player && !event.isCancelled()) attach();
        });
    }

    /** Draws the menu. Called by {@link #show()} and {@link #refresh()}; the default draws nothing. */
    protected void render() {
    }

    /**
     * Runs once when the menu stops being shown: closed by the player, by {@link #close()}, or replaced by another
     * inventory. When replaced, it runs while the new inventory's open event is processed, so {@link #isCurrent()} is
     * still true inside this hook. It does not run when the player disconnects.
     */
    protected void onClose(@NotNull Player player) {
    }

    /** Renders the menu, then opens it for its player. */
    public void show() {
        render();
        open(this.player);
    }

    /** Clears every slot and handler, then renders again. */
    public final void refresh() {
        clear();
        render();
    }

    /**
     * Opens the menu for its player.
     *
     * @throws IllegalArgumentException if {@code player} is not the player this menu was built for
     */
    public void open(@NotNull Player player) {
        if (player != this.player) {
            throw new IllegalArgumentException("Menu belongs to " + this.player.getUsername()
                    + ", cannot open it for " + player.getUsername());
        }
        player.openInventory(this.inventory);
    }

    /** True while this menu's inventory is the player's open inventory. */
    public boolean isCurrent() {
        return this.player.getOpenInventory() == this.inventory;
    }

    /** True once the menu was closed or replaced, or {@link #close()} was called; false again if it is reopened. */
    public final boolean isClosed() {
        return this.closed;
    }

    @NotNull
    public final Player getPlayer() {
        return this.player;
    }

    @NotNull
    public final Inventory getInventory() {
        return this.inventory;
    }

    protected final void set(int slot, ItemStack item, ClickHandlers handlers) {
        this.inventory.setItemStack(slot, item);
        this.handlers.put(slot, handlers);
    }

    protected final void set(int slot, ItemStack itemStack, Consumer<Click> handler) {
        this.set(slot, itemStack, ClickHandlers.any(handler));
    }

    protected final void set(int slot, ItemStack itemStack) {
        this.set(slot, itemStack, ClickHandlers.none());
    }

    protected final void add(ItemStack itemStack) {
        this.inventory.addItemStack(itemStack);
    }

    /** Lets clicks that stay inside the player's own inventory through. Off by default. */
    protected final void allowPlayerInventory(boolean allow) {
        this.allowPlayerInventory = allow;
    }

    protected final void clear() {
        this.handlers.clear();
        this.inventory.clear();
    }

    /** Closes the menu on the next tick and removes its listeners. */
    protected void close() {
        this.closed = true;
        if (this.closing) return;
        this.closing = true;
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            this.closing = false;
            this.inventory.getViewers().forEach(Player::closeInventory);
            detach();
            clear();
        });
    }

    private void onMenuClick(InventoryPreClickEvent event) {
        if (event.getInventory() != this.inventory) return;
        event.setCancelled(true);
        ClickHandlers slotHandlers = this.handlers.get(event.getSlot());
        if (slotHandlers != null) slotHandlers.handle(event.getClick());
    }

    // Minestom reports clicks in the player's own inventory on the player inventory, with player-inventory slots,
    // even while another inventory is open.
    private void onPlayerInventoryClick(InventoryPreClickEvent event) {
        if (event.getInventory() != this.player.getInventory() || !isCurrent()) return;
        if (!this.allowPlayerInventory || movesIntoMenu(event.getClick())) event.setCancelled(true);
    }

    private static boolean movesIntoMenu(Click click) {
        return click instanceof Click.LeftShift || click instanceof Click.RightShift || click instanceof Click.Double;
    }

    private void attach() {
        this.closed = false;
        if (this.attached) return;
        this.attached = true;
        this.inventory.eventNode().addListener(this.closeListener);
        this.player.eventNode().addListener(this.playerInventoryClickListener);
        this.player.eventNode().addListener(this.replacedListener);
    }

    private void detach() {
        this.closed = true;
        if (!this.attached) return;
        this.attached = false;
        this.inventory.eventNode().removeListener(this.closeListener);
        this.player.eventNode().removeListener(this.playerInventoryClickListener);
        this.player.eventNode().removeListener(this.replacedListener);
        onClose(this.player);
    }
}
