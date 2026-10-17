package club.tesseract.minestom.utils.gui;

import net.minestom.server.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

/**
 * Slot arithmetic for a 9-column chest inventory. The footer is the last row: back (column 0), previous (1),
 * close (4), info (7) and next (8).
 *
 * @param rows number of rows, 1 to 6
 */
public record Layout(int rows) {
    private static final int COLUMNS = 9;

    public Layout {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("rows must be 1..6: " + rows);
    }

    /** @throws IllegalArgumentException unless {@code type} is CHEST_1_ROW to CHEST_6_ROW */
    @NotNull
    public static Layout of(@NotNull InventoryType type) {
        return switch (type) {
            case CHEST_1_ROW -> new Layout(1);
            case CHEST_2_ROW -> new Layout(2);
            case CHEST_3_ROW -> new Layout(3);
            case CHEST_4_ROW -> new Layout(4);
            case CHEST_5_ROW -> new Layout(5);
            case CHEST_6_ROW -> new Layout(6);
            default -> throw new IllegalArgumentException("Not a chest inventory type: " + type);
        };
    }

    public int size() {
        return this.rows * COLUMNS;
    }

    /** First slot of the footer (the last row). */
    public int footerStart() {
        return size() - COLUMNS;
    }

    public int back() {
        return footerStart();
    }

    public int prev() {
        return footerStart() + 1;
    }

    public int close() {
        return footerStart() + 4;
    }

    public int info() {
        return footerStart() + 7;
    }

    public int next() {
        return footerStart() + 8;
    }

    /** Every slot before the footer, left to right, top to bottom. Empty for a 1-row layout (it has no content area). */
    public int @NotNull [] contentSlots() {
        return IntStream.range(0, footerStart()).toArray();
    }

    /**
     * The content area without a ring of {@code borderWidth} slots on every side. Only the non-footer rows count:
     * the footer is never part of the area. Empty when the ring leaves nothing.
     */
    public int @NotNull [] contentSlots(int borderWidth) {
        if (borderWidth < 0) throw new IllegalArgumentException("borderWidth < 0: " + borderWidth);
        int contentRows = this.rows - 1;
        return IntStream.range(0, footerStart())
                .filter(s -> {
                    int row = s / COLUMNS, col = s % COLUMNS;
                    return row >= borderWidth && row < contentRows - borderWidth
                            && col >= borderWidth && col < COLUMNS - borderWidth;
                })
                .toArray();
    }

    /** The outer ring of the whole inventory (footer row included), in slot order. */
    public int @NotNull [] borderSlots() {
        return IntStream.range(0, size())
                .filter(s -> {
                    int row = s / COLUMNS, col = s % COLUMNS;
                    return row == 0 || row == this.rows - 1 || col == 0 || col == COLUMNS - 1;
                })
                .toArray();
    }
}
