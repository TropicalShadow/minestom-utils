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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class ToggleTest {

    private static final ItemStack ON = ItemStack.of(Material.LIME_DYE);
    private static final ItemStack OFF = ItemStack.of(Material.GRAY_DYE);

    static final class TogglesMenu extends Menu {
        final AtomicBoolean state = new AtomicBoolean(false);
        final List<Boolean> changes = new ArrayList<>();
        int renders = 0;

        TogglesMenu(Player player) {
            super(player, InventoryType.CHEST_3_ROW, Component.text("settings"));
        }

        @Override
        protected void render() {
            renders++;
            set(3, ItemStack.of(Material.STONE));
            Toggle.set(this, 5, state::get, on -> on ? ON : OFF, v -> {
                changes.add(v);
                state.set(v);
            });
        }
    }

    private static void click(Player player, Inventory inventory, int slot, int button) {
        WindowListener.clickWindowListener(new ClientClickWindowPacket(inventory.getWindowId(), 0, (short) slot,
                (byte) button, ClickType.PICKUP, Map.of(), ItemStack.Hash.of(ItemStack.AIR, MinecraftServer.process())), player);
    }

    private static Player player(Env env) {
        return env.createPlayer(env.createFlatInstance(), new Pos(0, 42, 0));
    }

    @Test
    void toggleFlipsAndRerendersSlot(Env env) {
        Player player = player(env);
        TogglesMenu menu = new TogglesMenu(player);
        menu.show();
        Inventory inv = menu.getInventory();
        assertEquals(OFF, inv.getItemStack(5));
        click(player, inv, 5, 0);
        assertEquals(List.of(true), menu.changes);
        assertEquals(ON, inv.getItemStack(5));
        click(player, inv, 5, 1);
        assertEquals(List.of(true, false), menu.changes);
        assertEquals(OFF, inv.getItemStack(5));
    }

    @Test
    void rerenderTouchesOnlyItsOwnSlotAndNotTheWholeMenu(Env env) {
        Player player = player(env);
        TogglesMenu menu = new TogglesMenu(player);
        menu.show();
        menu.getInventory().setItemStack(3, ItemStack.of(Material.DIRT)); // would be reset by a full render
        click(player, menu.getInventory(), 5, 0);
        assertEquals(1, menu.renders);
        assertEquals(ItemStack.of(Material.DIRT), menu.getInventory().getItemStack(3));
    }

    @Test
    void secondToggleOnSameMenuIsIndependent(Env env) {
        Player player = player(env);
        AtomicInteger a = new AtomicInteger();
        AtomicBoolean sa = new AtomicBoolean(), sb = new AtomicBoolean();
        Menu menu = new Menu(player, InventoryType.CHEST_3_ROW, Component.text("t")) {
            @Override protected void render() {
                Toggle.set(this, 0, sa::get, on -> on ? ON : OFF, v -> { sa.set(v); a.incrementAndGet(); });
                Toggle.set(this, 1, sb::get, on -> on ? ON : OFF, sb::set);
            }
        };
        menu.show();
        click(player, menu.getInventory(), 0, 0);
        assertEquals(ON, menu.getInventory().getItemStack(0));
        assertEquals(OFF, menu.getInventory().getItemStack(1));
        assertEquals(1, a.get());
    }
}
