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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class ConfirmMenuTest {

    private static final ButtonStyle STYLE = new PaginatedMenuTest.TestStyle();
    private static final ItemStack SUBJECT = ItemStack.of(Material.DIAMOND);

    static final class ParentMenu extends Menu {
        int renders = 0;

        ParentMenu(Player player) {
            super(player, InventoryType.CHEST_3_ROW, Component.text("parent"));
        }

        @Override
        protected void render() {
            renders++;
            set(0, ItemStack.of(Material.STONE));
        }
    }

    private static Player player(Env env) {
        return env.createPlayer(env.createFlatInstance(), new Pos(0, 42, 0));
    }

    private static void click(Player player, Inventory inventory, int slot, int button) {
        WindowListener.clickWindowListener(new ClientClickWindowPacket(inventory.getWindowId(), 0, (short) slot,
                (byte) button, ClickType.PICKUP, Map.of(), ItemStack.Hash.of(ItemStack.AIR, MinecraftServer.process())), player);
    }

    /** Waits out the click guard that a menu-to-menu transition arms. */
    private static void tickPastGuard(Env env) {
        for (int i = 0; i < Menu.DEFAULT_CLICK_GUARD_TICKS; i++) env.tick();
    }

    private static final class Counts {
        final AtomicInteger confirm = new AtomicInteger();
        final AtomicInteger cancel = new AtomicInteger();

        ConfirmMenu menu(Player player) {
            return new ConfirmMenu(player, Component.text("sure?"), SUBJECT, STYLE,
                    confirm::incrementAndGet, cancel::incrementAndGet);
        }
    }

    @Test
    void layoutHasConfirmSubjectCancelAndBorder(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        menu.show();
        Inventory inv = menu.getInventory();
        assertEquals(InventoryType.CHEST_3_ROW, inv.getInventoryType());
        assertEquals(STYLE.confirm(), inv.getItemStack(11));
        assertEquals(SUBJECT, inv.getItemStack(13));
        assertEquals(STYLE.cancel(), inv.getItemStack(15));
        for (int slot : new Layout(3).borderSlots()) {
            assertEquals(STYLE.filler(), inv.getItemStack(slot), "border " + slot);
        }
        assertNotEquals(STYLE.filler(), inv.getItemStack(11));
        assertNotEquals(STYLE.filler(), inv.getItemStack(13));
        assertNotEquals(STYLE.filler(), inv.getItemStack(15));
    }

    @Test
    void confirmRunsCallbackExactlyOnceOnRepeatedClicks(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        menu.show();
        click(player, menu.getInventory(), 11, 0);
        click(player, menu.getInventory(), 11, 0);
        click(player, menu.getInventory(), 11, 1);
        click(player, menu.getInventory(), 15, 0);
        assertEquals(1, counts.confirm.get());
        assertEquals(0, counts.cancel.get());
        assertNull(player.getOpenInventory());
    }

    @Test
    void cancelClickRunsOnCancelOnceNotTwiceAfterClose(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        menu.show();
        click(player, menu.getInventory(), 15, 0);
        click(player, menu.getInventory(), 15, 0);
        click(player, menu.getInventory(), 11, 0);
        player.closeInventory();
        env.tick();
        assertEquals(1, counts.cancel.get());
        assertEquals(0, counts.confirm.get());
        assertNull(player.getOpenInventory());
    }

    @Test
    void closeWithoutClickRunsCancelOnce(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        menu.show();
        player.closeInventory();
        player.closeInventory();
        assertEquals(0, counts.cancel.get()); // runs on the next tick
        env.tick();
        env.tick();
        assertEquals(1, counts.cancel.get());
        assertEquals(0, counts.confirm.get());
    }

    @Test
    void replacedByAnotherInventoryRunsCancelOnce(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        counts.menu(player).show();
        new ParentMenu(player).show();
        env.tick();
        assertEquals(1, counts.cancel.get());
    }

    @Test
    void confirmWithParentReturnsToParentAndRerenders(Env env) {
        Player player = player(env);
        ParentMenu parent = new ParentMenu(player);
        parent.show();
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        parent.openChild(menu);
        assertTrue(menu.hasParent());
        tickPastGuard(env);
        int before = parent.renders;
        click(player, menu.getInventory(), 11, 0);
        assertEquals(1, counts.confirm.get());
        assertEquals(0, counts.cancel.get());
        assertSame(parent.getInventory(), player.getOpenInventory());
        assertEquals(before + 1, parent.renders);
    }

    @Test
    void cancelWithParentReturnsToParentWithoutExtraCancel(Env env) {
        Player player = player(env);
        ParentMenu parent = new ParentMenu(player);
        parent.show();
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        parent.openChild(menu);
        tickPastGuard(env);
        click(player, menu.getInventory(), 15, 0);
        assertSame(parent.getInventory(), player.getOpenInventory());
        player.closeInventory();
        env.tick();
        assertEquals(1, counts.cancel.get());
        assertEquals(0, counts.confirm.get());
    }

    @Test
    void throwingConfirmStillLeavesAndSkipsCancel(Env env) {
        Player player = player(env);
        AtomicInteger cancel = new AtomicInteger();
        ConfirmMenu menu = new ConfirmMenu(player, Component.text("x"), SUBJECT, STYLE,
                () -> { throw new IllegalStateException("boom"); }, cancel::incrementAndGet);
        menu.show();
        assertDoesNotThrow(() -> click(player, menu.getInventory(), 11, 0));
        assertNull(player.getOpenInventory());
        assertEquals(0, cancel.get());
    }

    @Test
    void throwingCancelOnCloseDoesNotPropagate(Env env) {
        Player player = player(env);
        AtomicInteger confirm = new AtomicInteger();
        ConfirmMenu menu = new ConfirmMenu(player, Component.text("x"), SUBJECT, STYLE,
                confirm::incrementAndGet, () -> { throw new IllegalStateException("boom"); });
        menu.show();
        assertDoesNotThrow(() -> { player.closeInventory(); });
        assertDoesNotThrow(() -> env.tick());
        assertEquals(0, confirm.get());
    }

    @Test
    void reshownMenuCanDecideAgain(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        menu.show();
        click(player, menu.getInventory(), 11, 0);
        menu.show();
        click(player, menu.getInventory(), 11, 0);
        assertEquals(2, counts.confirm.get());
    }

    @Test
    void callbackOpeningAnotherMenuKeepsItOpen(Env env) {
        for (int slot : new int[]{11, 15}) {
            Player player = player(env);
            ParentMenu next = new ParentMenu(player);
            Runnable open = next::show;
            ConfirmMenu menu = new ConfirmMenu(player, Component.text("x"), SUBJECT, STYLE, open, open);
            menu.show();
            click(player, menu.getInventory(), slot, 0);
            assertSame(next.getInventory(), player.getOpenInventory(), "slot " + slot);
            tickPastGuard(env);
            click(player, next.getInventory(), 0, 0);
            assertSame(next.getInventory(), player.getOpenInventory());
        }
    }

    @Test
    void refreshAndReopenStartsNewDecision(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        menu.show();
        click(player, menu.getInventory(), 11, 0);
        menu.refresh();
        menu.open(player);
        click(player, menu.getInventory(), 11, 0);
        assertEquals(2, counts.confirm.get());
    }

    @Test
    void parentClosedWhileSuspendedStillRunsOneCallbackAndCloses(Env env) {
        Player player = player(env);
        ParentMenu parent = new ParentMenu(player);
        parent.show();
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        parent.openChild(menu);
        tickPastGuard(env);
        parent.close();
        click(player, menu.getInventory(), 11, 0);
        assertEquals(1, counts.confirm.get());
        assertEquals(0, counts.cancel.get());
        assertNull(player.getOpenInventory());
    }

    @Test
    void confirmThenEscInSameTickWithParentRunsCallbackOnly(Env env) {
        Player player = player(env);
        ParentMenu parent = new ParentMenu(player);
        parent.show();
        Counts counts = new Counts();
        ConfirmMenu menu = counts.menu(player);
        parent.openChild(menu);
        tickPastGuard(env);
        click(player, menu.getInventory(), 11, 0);
        player.closeInventory();
        env.tick();
        assertEquals(1, counts.confirm.get());
        assertEquals(0, counts.cancel.get());
    }

    @Test
    void refreshFromInsideACallbackDoesNotRearmTheDecision(Env env) {
        for (boolean withParent : new boolean[]{false, true}) {
            Player player = player(env);
            ParentMenu parent = new ParentMenu(player);
            AtomicInteger confirm = new AtomicInteger();
            AtomicInteger cancel = new AtomicInteger();
            AtomicReference<ConfirmMenu> self = new AtomicReference<>();
            ConfirmMenu menu = new ConfirmMenu(player, Component.text("x"), SUBJECT, STYLE,
                    () -> { confirm.incrementAndGet(); self.get().refresh(); }, cancel::incrementAndGet);
            self.set(menu);
            if (withParent) {
                parent.show();
                parent.openChild(menu);
                tickPastGuard(env);
            } else {
                menu.show();
            }

            click(player, menu.getInventory(), ConfirmMenu.CONFIRM_SLOT, 0);
            env.tick();
            env.tick();

            assertEquals(1, confirm.get(), "parent " + withParent);
            assertEquals(0, cancel.get(), "parent " + withParent);
            if (withParent) assertSame(parent.getInventory(), player.getOpenInventory());
            else assertNull(player.getOpenInventory());
        }
    }

    @Test
    void escWhoseCancelOpensAnotherMenuLeavesItOpen(Env env) {
        Player player = player(env);
        ParentMenu next = new ParentMenu(player);
        AtomicInteger confirm = new AtomicInteger();
        ConfirmMenu menu = new ConfirmMenu(player, Component.text("x"), SUBJECT, STYLE,
                confirm::incrementAndGet, next::show);
        menu.show();

        player.closeInventory();
        env.tick();

        assertSame(next.getInventory(), player.getOpenInventory());
        assertTrue(next.isCurrent());
        assertFalse(next.isClosed());
        click(player, next.getInventory(), 0, 0);
        assertEquals(ItemStack.of(Material.STONE), next.getInventory().getItemStack(0));
        assertTrue(player.getInventory().getCursorItem().isAir());
        assertSame(next.getInventory(), player.getOpenInventory());
        assertEquals(0, confirm.get());
    }
}
