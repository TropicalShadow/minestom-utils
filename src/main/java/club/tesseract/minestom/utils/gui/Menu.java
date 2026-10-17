package club.tesseract.minestom.utils.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryOpenEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
 * <p>
 * Navigation: {@link #openChild(Menu)} opens a child and suspends this menu (its listeners stay attached and
 * {@link #onClose(Player)} does not run); {@link #back()} re-renders the suspended parent from its in-memory state and
 * shows it again. The chain ends when the player closes the inventory, another inventory replaces a menu in the chain,
 * or a menu is opened with plain {@link #show()}: the player's stack is cleared and every suspended ancestor's
 * {@link #onClose(Player)} runs once. The stack lives on the player (a transient tag), so it goes away with them.
 * <p>
 * Click guard: Minestom delivers a click to whatever inventory is open, so the second click of a fast double click
 * that switched menus lands on the same slot of the next menu. When a menu opens while the player had another library
 * menu open (a child from {@link #openChild(Menu)}, the parent resumed by {@link #back()}, or any menu shown from
 * another menu's click handler), its clicks are ignored for {@link #clickGuardTicks()} ticks of the player's own tick
 * counter ({@link Player#getAliveTicks()}): still cancelled, with no handler run and no player-inventory click let
 * through. Each such open sets a fresh deadline, so the guard cannot get stuck. A menu opened with no inventory
 * open, or over a plain (non-menu) inventory, is not guarded; reopening a menu over itself keeps its deadline.
 * A handler that closes the inventory and then opens the next menu is not guarded (nothing was open at that moment):
 * open the next menu directly instead, without closing first.
 *
 * @author Mudkjp
 * @see Inventory
 * @since 0.0.1
 */
public abstract class Menu {
    public static final Tag<@NotNull Menu> MENU_TAG = Tag.Transient("menu");
    /** The player's navigation stack: menus {@link #back()} can return to. */
    static final Tag<@NotNull NavStack> NAV_TAG = Tag.Transient("menu-nav");
    /**
     * Default length of the menu-to-menu click guard, in ticks of the player's own tick counter
     * ({@link Player#getAliveTicks()}): 5 ticks, a quarter of a second at 20 TPS. See {@link #clickGuardTicks()}.
     * A compile-time constant: code that reads it inlines the value, so recompile consumers when it changes.
     */
    public static final int DEFAULT_CLICK_GUARD_TICKS = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(Menu.class);

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
    /** Covered by a child opened with {@link #openChild(Menu)}: being replaced does not detach it. */
    private boolean suspended = false;
    /** Being opened by {@link #openChild(Menu)} or {@link #back()}: the open keeps the player's stack. */
    private boolean navOpening = false;
    /** Being replaced by its parent in {@link #back()}: the replacement keeps the player's stack. */
    private boolean navLeaving = false;
    private Menu parent;
    /**
     * Clicks are ignored while the player's {@link Player#getAliveTicks()} is below this. Set afresh every time the
     * menu opens: a deadline over another library menu, none from nothing, so it can never stay stuck.
     */
    private long clickGuardUntil = Long.MIN_VALUE;

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
            if (event.getPlayer() != this.player) return;
            detach();
            endChain(null);
        });
        this.playerInventoryClickListener = EventListener.of(InventoryPreClickEvent.class, this::onPlayerInventoryClick);
        this.replacedListener = EventListener.of(InventoryOpenEvent.class, event -> {
            if (event.getInventory() == this.inventory || event.isCancelled() || this.suspended) return;
            detach();
            if (!this.navLeaving) endChain(event.getInventory());
        });
        // Lives as long as the inventory: re-attaches whenever this inventory is opened, however it is opened.
        this.inventory.eventNode().addListener(InventoryOpenEvent.class, event -> {
            if (event.getPlayer() != this.player || event.isCancelled()) return;
            armClickGuard();
            if (!this.navOpening) endChain(this.inventory);
            attach();
        });
    }

    /** Draws the menu. Called by {@link #show()} and {@link #refresh()}; the default draws nothing. */
    protected void render() {
    }

    /**
     * Runs once when the menu stops being shown: closed by the player, by {@link #close()}, or replaced by another
     * inventory. When replaced, it runs while the new inventory's open event is processed, so {@link #isCurrent()} is
     * still true inside this hook. It does not run when the player disconnects, nor while the menu is suspended by
     * {@link #openChild(Menu)}; a suspended menu's runs once when the navigation chain ends without returning to it.
     * <p>
     * Do not open an inventory synchronously from here: when the player closed the menu, Minestom clears the open
     * inventory after this hook returns, so the new one would be lost. Schedule it for the next tick
     * ({@code player.scheduler().scheduleNextTick(...)}).
     */
    protected void onClose(@NotNull Player player) {
    }

    /**
     * Renders the menu, then opens it for its player. Unless the menu is being opened by {@link #openChild(Menu)} or
     * {@link #back()}, this ends the player's navigation chain first, so the menu has no parent.
     */
    public void show() {
        if (!this.navOpening) endChain(this.inventory);
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

    /**
     * Opens {@code child} on top of this menu. This menu is suspended, not closed: {@link #back()} from the child
     * re-renders it and shows it again. The parent must draw in {@link #render()}: that is all {@code back()} re-runs.
     * <p>
     * Ignored, with no state change, unless this menu is the player's open inventory (so a stale or closed menu can
     * never push itself), and when {@code child} is this menu or already in the player's navigation stack (no cycles).
     * If the child does not open (its open is cancelled, or {@code show()} throws), the stack and this menu are left as
     * they were and any exception is rethrown.
     *
     * @throws IllegalArgumentException if {@code child} belongs to another player
     */
    public final void openChild(@NotNull Menu child) {
        if (child.player != this.player) {
            throw new IllegalArgumentException("Child menu belongs to " + child.player.getUsername()
                    + ", not " + this.player.getUsername());
        }
        if (!isCurrent() || child == this) return;
        NavStack stack = this.player.getTag(NAV_TAG);
        if (stack == null) {
            stack = new NavStack();
            this.player.setTag(NAV_TAG, stack);
        } else if (stack.contains(child)) {
            return;
        }
        Menu previousParent = child.parent;
        stack.push(this);
        this.suspended = true;
        child.suspended = false;
        child.parent = this;
        child.navOpening = true;
        try {
            child.show();
        } finally {
            child.navOpening = false;
            if (isCurrent()) { // the child did not open: undo
                stack.pop();
                this.suspended = false;
                child.parent = previousParent;
            }
        }
    }

    /**
     * Returns to the parent: re-renders it with {@link #refresh()} and shows it (the parent must draw in
     * {@link #render()}). Ancestors that were closed while
     * suspended (by {@link #close()}) are skipped and dropped; with no live parent, closes the inventory.
     * Does nothing unless this menu is the player's open inventory, so stale callbacks cannot navigate.
     * If the parent does not open (its open is cancelled, or rendering throws), it stays on the stack, this menu stays
     * open and any exception is rethrown.
     */
    public final void back() {
        if (!isCurrent()) return;
        NavStack stack = this.player.getTag(NAV_TAG);
        Menu target = null;
        if (stack != null) {
            while ((target = stack.pop()) != null) {
                if (target.player == this.player && !target.closed) break;
                target.suspended = false; // already closed, its onClose ran: drop it
                target.parent = null;
            }
        }
        if (target == null) {
            this.player.closeInventory();
            return;
        }
        target.suspended = false;
        this.navLeaving = true;
        target.navOpening = true;
        try {
            target.refresh();
            target.open(this.player);
        } finally {
            this.navLeaving = false;
            target.navOpening = false;
            if (target.isCurrent()) {
                this.parent = null;
            } else { // the parent did not open: stay on this menu
                stack.push(target);
                target.suspended = true;
            }
        }
    }

    /**
     * Runs {@code render} with the future's result on the player's next tick, but only if this menu is still the
     * player's open, unclosed menu then; otherwise the result is dropped silently. A failed future is logged at warn
     * level (under the same rule) and dropped; use {@link #loadAsync(CompletableFuture, Consumer, Consumer)} to handle
     * errors.
     * <p>
     * Results for a menu that is covered or closed are dropped; if the render must always take effect, store the
     * result in your own state and redraw in {@link #render()}.
     */
    public final <R> void loadAsync(@NotNull CompletableFuture<R> future, @NotNull Consumer<R> render) {
        loadAsync(future, render, error -> LOGGER.warn("Async load for menu {} failed", getClass().getName(), error));
    }

    /**
     * Like {@link #loadAsync(CompletableFuture, Consumer)}, but a failed future runs {@code onError} (with the cause
     * unwrapped from {@link CompletionException}) under the same rule: only while this menu is current and not closed.
     * Results for a menu that is covered or closed are dropped; if the render must always take effect, store the
     * result in your own state and redraw in {@link #render()}.
     */
    public final <R> void loadAsync(@NotNull CompletableFuture<R> future, @NotNull Consumer<R> render,
                                    @NotNull Consumer<Throwable> onError) {
        future.whenComplete((value, error) -> this.player.scheduler().scheduleNextTick(() -> {
            if (!isCurrent() || isClosed()) return;
            try {
                if (error != null) {
                    onError.accept(unwrap(error));
                } else {
                    render.accept(value);
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Async load callback for menu {} failed", getClass().getName(), e);
            }
        }));
    }

    static Throwable unwrap(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
        return error;
    }

    /** True when this menu was opened with {@link #openChild(Menu)} and {@link #back()} returns to a parent. */
    public final boolean hasParent() {
        return this.parent != null;
    }

    /**
     * How long, in ticks of the player's own tick counter ({@link Player#getAliveTicks()}), clicks on this menu are
     * ignored after it replaced another library menu (see the class documentation). Read each time the menu opens.
     * Default {@link #DEFAULT_CLICK_GUARD_TICKS}; return 0 (or less) to switch the guard off for this menu.
     */
    protected int clickGuardTicks() {
        return DEFAULT_CLICK_GUARD_TICKS;
    }

    /**
     * True while clicks on this menu are ignored: it replaced another library menu less than
     * {@link #clickGuardTicks()} player ticks ago.
     */
    protected final boolean isClickGuarded() {
        return this.player.getAliveTicks() < this.clickGuardUntil;
    }

    /**
     * The menu below this one in the player's navigation chain: the menu that opened it with {@link #openChild(Menu)}
     * and that {@link #back()} returns to. Null when this menu was not opened as a child, or once the chain ended.
     */
    @Nullable
    protected final Menu parent() {
        return this.parent;
    }

    /** True while this menu's inventory is the player's open inventory. */
    public final boolean isCurrent() {
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

    /**
     * Puts {@code item} on every border slot of the inventory (see {@link Layout#borderSlots()}), with no handler.
     * Call it before placing items: it overwrites the whole border, including the footer row.
     *
     * @throws IllegalArgumentException if the inventory is not a chest type
     */
    protected final void fillBorder(@NotNull ItemStack item) {
        for (int slot : Layout.of(this.inventory.getInventoryType()).borderSlots()) set(slot, item);
    }

    /** Puts {@code item} on every slot that is currently empty (air), with no handler. */
    protected final void fillEmpty(@NotNull ItemStack item) {
        for (int slot = 0; slot < this.inventory.getSize(); slot++) {
            if (this.inventory.getItemStack(slot).isAir()) set(slot, item);
        }
    }

    protected final void add(ItemStack itemStack) {
        this.inventory.addItemStack(itemStack);
    }

    /**
     * Lets clicks that stay inside the player's own inventory through. Off by default. Player-inventory clicks are
     * still frozen while the click guard is active (see {@link #clickGuardTicks()}).
     */
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
        if (isClickGuarded()) return;
        ClickHandlers slotHandlers = this.handlers.get(event.getSlot());
        if (slotHandlers != null) slotHandlers.handle(event.getClick());
    }

    // Minestom reports clicks in the player's own inventory on the player inventory, with player-inventory slots,
    // even while another inventory is open.
    private void onPlayerInventoryClick(InventoryPreClickEvent event) {
        if (event.getInventory() != this.player.getInventory() || !isCurrent()) return;
        if (!this.allowPlayerInventory || movesIntoMenu(event.getClick()) || isClickGuarded()) event.setCancelled(true);
    }

    /**
     * Runs while this menu's open event is processed, when the player's open inventory is still the one being
     * replaced. Over another library menu: a fresh deadline. From nothing or a plain inventory: no guard. Reopened
     * over itself: not a transition, the deadline is kept.
     */
    private void armClickGuard() {
        AbstractInventory previous = this.player.getOpenInventory();
        if (previous == this.inventory) return;
        int ticks = clickGuardTicks();
        boolean overMenu = previous != null && previous.getTag(MENU_TAG) != null;
        this.clickGuardUntil = overMenu && ticks > 0 ? this.player.getAliveTicks() + ticks : Long.MIN_VALUE;
    }

    private static boolean movesIntoMenu(Click click) {
        return click instanceof Click.LeftShift || click instanceof Click.RightShift || click instanceof Click.Double;
    }

    /**
     * Ends the player's navigation chain: empties the stack and closes every suspended ancestor (running its
     * {@link #onClose(Player)} once), except the one whose inventory is {@code opening}, which simply resumes.
     */
    private void endChain(AbstractInventory opening) {
        this.parent = null;
        NavStack stack = this.player.getTag(NAV_TAG);
        if (stack == null) return;
        Menu menu;
        while ((menu = stack.pop()) != null) {
            menu.suspended = false;
            menu.parent = null;
            if (menu.inventory != opening) menu.detach();
        }
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
