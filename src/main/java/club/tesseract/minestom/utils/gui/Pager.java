package club.tesseract.minestom.utils.gui;

/**
 * Pagination math for menus. Pages are 0-based; an empty list still has one page.
 */
public final class Pager {

    private final int pageSize;

    public Pager(int pageSize) {
        if (pageSize < 1) throw new IllegalArgumentException("pageSize must be >= 1, was " + pageSize);
        this.pageSize = pageSize;
    }

    public int pageSize() {
        return pageSize;
    }

    public int pageCount(int total) {
        if (total <= 0) return 1;
        return (int) (((long) total + pageSize - 1) / pageSize);
    }

    public int clamp(int page, int total) {
        return Math.max(0, Math.min(page, pageCount(total) - 1));
    }

    /** First item index of the page (inclusive). */
    public int from(int page) {
        return page * pageSize;
    }

    /** End item index of the page (exclusive), capped at total. */
    public int to(int page, int total) {
        return Math.min(from(page) + pageSize, Math.max(total, 0));
    }

    public boolean hasPrev(int page) {
        return page > 0;
    }

    public boolean hasNext(int page, int total) {
        return page < pageCount(total) - 1;
    }
}
