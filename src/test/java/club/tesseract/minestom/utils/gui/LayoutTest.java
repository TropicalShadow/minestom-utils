package club.tesseract.minestom.utils.gui;

import net.minestom.server.inventory.InventoryType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LayoutTest {

    @Test
    void sixRowFooterSlots() {
        Layout layout = Layout.of(InventoryType.CHEST_6_ROW);
        assertEquals(54, layout.size());
        assertEquals(45, layout.footerStart());
        assertEquals(45, layout.back());
        assertEquals(46, layout.prev());
        assertEquals(49, layout.close());
        assertEquals(52, layout.info());
        assertEquals(53, layout.next());
    }

    @Test
    void contentSlotsExcludeFooterAndCountMatches() {
        assertEquals(45, Layout.of(InventoryType.CHEST_6_ROW).contentSlots().length);
        int[] three = Layout.of(InventoryType.CHEST_3_ROW).contentSlots();
        assertEquals(18, three.length);
        assertEquals(0, three[0]);
        assertEquals(17, three[17]);
        assertEquals(0, Layout.of(InventoryType.CHEST_1_ROW).contentSlots().length);
    }

    @Test
    void innerContentSlotsSkipTheRing() {
        int[] inner = Layout.of(InventoryType.CHEST_6_ROW).contentSlots(1);
        assertEquals(10, inner[0]);
        assertEquals(21, inner.length);
    }

    @Test
    void borderSlotsForThreeRows() {
        int[] border = Layout.of(InventoryType.CHEST_3_ROW).borderSlots();
        assertEquals(20, border.length);
        assertEquals(20, Arrays.stream(border).distinct().count());
    }

    @Test
    void rejectsNonChest() {
        assertThrows(IllegalArgumentException.class, () -> Layout.of(InventoryType.HOPPER));
    }
}
