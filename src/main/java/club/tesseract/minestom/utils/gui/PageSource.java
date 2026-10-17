package club.tesseract.minestom.utils.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Loads pages of a cursor-paged list for {@link PaginatedMenu}.
 *
 * @param <T> element type
 */
@FunctionalInterface
public interface PageSource<T> {

    /**
     * Loads the page starting at {@code cursor}.
     *
     * @param cursor null for the first page, else a {@link Page#nextCursor()} returned earlier
     */
    @NotNull CompletableFuture<Page<T>> load(@Nullable Long cursor);

    /**
     * One loaded page.
     *
     * @param items      the page's elements; return at most the menu's page size (its content slot count): extra
     *                   items are dropped with a warning
     * @param nextCursor cursor of the following page, or null when this is the last page
     */
    record Page<T>(@NotNull List<T> items, @Nullable Long nextCursor) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
