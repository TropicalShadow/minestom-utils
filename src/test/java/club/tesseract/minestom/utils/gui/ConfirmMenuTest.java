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
        assertEquals(1, counts.cancel.get());
        assertEquals(0, counts.confirm.get());
    }

    @Test
    void replacedByAnotherInventoryRunsCancelOnce(Env env) {
        Player player = player(env);
        Counts counts = new Counts();
        counts.menu(player).show();
        new ParentMenu(player).show();
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
        click(player, menu.getInventory(), 15, 0);
        assertSame(parent.getInventory(), player.getOpenInventory());
        player.closeInventory();
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
}
