package club.tesseract.minestom.utils.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryOpenEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.listener.WindowListener;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket.ClickType;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives real click packets through {@link WindowListener} so the slot mapping (menu slots vs. the player's own
 * inventory, which Minestom reports on the player inventory) is the one a client produces.
 */
@EnvTest
class MenuTest {

    private static final ItemStack STONE = ItemStack.of(Material.STONE);
    private static final ItemStack DIAMOND = ItemStack.of(Material.DIAMOND);
    /** Chest 9x3 has 27 slots; the hotbar follows the 27 main-inventory slots in the window. */
    private static final int HOTBAR_0_WINDOW_SLOT = 27 + 27;

    static final class TestMenu extends Menu {
        Consumer<TestMenu> renderer = m -> {};
        int closes = 0;

        TestMenu(Player player) {
            super(player, InventoryType.CHEST_3_ROW, Component.text("test"));
        }

        @Override
        protected void render() {
            renderer.accept(this);
        }

        @Override
        protected void onClose(Player player) {
            closes++;
        }

        void put(int slot, ItemStack item, ClickHandlers handlers) {
            set(slot, item, handlers);
        }

        void put(int slot, ItemStack item, Consumer<net.minestom.server.inventory.click.Click> handler) {
            set(slot, item, handler);
        }

        void allowPlayerInv(boolean allow) {
            allowPlayerInventory(allow);
        }
    }

    private static Player player(Env env) {
        return env.createPlayer(env.createFlatInstance(), new Pos(0, 42, 0));
    }

    private static void click(Player player, Inventory inventory, int slot, int button, ClickType type) {
        WindowListener.clickWindowListener(new ClientClickWindowPacket(inventory.getWindowId(), 0, (short) slot,
                (byte) button, type, Map.of(), ItemStack.Hash.of(ItemStack.AIR, MinecraftServer.process())), player);
    }

    @Test
    void unhandledSlotClickIsCancelled(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        menu.show();
        menu.getInventory().setItemStack(5, STONE);

        click(player, menu.getInventory(), 5, 0, ClickType.PICKUP);

        assertEquals(STONE, menu.getInventory().getItemStack(5));
        assertTrue(player.getInventory().getCursorItem().isAir());
    }

    @Test
    void shiftClickFromPlayerInventoryIsCancelled(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        menu.show();
        player.getInventory().setItemStack(0, DIAMOND);

        click(player, menu.getInventory(), HOTBAR_0_WINDOW_SLOT, 0, ClickType.QUICK_MOVE);

        assertEquals(DIAMOND, player.getInventory().getItemStack(0));
        for (int i = 0; i < menu.getInventory().getSize(); i++) {
            assertTrue(menu.getInventory().getItemStack(i).isAir(), "menu slot " + i);
        }

        // Opting in lets plain player-inventory clicks through, but still never moves items into the menu.
        menu.allowPlayerInv(true);
        click(player, menu.getInventory(), HOTBAR_0_WINDOW_SLOT, 0, ClickType.QUICK_MOVE);
        assertEquals(DIAMOND, player.getInventory().getItemStack(0));
        click(player, menu.getInventory(), HOTBAR_0_WINDOW_SLOT, 0, ClickType.PICKUP);
        assertEquals(DIAMOND, player.getInventory().getCursorItem());
    }

    @Test
    void playerInventoryClickIsCancelledByDefault(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        menu.show();
        player.getInventory().setItemStack(0, DIAMOND);

        click(player, menu.getInventory(), HOTBAR_0_WINDOW_SLOT, 0, ClickType.PICKUP);

        assertEquals(DIAMOND, player.getInventory().getItemStack(0));
        assertTrue(player.getInventory().getCursorItem().isAir());
    }

    @Test
    void numberKeyAndDragAreCancelled(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        menu.show();
        player.getInventory().setItemStack(0, DIAMOND);

        // Number key 1 over empty menu slot 3 would swap the hotbar item in.
        click(player, menu.getInventory(), 3, 0, ClickType.SWAP);
        assertEquals(DIAMOND, player.getInventory().getItemStack(0));
        assertTrue(menu.getInventory().getItemStack(3).isAir());

        // Left drag of a held stack across menu slots 1 and 2.
        ItemStack held = DIAMOND.withAmount(2);
        player.getInventory().setCursorItem(held);
        click(player, menu.getInventory(), -999, 0, ClickType.QUICK_CRAFT);
        click(player, menu.getInventory(), 1, 1, ClickType.QUICK_CRAFT);
        click(player, menu.getInventory(), 2, 1, ClickType.QUICK_CRAFT);
        click(player, menu.getInventory(), -999, 2, ClickType.QUICK_CRAFT);
        assertTrue(menu.getInventory().getItemStack(1).isAir());
        assertTrue(menu.getInventory().getItemStack(2).isAir());
        assertEquals(held, player.getInventory().getCursorItem());
    }

