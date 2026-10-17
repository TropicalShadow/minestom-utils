package club.tesseract.minestom.utils.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The menu-to-menu click guard: a stray second click of a fast double click must not land on the next menu's slot.
 * Clicks are real packets through {@link WindowListener}; time is the player's own tick counter, advanced by
 * {@code env.tick()}.
 */
@EnvTest
class MenuClickGuardTest {

    private static final ItemStack STONE = ItemStack.of(Material.STONE);
    private static final ItemStack DIAMOND = ItemStack.of(Material.DIAMOND);
    /** Chest 9x3 has 27 slots; the hotbar follows the 27 main-inventory slots in the window. */
    private static final int HOTBAR_0_WINDOW_SLOT = 27 + 27;
    private static final int SLOT = 12;

    static final class GuardMenu extends Menu {
        final AtomicInteger clicks = new AtomicInteger();
        Consumer<GuardMenu> onClick = m -> {};
        int guardTicks = DEFAULT_CLICK_GUARD_TICKS;

        GuardMenu(Player player) {
            super(player, InventoryType.CHEST_3_ROW, Component.text("guard"));
        }

        @Override
        protected void render() {
            set(SLOT, STONE, c -> {
                clicks.incrementAndGet();
                onClick.accept(this);
            });
        }

        @Override
        protected int clickGuardTicks() {
            return guardTicks;
        }

        Menu parentMenu() {
            return parent();
        }

        void allowPlayerInv() {
            allowPlayerInventory(true);
        }
    }

    private static Player player(Env env) {
        return env.createPlayer(env.createFlatInstance(), new Pos(0, 42, 0));
    }

