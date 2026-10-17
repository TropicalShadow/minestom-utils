package club.tesseract.minestom.utils.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class NavStackTest {

    private static Menu menu(Player player) {
        return new Menu(player, InventoryType.CHEST_1_ROW, Component.text("m")) {};
    }

    @Test
    void popOnEmptyReturnsNull() {
        NavStack stack = new NavStack();
        assertTrue(stack.isEmpty());
        assertNull(stack.pop());
    }

    @Test
    void popsInReverseOrderAndClearEmpties(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), new Pos(0, 42, 0));
        Menu a = menu(player), b = menu(player);
        NavStack stack = new NavStack();

        stack.push(a);
        stack.push(b);
        assertEquals(2, stack.size());
        assertSame(b, stack.pop());
        assertSame(a, stack.pop());
        assertNull(stack.pop());

        stack.push(a);
        stack.clear();
        assertTrue(stack.isEmpty());
    }
}