    @Test
    void rightClickRoutesToRightHandlerElseAny(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        AtomicInteger any0 = new AtomicInteger(), right0 = new AtomicInteger(), shift0 = new AtomicInteger();
        AtomicInteger any1 = new AtomicInteger(), left1 = new AtomicInteger();
        menu.renderer = m -> {
            m.put(0, STONE, ClickHandlers.any(c -> any0.incrementAndGet())
                    .right(c -> right0.incrementAndGet())
                    .shift(c -> shift0.incrementAndGet()));
            m.put(1, STONE, ClickHandlers.any(c -> any1.incrementAndGet()).left(c -> left1.incrementAndGet()));
        };
        menu.show();

        click(player, menu.getInventory(), 0, 1, ClickType.PICKUP);
        assertEquals(1, right0.get());
        assertEquals(0, any0.get());

        click(player, menu.getInventory(), 0, 0, ClickType.PICKUP);
        assertEquals(1, any0.get());

        click(player, menu.getInventory(), 0, 0, ClickType.QUICK_MOVE);
        click(player, menu.getInventory(), 0, 1, ClickType.QUICK_MOVE);
        assertEquals(2, shift0.get());
        assertEquals(1, any0.get());

        click(player, menu.getInventory(), 1, 1, ClickType.PICKUP);
        assertEquals(1, any1.get());
        assertEquals(0, left1.get());
        click(player, menu.getInventory(), 1, 0, ClickType.PICKUP);
        assertEquals(1, left1.get());

        assertEquals(STONE, menu.getInventory().getItemStack(0));
        assertTrue(player.getInventory().getCursorItem().isAir());
    }

    @Test
    void legacyConsumerHandlerStillReceivesClicks(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        AtomicInteger clicks = new AtomicInteger();
        menu.renderer = m -> m.put(4, STONE, c -> clicks.incrementAndGet());
        menu.show();

        click(player, menu.getInventory(), 4, 0, ClickType.PICKUP);
        click(player, menu.getInventory(), 4, 1, ClickType.PICKUP);

        assertEquals(2, clicks.get());
        assertEquals(STONE, menu.getInventory().getItemStack(4));
    }

    @Test
    void listenersRemovedOnInventoryClose(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        menu.renderer = m -> m.put(0, STONE, ClickHandlers.any(c -> {}));
        menu.show();
        assertTrue(menu.getInventory().eventNode().hasListener(InventoryPreClickEvent.class));
        assertFalse(menu.isClosed());

        player.closeInventory();

        assertEquals(1, menu.closes);
        assertTrue(menu.isClosed());
        // The menu-slot click listener lives as long as the inventory (it only ever cancels); the close listener and
        // the player-node listeners are gone.
        assertFalse(menu.getInventory().eventNode().hasListener(InventoryCloseEvent.class));
        assertFalse(player.eventNode().hasListener(InventoryPreClickEvent.class));
        assertFalse(player.eventNode().hasListener(InventoryOpenEvent.class));

        player.closeInventory();
        assertEquals(1, menu.closes);
    }

    @Test
    void cancelledOpenOfAnotherInventoryKeepsOldMenuCancelling(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        AtomicInteger clicks = new AtomicInteger();
        menu.renderer = m -> m.put(0, STONE, ClickHandlers.any(c -> clicks.incrementAndGet()));
        menu.show();
        menu.getInventory().setItemStack(5, STONE);
        Inventory other = new Inventory(InventoryType.CHEST_1_ROW, Component.text("other"));
        // Added to the player node after the menu's own listener, so it cancels after the menu has seen the event.
        player.eventNode().addListener(InventoryOpenEvent.class, e -> {
            if (e.getInventory() == other) e.setCancelled(true);
        });

        player.openInventory(other);

        assertTrue(menu.isCurrent());
        click(player, menu.getInventory(), 5, 0, ClickType.PICKUP);
        assertEquals(STONE, menu.getInventory().getItemStack(5));
        assertTrue(player.getInventory().getCursorItem().isAir());
        click(player, menu.getInventory(), 0, 0, ClickType.PICKUP);
        assertEquals(STONE, menu.getInventory().getItemStack(0));
        assertTrue(player.getInventory().getCursorItem().isAir());
        assertTrue(menu.closes <= 1);
    }

    @Test
    void doubleClickIsCancelledAndRunsNoHandler(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        AtomicInteger clicks = new AtomicInteger();
        menu.renderer = m -> m.put(0, STONE, ClickHandlers.any(c -> clicks.incrementAndGet()));
        menu.show();

        click(player, menu.getInventory(), 0, 0, ClickType.PICKUP_ALL);

        assertEquals(0, clicks.get());
        assertEquals(STONE, menu.getInventory().getItemStack(0));
        assertTrue(player.getInventory().getCursorItem().isAir());
    }