    private static void click(Player player, Inventory inventory, int slot) {
        WindowListener.clickWindowListener(new ClientClickWindowPacket(inventory.getWindowId(), 0, (short) slot,
                (byte) 0, ClickType.PICKUP, Map.of(), ItemStack.Hash.of(ItemStack.AIR, MinecraftServer.process())), player);
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) env.tick();
    }

    private static void assertIgnored(Player player, GuardMenu menu, int clicksBefore) {
        assertEquals(clicksBefore, menu.clicks.get(), "a guarded click must not run the handler");
        assertEquals(STONE, menu.getInventory().getItemStack(SLOT));
        assertTrue(player.getInventory().getCursorItem().isAir());
    }

    @Test
    void firstOpenIsNotGuarded(Env env) {
        Player player = player(env);
        GuardMenu menu = new GuardMenu(player);
        menu.show();

        click(player, menu.getInventory(), SLOT);

        assertEquals(1, menu.clicks.get());
    }

    @Test
    void openChildGuardsTheChildUntilTheDeadline(Env env) {
        Player player = player(env);
        GuardMenu parent = new GuardMenu(player);
        GuardMenu child = new GuardMenu(player);
        parent.onClick = m -> m.openChild(child);
        parent.show();
        child.allowPlayerInv();
        player.getInventory().setItemStack(0, DIAMOND);

        click(player, parent.getInventory(), SLOT); // opens the child
        assertTrue(child.isCurrent());
        // The stray second click of the double click carries the parent's window id and lands on the child.
        click(player, parent.getInventory(), SLOT);
        assertIgnored(player, child, 0);
        assertEquals(1, parent.clicks.get());
        // Even the player's own inventory is frozen while guarded, though the child allows it.
        click(player, child.getInventory(), HOTBAR_0_WINDOW_SLOT);
        assertEquals(DIAMOND, player.getInventory().getItemStack(0));
        assertTrue(player.getInventory().getCursorItem().isAir());

        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS - 1);
        click(player, child.getInventory(), SLOT);
        assertIgnored(player, child, 0);

        env.tick();
        click(player, child.getInventory(), SLOT);
        assertEquals(1, child.clicks.get());
        click(player, child.getInventory(), HOTBAR_0_WINDOW_SLOT);
        assertEquals(DIAMOND, player.getInventory().getCursorItem());
    }

    @Test
    void backGuardsTheResumedParent(Env env) {
        Player player = player(env);
        GuardMenu parent = new GuardMenu(player);
        GuardMenu child = new GuardMenu(player);
        child.onClick = Menu::back;
        parent.show();
        parent.openChild(child);
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);

        click(player, child.getInventory(), SLOT); // back
        assertTrue(parent.isCurrent());
        click(player, child.getInventory(), SLOT);
        assertIgnored(player, parent, 0);

        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);
        click(player, parent.getInventory(), SLOT);
        assertEquals(1, parent.clicks.get());
        assertEquals(1, child.clicks.get());
    }

    @Test
    void everyAncestorResumedByBackGetsAFreshDeadline(Env env) {
        Player player = player(env);
        GuardMenu root = new GuardMenu(player);
        GuardMenu child = new GuardMenu(player);
        GuardMenu grandchild = new GuardMenu(player);
        root.show();
        root.openChild(child);
        child.openChild(grandchild);
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS * 3);

        grandchild.back();
        click(player, child.getInventory(), SLOT);
        assertIgnored(player, child, 0);
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);

        child.back();
        click(player, root.getInventory(), SLOT);
        assertIgnored(player, root, 0);
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);
        click(player, root.getInventory(), SLOT);
        assertEquals(1, root.clicks.get());
    }

    @Test
    void plainShowFromAnotherMenusClickHandlerIsGuarded(Env env) {
        Player player = player(env);
        GuardMenu first = new GuardMenu(player);
        GuardMenu second = new GuardMenu(player);
        first.onClick = m -> second.show();
        first.show();

        click(player, first.getInventory(), SLOT);
        assertTrue(second.isCurrent());
        assertFalse(second.hasParent());
        click(player, first.getInventory(), SLOT);
        assertIgnored(player, second, 0);

        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);
        click(player, second.getInventory(), SLOT);
        assertEquals(1, second.clicks.get());
    }

    @Test
    void menuOpenedDirectlyOverAnotherMenuIsGuarded(Env env) {
        Player player = player(env);
        GuardMenu first = new GuardMenu(player);
        GuardMenu second = new GuardMenu(player);
        first.show();
        second.refresh();

        // Not show() or openChild(): the open event replaces the first menu.
        player.openInventory(second.getInventory());

        assertTrue(first.isClosed());
        click(player, second.getInventory(), SLOT);
        assertIgnored(player, second, 0);
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);
        click(player, second.getInventory(), SLOT);
        assertEquals(1, second.clicks.get());
    }

    @Test
    void rawInventoryOpenBeforeIsNotGuarded(Env env) {
        Player player = player(env);
        player.openInventory(new Inventory(InventoryType.CHEST_1_ROW, Component.text("raw")));
        GuardMenu menu = new GuardMenu(player);
        menu.show();

        click(player, menu.getInventory(), SLOT);

        assertEquals(1, menu.clicks.get());
    }

    @Test
    void reopeningFromNothingClearsAnEarlierGuard(Env env) {
        Player player = player(env);
        GuardMenu parent = new GuardMenu(player);
        GuardMenu child = new GuardMenu(player);
        parent.show();
        parent.openChild(child);
        player.closeInventory();

        child.show(); // from nothing: the deadline set by openChild no longer applies
        click(player, child.getInventory(), SLOT);
        assertEquals(1, child.clicks.get());

        player.closeInventory();
        parent.show();
        click(player, parent.getInventory(), SLOT);
        assertEquals(1, parent.clicks.get());
    }

    @Test
    void zeroGuardTicksDisablesTheGuard(Env env) {
        Player player = player(env);
        GuardMenu parent = new GuardMenu(player);
        GuardMenu child = new GuardMenu(player);
        child.guardTicks = 0;
        parent.show();
        parent.openChild(child);

        click(player, child.getInventory(), SLOT);

        assertEquals(1, child.clicks.get());
    }

    @Test
    void customGuardTicksAreHonoured(Env env) {
        Player player = player(env);
        GuardMenu parent = new GuardMenu(player);
        GuardMenu child = new GuardMenu(player);
        child.guardTicks = 2;
        parent.show();
        parent.openChild(child);

        env.tick();
        click(player, child.getInventory(), SLOT);
        assertIgnored(player, child, 0);
        env.tick();
        click(player, child.getInventory(), SLOT);
        assertEquals(1, child.clicks.get());
    }

    @Test
    void parentIsTheMenuBelowInTheChain(Env env) {
        Player player = player(env);
        GuardMenu root = new GuardMenu(player);
        GuardMenu child = new GuardMenu(player);
        root.show();
        assertNull(root.parentMenu());

        root.openChild(child);
        assertSame(root, child.parentMenu());

        child.back();
        assertNull(child.parentMenu());
        assertNull(root.parentMenu());
    }

    @Test
    void secondConfirmClickLandsOnTheGuardedParentAndDoesNothing(Env env) {
        Player player = player(env);
        AtomicInteger parentClicks = new AtomicInteger();
        Menu parent = new Menu(player, InventoryType.CHEST_3_ROW, Component.text("shop")) {
            @Override
            protected void render() {
                // The shop's own item sits under the confirm button.
                set(ConfirmMenu.CONFIRM_SLOT, STONE, c -> parentClicks.incrementAndGet());
            }
        };
        parent.show();
        AtomicInteger confirms = new AtomicInteger();
        AtomicInteger cancels = new AtomicInteger();
        ConfirmMenu confirm = new ConfirmMenu(player, Component.text("sure?"), DIAMOND, PaginatedMenuTest.STYLE,
                confirms::incrementAndGet, cancels::incrementAndGet);
        parent.openChild(confirm);
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);

        click(player, confirm.getInventory(), ConfirmMenu.CONFIRM_SLOT);
        assertTrue(parent.isCurrent());
        click(player, confirm.getInventory(), ConfirmMenu.CONFIRM_SLOT);

        assertEquals(1, confirms.get());
        assertEquals(0, parentClicks.get());
        assertTrue(parent.isCurrent());
        assertEquals(STONE, parent.getInventory().getItemStack(ConfirmMenu.CONFIRM_SLOT));
        assertTrue(player.getInventory().getCursorItem().isAir());
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);
        assertEquals(0, cancels.get());
    }

    @Test
    void paginatedArrowsDoNothingInsideTheWindow(Env env) {
        Player player = player(env);
        PaginatedMenuTest.ListMenu root = new PaginatedMenuTest.ListMenu(player);
        root.items(IntStream.range(0, 40).boxed().toList());
        root.show();
        PaginatedMenuTest.ListMenu child = new PaginatedMenuTest.ListMenu(player);
        child.items(List.of());
        root.openChild(child);
        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);
        Layout layout = new Layout(3);

        click(player, child.getInventory(), layout.back());
        assertTrue(root.isCurrent());
        click(player, root.getInventory(), layout.next());
        click(player, root.getInventory(), 0);
        assertEquals(ItemStack.of(Material.PAPER, 1), root.getInventory().getItemStack(0), "still page 1");
        assertTrue(root.clicked.isEmpty());

        tick(env, Menu.DEFAULT_CLICK_GUARD_TICKS);
        click(player, root.getInventory(), layout.next());
        assertEquals(ItemStack.of(Material.PAPER, 19), root.getInventory().getItemStack(0), "page 2");
    }
}
