package club.tesseract.minestom.utils.gui;

import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The items menus use for their standard buttons. This library ships no colours or translations: consumers supply
 * a themed implementation and pass it to the menus that need one.
 * <p>
 * Page numbers handed to {@link #prev(int, int)} and {@link #next(int, int)} are the page the player is on, 1-based,
 * and the page count. For cursor-paged menus the count is not known: it is the current page plus one when a next page
 * exists, else the current page (see {@link PaginatedMenu}).
 */
public interface ButtonStyle {

    @NotNull ItemStack back();

    /** @param page the page shown now, 1-based */
    @NotNull ItemStack prev(int page, int pageCount);

    /** @param page the page shown now, 1-based */
    @NotNull ItemStack next(int page, int pageCount);

    @NotNull ItemStack close();

    /** Shown in the content area while a page is loading. */
    @NotNull ItemStack loading();

    /** Shown in the content area when there is nothing to list. */
    @NotNull ItemStack empty();

    @NotNull ItemStack filler();

    @NotNull ItemStack confirm();

    @NotNull ItemStack cancel();
}
