package club.tesseract.minestom.utils.gui;

import club.tesseract.minestom.utils.gui.PageSource.Page;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A menu that lists elements over pages: one element per content slot, previous/next buttons in the footer, a back
 * button when the menu has a parent ({@link #hasParent()}).
 * <p>
 * Two sources:
 * <ul>
 *     <li>{@link #setItems(List)}: an in-memory list, paged with {@link Pager}. The page count is exact.</li>
 *     <li>{@link #setSource(PageSource)}: async cursor paging. The menu keeps the cursors of earlier pages for
 *     "previous". The page count is unknown, so the count handed to {@link ButtonStyle} is the current page (1-based)
 *     plus one when a next page exists, else the current page. While a page loads, the content area shows
 *     {@link ButtonStyle#loading()} and no previous/next buttons; a newer load supersedes older ones, whose results are
 *     discarded.</li>
 * </ul>
 * An empty list shows {@link ButtonStyle#empty()} as page 1 of 1, with no previous/next.
 * <p>
 * {@link #render()} draws only from the menu's state, so {@link #refresh()} and {@link #back()} into this menu never
 * refetch: an async menu keeps its loaded page and cursor history. {@link #reload()} refetches. A page that finishes
 * loading while the menu is covered by a child is kept and shown on {@link #back()}; it is drawn only while the menu
 * is the player's open, unclosed menu.
 * <p>
 * Slots: the menu draws the content slots ({@link #contentSlots()}), the back/prev/next slots and nothing else. Draw
 * other buttons (filters, info) in {@link #renderFooter()}, which runs on every redraw, so they survive page changes.
 *
 * @param <T> element type
 */
public abstract class PaginatedMenu<T> extends Menu {

    protected final ButtonStyle style;
    private final Layout layout;

    private int[] contentSlots;
    private Pager pager;

    /** In-memory mode: the whole list. Cursor mode: the loaded page's elements. */
    private List<T> items = List.of();
    /** 0-based page shown. In cursor mode equal to the cursor history's size. */
    private int page = 0;

    private @Nullable PageSource<T> source;
    private @Nullable Long cursor;
    private @Nullable Long nextCursor;
    /** Cursors of the earlier pages, most recent first; null stands for the first page. */
    private final Deque<Long> history = new LinkedList<>();
    private boolean loading = false;
    /** Bumped by every load and source change; a completion with an older generation is stale. */
    private long generation = 0;

    private enum Move { FIRST, NEXT, PREV, RELOAD }

    protected PaginatedMenu(@NotNull Player player, @NotNull InventoryType type, @NotNull Component title,
                            @NotNull ButtonStyle style) {
        super(player, type, title);
        this.style = style;
        this.layout = Layout.of(type);
    }

    /** The item shown for {@code element}. */
    @NotNull
    protected abstract ItemStack item(@NotNull T element);

    /** Runs when the player clicks {@code element}'s slot. Does nothing by default. */
    protected void onClick(@NotNull T element, @NotNull Click click) {
    }

    /**
     * Draws the menu's own extra buttons after the content and the navigation buttons, on every redraw. Use slots
     * outside {@link #contentSlots()} and the back/prev/next slots. Does nothing by default.
     */
    protected void renderFooter() {
    }

    /** Runs when an async page load fails while this menu is current. The menu keeps the page it showed. */
    protected void onLoadError(@NotNull Throwable error) {
    }

    /** Slots that show elements, in order. Default {@link Layout#contentSlots()}; the page size is its length. */
    protected int @NotNull [] contentSlots() {
        return this.layout.contentSlots();
    }

    /** Slot for the empty-state and loading items. Default: the centre of a full-width content area. */
    protected int statusSlot() {
        int[] slots = slots();
        if (slots.length % 9 == 0) return slots[(slots.length / 9 / 2) * 9 + 4];
        return slots[slots.length / 2];
    }

    protected int backSlot() {
        return this.layout.back();
    }

    protected int prevSlot() {
        return this.layout.prev();
    }

    protected int nextSlot() {
        return this.layout.next();
    }

    @NotNull
    protected final Layout layout() {
        return this.layout;
    }

    /** The 0-based page shown. */
    protected final int page() {
        return this.page;
    }

    /** True while an async page is loading. */
    protected final boolean isLoading() {
        return this.loading;
    }

    /**
     * Lists {@code items} from memory, replacing any async source (an in-flight load is discarded). Keeps the current
     * page, clamped to the new page count, and redraws if the menu is current.
     */
    protected final void setItems(@NotNull List<T> items) {
        this.generation++;
        this.source = null;
        this.loading = false;
        this.history.clear();
        this.cursor = null;
        this.nextCursor = null;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.page = pager().clamp(this.page, this.items.size());
        if (isCurrent()) refresh();
    }

    /** Pages through {@code source} by cursor, starting at the first page (loads it now). */
    protected final void setSource(@NotNull PageSource<T> source) {
        this.source = source;
        this.history.clear();
        this.cursor = null;
        this.nextCursor = null;
        this.items = List.of();
        this.page = 0;
        load(null, Move.FIRST);
    }

    /**
     * Refetches the page shown from the async source (the cursor history is kept); with an in-memory list, just
     * redraws if current. Unlike {@link #refresh()}, which only redraws from state.
     */
    public final void reload() {
        if (this.source != null) {
            load(this.cursor, Move.RELOAD);
        } else if (isCurrent()) {
            refresh();
        }
    }

    @Override
    protected final void render() {
        int[] slots = slots();
        List<T> shown = pageItems();
        if (this.loading) {
            set(statusSlot(), this.style.loading());
        } else if (shown.isEmpty()) {
            set(statusSlot(), this.style.empty());
        } else {
            for (int i = 0; i < shown.size(); i++) {
                T element = shown.get(i);
                set(slots[i], item(element), ClickHandlers.any(click -> onClick(element, click)));
            }
        }
        if (hasParent()) set(backSlot(), this.style.back(), click -> back());
        if (!this.loading) {
            int current = this.page + 1;
            int count = pageCount();
            if (hasPrevPage()) set(prevSlot(), this.style.prev(current, count), click -> previousPage());
            if (hasNextPage()) set(nextSlot(), this.style.next(current, count), click -> nextPage());
        }
        renderFooter();
    }

    private void previousPage() {
        if (!isCurrent() || this.loading) return;
        if (this.source != null) {
            if (!this.history.isEmpty()) load(this.history.peek(), Move.PREV);
        } else if (this.page > 0) {
            this.page--;
            refresh();
        }
    }

    private void nextPage() {
        if (!isCurrent() || this.loading) return;
        if (this.source != null) {
            if (this.nextCursor != null) load(this.nextCursor, Move.NEXT);
        } else if (pager().hasNext(this.page, this.items.size())) {
            this.page++;
            refresh();
        }
    }

    private void load(@Nullable Long target, Move move) {
        PageSource<T> loadingFrom = this.source;
        long loadGeneration = ++this.generation;
        this.loading = true;
        CompletableFuture<Page<T>> future;
        try {
            future = loadingFrom.load(target);
        } catch (RuntimeException e) {
            future = CompletableFuture.failedFuture(e);
        }
        if (isCurrent()) refresh();
        future.whenComplete((result, error) -> this.player.scheduler().scheduleNextTick(() -> {
            if (loadGeneration != this.generation) return; // superseded by a newer load or source
            this.loading = false;
            boolean visible = isCurrent() && !isClosed();
            if (error != null || result == null) {
                if (visible) onLoadError(error != null ? unwrap(error) : new NullPointerException("null page"));
            } else {
                apply(target, move, result);
            }
            if (visible) refresh();
        }));
    }

    private void apply(@Nullable Long target, Move move, Page<T> result) {
        switch (move) {
            case NEXT -> {
                this.history.push(this.cursor);
                this.page++;
            }
            case PREV -> {
                this.history.pop();
                this.page--;
            }
            case FIRST, RELOAD -> {
            }
        }
        this.cursor = target;
        this.nextCursor = result.nextCursor();
        List<T> loaded = result.items();
        this.items = loaded.size() > slots().length ? loaded.subList(0, slots().length) : loaded;
    }

    private List<T> pageItems() {
        if (this.source != null) return this.items;
        Pager pager = pager();
        int total = this.items.size();
        return this.items.subList(pager.from(this.page), pager.to(this.page, total));
    }

    private int pageCount() {
        if (this.source != null) return this.page + 1 + (this.nextCursor != null ? 1 : 0);
        return pager().pageCount(this.items.size());
    }

    private boolean hasPrevPage() {
        return this.source != null ? !this.history.isEmpty() : this.page > 0;
    }

    private boolean hasNextPage() {
        return this.source != null ? this.nextCursor != null : pager().hasNext(this.page, this.items.size());
    }

    private int[] slots() {
        if (this.contentSlots == null) {
            int[] slots = contentSlots();
            if (slots.length == 0) throw new IllegalStateException("PaginatedMenu needs at least one content slot");
            this.contentSlots = slots.clone();
        }
        return this.contentSlots;
    }

    private Pager pager() {
        if (this.pager == null) this.pager = new Pager(slots().length);
        return this.pager;
    }
}
