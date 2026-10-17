package club.tesseract.minestom.utils.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagerTest {

    private final Pager pager = new Pager(28);

    @Test
    void pageCountRoundsUpAndEmptyIsOne() {
        assertEquals(1, pager.pageCount(0));
        assertEquals(1, pager.pageCount(1));
        assertEquals(1, pager.pageCount(28));
        assertEquals(2, pager.pageCount(29));
    }

    @Test
    void clampsPastTheEnd() {
        Pager p = new Pager(5);
        assertEquals(1, p.clamp(5, 10));
        assertEquals(0, p.clamp(-3, 10));
    }

    @Test
    void rangeIsHalfOpenAndCapped() {
        assertEquals(28, pager.from(1));
        assertEquals(40, pager.to(1, 40));
        assertEquals(28, pager.to(0, 40));
    }

    @Test
    void hasPrevHasNext() {
        assertFalse(pager.hasPrev(0));
        assertTrue(pager.hasPrev(1));
        assertTrue(pager.hasNext(0, 40));
        assertFalse(pager.hasNext(1, 40));
        assertFalse(pager.hasPrev(0));
        assertFalse(pager.hasNext(0, 0));
    }

    @Test
    void rejectsBadPageSize() {
        assertThrows(IllegalArgumentException.class, () -> new Pager(0));
    }
}