    @Test
    void closeCanRunAgainAfterReopen(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        menu.show();
        menu.close();
        env.tick();
        assertNull(player.getOpenInventory());

        menu.show();
        assertFalse(menu.isClosed());
        menu.close();
        env.tick();

        assertNull(player.getOpenInventory());
        assertEquals(2, menu.closes);
        assertTrue(menu.isClosed());
    }

    @Test
    void openForAnotherPlayerIsRefused(Env env) {
        Player owner = player(env);
        Player other = player(env);
        TestMenu menu = new TestMenu(owner);

        assertThrows(IllegalArgumentException.class, () -> menu.open(other));
        assertNull(other.getOpenInventory());
    }

    @Test
    void reopeningSameMenuKeepsItAttached(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        AtomicInteger clicks = new AtomicInteger();
        menu.renderer = m -> m.put(0, STONE, ClickHandlers.any(c -> clicks.incrementAndGet()));
        menu.show();
        menu.show();

        assertEquals(0, menu.closes);
        assertFalse(menu.isClosed());
        assertTrue(menu.isCurrent());
        click(player, menu.getInventory(), 0, 0, ClickType.PICKUP);
        assertEquals(1, clicks.get());
        player.getInventory().setItemStack(0, DIAMOND);
        click(player, menu.getInventory(), HOTBAR_0_WINDOW_SLOT, 0, ClickType.QUICK_MOVE);
        assertEquals(DIAMOND, player.getInventory().getItemStack(0));
    }

    @Test
    void replacingMenuFromClickHandler(Env env) {
        Player player = player(env);
        TestMenu a = new TestMenu(player);
        TestMenu b = new TestMenu(player);
        AtomicInteger bClicks = new AtomicInteger();
        b.renderer = m -> m.put(1, STONE, ClickHandlers.any(c -> bClicks.incrementAndGet()));
        a.renderer = m -> m.put(0, STONE, ClickHandlers.any(c -> b.show()));
        a.show();

        click(player, a.getInventory(), 0, 0, ClickType.PICKUP);

        assertEquals(1, a.closes);
        assertTrue(a.isClosed());
        assertTrue(b.isCurrent());
        assertEquals(STONE, a.getInventory().getItemStack(0));
        click(player, b.getInventory(), 1, 0, ClickType.PICKUP);
        assertEquals(1, bClicks.get());
        assertEquals(STONE, b.getInventory().getItemStack(1));
        assertTrue(player.getInventory().getCursorItem().isAir());
        assertEquals(1, a.closes);
        assertEquals(0, b.closes);
    }

    @Test
    void refreshFromInsideClickHandler(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        AtomicInteger clicks = new AtomicInteger();
        menu.renderer = m -> m.put(0, STONE.withAmount(clicks.get() + 1),
                ClickHandlers.any(c -> { clicks.incrementAndGet(); m.refresh(); }));
        menu.show();

        click(player, menu.getInventory(), 0, 0, ClickType.PICKUP);
        click(player, menu.getInventory(), 0, 0, ClickType.PICKUP);

        assertEquals(2, clicks.get());
        assertEquals(3, menu.getInventory().getItemStack(0).amount());
        assertTrue(player.getInventory().getCursorItem().isAir());
    }

    @Test
    void closeRunsCleanupOnce(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        menu.show();

        menu.close();
        assertTrue(menu.isClosed());
        env.tick();

        assertNull(player.getOpenInventory());
        assertEquals(1, menu.closes);
        assertFalse(menu.getInventory().eventNode().hasListener(InventoryCloseEvent.class));
        assertFalse(player.eventNode().hasListener(InventoryPreClickEvent.class));
    }

    @Test
    void refreshRerendersWithoutDuplicateListeners(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        AtomicInteger clicks = new AtomicInteger();
        AtomicInteger renders = new AtomicInteger();
        menu.renderer = m -> {
            renders.incrementAndGet();
            m.put(0, STONE, ClickHandlers.any(c -> clicks.incrementAndGet()));
        };
        menu.show();

        menu.refresh();
        menu.refresh();
        menu.refresh();
        click(player, menu.getInventory(), 0, 0, ClickType.PICKUP);

        assertEquals(4, renders.get());
        assertEquals(1, clicks.get());
    }

    @Test
    void isCurrentFalseAfterAnotherInventoryOpens(Env env) {
        Player player = player(env);
        TestMenu menu = new TestMenu(player);
        assertFalse(menu.isCurrent());
        menu.show();
        assertTrue(menu.isCurrent());

        player.openInventory(new Inventory(InventoryType.CHEST_1_ROW, Component.text("other")));

        assertFalse(menu.isCurrent());
        assertTrue(menu.isClosed());
        assertEquals(1, menu.closes);
        assertFalse(menu.getInventory().eventNode().hasListener(InventoryCloseEvent.class));
        assertFalse(player.eventNode().hasListener(InventoryPreClickEvent.class));
    }
}
