package club.tesseract.minestom.utils.gui;

import club.tesseract.minestom.utils.gui.PageSource.Page;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class PaginatedMenuTest {

    /** CHEST_3_ROW: content slots 0..17 (page size 18), footer 18..26. */
    private static final Layout LAYOUT = new Layout(3);
    private static final int PAGE_SIZE = 18;
    private static final int FOOTER_EXTRA_SLOT = 22; // Layout.close(): free for subclasses

    static final class TestStyle implements ButtonStyle {
        private static ItemStack named(Material material, String name) {
            return ItemStack.of(material).with(DataComponents.CUSTOM_NAME, Component.text(name));
        }

        @Override public ItemStack back() { return named(Material.OAK_DOOR, "back"); }
        @Override public ItemStack prev(int page, int pageCount) { return named(Material.ARROW, "prev " + page + "/" + pageCount); }
        @Override public ItemStack next(int page, int pageCount) { return named(Material.SPECTRAL_ARROW, "next " + page + "/" + pageCount); }
        @Override public ItemStack close() { return named(Material.BARRIER, "close"); }
        @Override public ItemStack loading() { return named(Material.CLOCK, "loading"); }
        @Override public ItemStack empty() { return named(Material.GLASS_BOTTLE, "empty"); }
        @Override public ItemStack filler() { return named(Material.GRAY_STAINED_GLASS_PANE, "filler"); }
        @Override public ItemStack confirm() { return named(Material.LIME_WOOL, "confirm"); }
        @Override public ItemStack cancel() { return named(Material.RED_WOOL, "cancel"); }
    }

    static final ButtonStyle STYLE = new TestStyle();

    /** Each element renders as paper whose amount is element + 1 (elements are 0..63). */
    static final class ListMenu extends PaginatedMenu<Integer> {
        final List<Integer> clicked = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();
        boolean throwOnError = false;
        int renders = 0;
        int failFooterDraws = 0;

        @Override
        protected void onLoadError(Throwable error) {
            errors.add(error);
            if (throwOnError) throw new IllegalStateException("onLoadError failed");
        }

        boolean loading() {
            return isLoading();
        }

        ListMenu(Player player) {
            super(player, InventoryType.CHEST_3_ROW, Component.text("list"), STYLE);
        }

        @Override
        protected ItemStack item(Integer element) {
            return ItemStack.of(Material.PAPER, element % 64 + 1);
        }

        @Override
        protected void onClick(Integer element, Click click) {
            clicked.add(element);
        }

        @Override
        protected void renderFooter() {
            if (failFooterDraws > 0) {
                failFooterDraws--;
                throw new IllegalStateException("footer failed");
            }
            renders++;
            set(FOOTER_EXTRA_SLOT, ItemStack.of(Material.HOPPER));
        }

        void items(List<Integer> items) {
            setItems(items);
        }

        void items(List<Integer> items, boolean resetPage) {
            setItems(items, resetPage);
        }

        void source(PageSource<Integer> source) {
            setSource(source);
        }
    }

    /** A source whose futures the test completes by hand; records every requested cursor. */
    static final class ManualSource implements PageSource<Integer> {
        final List<Long> requested = new ArrayList<>();
        final List<CompletableFuture<Page<Integer>>> futures = new ArrayList<>();

        @Override
        public CompletableFuture<Page<Integer>> load(Long cursor) {
            requested.add(cursor);
            CompletableFuture<Page<Integer>> future = new CompletableFuture<>();
            futures.add(future);
            return future;
        }

        CompletableFuture<Page<Integer>> last() {
            return futures.getLast();
        }
    }

    private static Player player(Env env) {
        return env.createPlayer(env.createFlatInstance(), new Pos(0, 42, 0));
    }

    private static void click(Player player, Inventory inventory, int slot) {
        WindowListener.clickWindowListener(new ClientClickWindowPacket(inventory.getWindowId(), 0, (short) slot,
                (byte) 0, ClickType.PICKUP, Map.of(), ItemStack.Hash.of(ItemStack.AIR, MinecraftServer.process())), player);
    }

    /** Waits out the click guard that a menu-to-menu transition arms. */
    private static void tickPastGuard(Env env) {
        for (int i = 0; i < Menu.DEFAULT_CLICK_GUARD_TICKS; i++) env.tick();
    }

    private static String name(ItemStack item) {
        Component name = item.get(DataComponents.CUSTOM_NAME);
        return name == null ? null : PlainTextComponentSerializer.plainText().serialize(name);
    }

    private static List<Integer> range(int from, int to) {
        return IntStream.range(from, to).boxed().toList();
    }

    private static ItemStack slot(Menu menu, int slot) {
        return menu.getInventory().getItemStack(slot);
    }

    private static void assertContent(Menu menu, List<Integer> expected) {
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemStack item = slot(menu, i);
            if (i < expected.size()) {
                assertEquals(ItemStack.of(Material.PAPER, expected.get(i) % 64 + 1), item, "content slot " + i);
            } else {
                assertTrue(item.isAir(), "content slot " + i + " should be empty, was " + item);
            }
        }
    }

    @Test
    void inMemoryPagingShowsCorrectSliceAndClampsAfterListShrinks(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.items(range(0, 40)); // 3 pages: 18, 18, 4
        menu.show();

        assertContent(menu, range(0, 18));
        assertTrue(slot(menu, LAYOUT.prev()).isAir());
        assertEquals("next 1/3", name(slot(menu, LAYOUT.next())));

        click(player, menu.getInventory(), LAYOUT.next());
        assertContent(menu, range(18, 36));
        assertEquals("prev 2/3", name(slot(menu, LAYOUT.prev())));
        assertEquals("next 2/3", name(slot(menu, LAYOUT.next())));

        click(player, menu.getInventory(), LAYOUT.next());
        assertContent(menu, range(36, 40));
        assertTrue(slot(menu, LAYOUT.next()).isAir());

        click(player, menu.getInventory(), 2);
        assertEquals(List.of(38), menu.clicked);

        click(player, menu.getInventory(), LAYOUT.prev());
        assertContent(menu, range(18, 36));
        click(player, menu.getInventory(), LAYOUT.next());

        // The list shrinks to 2 pages while the player is on page 3: clamp to the new last page.
        menu.items(range(0, 20));
        assertContent(menu, range(18, 20));
        assertEquals("prev 2/2", name(slot(menu, LAYOUT.prev())));
        assertTrue(slot(menu, LAYOUT.next()).isAir());

        // The footer hook's item survived every page change.
        assertEquals(ItemStack.of(Material.HOPPER), slot(menu, FOOTER_EXTRA_SLOT));
        assertTrue(player.getInventory().getCursorItem().isAir());
    }

    @Test
    void setItemsWithResetPageJumpsToTheFirstPage(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.items(range(0, 40));
        menu.show();
        click(player, menu.getInventory(), LAYOUT.next());
        click(player, menu.getInventory(), LAYOUT.next());
        assertContent(menu, range(36, 40));
        int renders = menu.renders;

        // A filter changed: the new list starts on page 1, in one redraw.
        menu.items(range(0, 30), true);
        assertContent(menu, range(0, 18));
        assertTrue(slot(menu, LAYOUT.prev()).isAir());
        assertEquals("next 1/2", name(slot(menu, LAYOUT.next())));
        assertEquals(renders + 1, menu.renders);

        // Without the flag the page is kept (clamped), as before.
        click(player, menu.getInventory(), LAYOUT.next());
        menu.items(range(0, 30), false);
        assertContent(menu, range(18, 30));

        // Not current: no redraw, the next show starts on page 1.
        player.closeInventory();
        renders = menu.renders;
        menu.items(range(0, 30), true);
        assertEquals(renders, menu.renders);
        menu.show();
        assertContent(menu, range(0, 18));
    }

    @Test
    void setItemsWithResetPageLeavesAnAsyncSourceForTheFirstListPage(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        ManualSource source = new ManualSource();
        menu.source(source);
        menu.show();
        source.last().complete(new Page<>(range(0, 18), 100L));
        env.tick();
        click(player, menu.getInventory(), LAYOUT.next());
        source.last().complete(new Page<>(range(18, 36), 200L));
        env.tick();

        // Switching from the source (on its page 2) to a list starts the list on page 1.
        menu.items(range(0, 40), true);

        assertFalse(menu.loading());
        assertContent(menu, range(0, 18));
        assertEquals("next 1/3", name(slot(menu, LAYOUT.next())));
    }

    @Test
    void emptyListShowsEmptyItemAndNoPrevNext(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.items(List.of());
        menu.show();

        long empties = IntStream.range(0, PAGE_SIZE).filter(i -> "empty".equals(name(slot(menu, i)))).count();
        assertEquals(1, empties);
        assertTrue(slot(menu, LAYOUT.prev()).isAir());
        assertTrue(slot(menu, LAYOUT.next()).isAir());
        assertEquals(ItemStack.of(Material.HOPPER), slot(menu, FOOTER_EXTRA_SLOT));
    }

    @Test
    void asyncSourceNextThenPreviousUsesCursorHistory(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        ManualSource source = new ManualSource();
        menu.source(source);
        menu.show();

        assertEquals(1, source.requested.size());
        assertNull(source.requested.getFirst());
        assertTrue(IntStream.range(0, PAGE_SIZE).anyMatch(i -> "loading".equals(name(slot(menu, i)))));
        assertTrue(slot(menu, LAYOUT.next()).isAir());

        source.last().complete(new Page<>(range(0, 18), 100L));
        env.tick();
        assertContent(menu, range(0, 18));
        assertTrue(slot(menu, LAYOUT.prev()).isAir());
        assertEquals("next 1/2", name(slot(menu, LAYOUT.next())));

        click(player, menu.getInventory(), LAYOUT.next());
        assertEquals(100L, source.requested.getLast());
        assertTrue(IntStream.range(0, PAGE_SIZE).anyMatch(i -> "loading".equals(name(slot(menu, i)))));
        source.last().complete(new Page<>(range(18, 30), 200L));
        env.tick();
        assertContent(menu, range(18, 30));
        assertEquals("prev 2/3", name(slot(menu, LAYOUT.prev())));
        assertEquals("next 2/3", name(slot(menu, LAYOUT.next())));

        click(player, menu.getInventory(), LAYOUT.next());
        assertEquals(200L, source.requested.getLast());
        source.last().complete(new Page<>(range(30, 35), null));
        env.tick();
        assertContent(menu, range(30, 35));
        assertEquals("prev 3/3", name(slot(menu, LAYOUT.prev())));
        assertTrue(slot(menu, LAYOUT.next()).isAir());

        click(player, menu.getInventory(), LAYOUT.prev());
        assertEquals(100L, source.requested.getLast());
        source.last().complete(new Page<>(range(18, 30), 200L));
        env.tick();
        assertContent(menu, range(18, 30));

        click(player, menu.getInventory(), LAYOUT.prev());
        assertNull(source.requested.getLast());
        source.last().complete(new Page<>(range(0, 18), 100L));
        env.tick();
        assertContent(menu, range(0, 18));
        assertTrue(slot(menu, LAYOUT.prev()).isAir());
        assertEquals(5, source.requested.size());
        assertEquals(ItemStack.of(Material.HOPPER), slot(menu, FOOTER_EXTRA_SLOT));
    }

    @Test
    void asyncResultDroppedWhenMenuNoLongerCurrent(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        ManualSource source = new ManualSource();
        menu.source(source);
        menu.show();
        int rendersBefore = menu.renders;

        player.openInventory(new Inventory(InventoryType.CHEST_1_ROW, Component.text("other")));
        assertDoesNotThrow(() -> {
            source.last().complete(new Page<>(range(0, 5), null));
            env.tick();
        });

        assertEquals(rendersBefore, menu.renders);
        assertFalse(menu.isCurrent());
    }

    @Test
    void asyncResultDroppedWhenMenuClosedBeforeCompletion(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        ManualSource source = new ManualSource();
        menu.source(source);
        menu.show();
        int rendersBefore = menu.renders;

        player.closeInventory();
        assertDoesNotThrow(() -> {
            source.last().completeExceptionally(new IllegalStateException("late"));
            env.tick();
        });
        assertEquals(rendersBefore, menu.renders);
    }

    @Test
    void onlyTheLatestOfTwoRapidLoadsRenders(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        ManualSource first = new ManualSource();
        ManualSource second = new ManualSource();
        menu.source(first);
        menu.show();
        menu.source(second);

        second.last().complete(new Page<>(range(10, 12), null));
        env.tick();
        first.last().complete(new Page<>(range(0, 5), null));
        env.tick();

        assertContent(menu, range(10, 12));
    }

    @Test
    void backButtonOnlyWhenParent(Env env) {
        Player player = player(env);
        ListMenu root = new ListMenu(player);
        root.items(range(0, 3));
        root.show();
        assertTrue(slot(root, LAYOUT.back()).isAir());

        ListMenu child = new ListMenu(player);
        child.items(range(5, 6));
        root.openChild(child);
        assertEquals("back", name(slot(child, LAYOUT.back())));
        tickPastGuard(env);

        click(player, child.getInventory(), LAYOUT.back());
        assertTrue(root.isCurrent());
        assertTrue(slot(root, LAYOUT.back()).isAir());
        assertContent(root, range(0, 3));
    }

    @Test
    void backToAsyncParentKeepsItsPageWithoutRefetching(Env env) {
        Player player = player(env);
        ListMenu root = new ListMenu(player);
        ManualSource source = new ManualSource();
        root.source(source);
        root.show();
        source.last().complete(new Page<>(range(0, 18), 7L));
        env.tick();
        click(player, root.getInventory(), LAYOUT.next());
        source.last().complete(new Page<>(range(18, 20), null));
        env.tick();
        assertEquals(2, source.requested.size());

        ListMenu child = new ListMenu(player);
        child.items(List.of());
        root.openChild(child);
        tickPastGuard(env);
        click(player, child.getInventory(), LAYOUT.back());

        assertTrue(root.isCurrent());
        assertEquals(2, source.requested.size());
        assertContent(root, range(18, 20));
        assertEquals("prev 2/2", name(slot(root, LAYOUT.prev())));

        // An explicit reload refetches the page the player is on.
        root.reload();
        assertEquals(3, source.requested.size());
        assertEquals(7L, source.requested.getLast());
    }

    @Test
    void pageLoadedWhileCoveredByChildShowsOnBack(Env env) {
        Player player = player(env);
        ListMenu root = new ListMenu(player);
        ManualSource source = new ManualSource();
        root.source(source);
        root.show();
        ListMenu child = new ListMenu(player);
        child.items(List.of());
        root.openChild(child);
        int rootRenders = root.renders;

        source.last().complete(new Page<>(range(0, 4), null));
        env.tick();
        assertEquals(rootRenders, root.renders);

        tickPastGuard(env);
        click(player, child.getInventory(), LAYOUT.back());
        assertContent(root, range(0, 4));
        assertEquals(1, source.requested.size());
    }

    private static boolean shows(Menu menu, String name) {
        return IntStream.range(0, PAGE_SIZE).anyMatch(i -> name.equals(name(slot(menu, i))));
    }

    @Test
    void failedFirstLoadShowsEmptyAndReportsError(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        ManualSource source = new ManualSource();
        menu.source(source);
        menu.show();

        source.last().completeExceptionally(new IllegalStateException("db down"));
        env.tick();

        assertFalse(menu.loading());
        assertFalse(shows(menu, "loading"));
        assertTrue(shows(menu, "empty"));
        assertEquals(1, menu.errors.size());
        assertInstanceOf(IllegalStateException.class, menu.errors.getFirst());
        assertEquals(ItemStack.of(Material.HOPPER), slot(menu, FOOTER_EXTRA_SLOT));
    }

    @Test
    void failedNextKeepsHistoryAndPreviousPage(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        ManualSource source = new ManualSource();
        menu.source(source);
        menu.show();
        source.last().complete(new Page<>(range(0, 18), 100L));
        env.tick();

        click(player, menu.getInventory(), LAYOUT.next());
        source.last().completeExceptionally(new IllegalStateException("timeout"));
        env.tick();

        assertFalse(menu.loading());
        assertEquals(1, menu.errors.size());
        assertContent(menu, range(0, 18));
        assertTrue(slot(menu, LAYOUT.prev()).isAir());
        assertEquals("next 1/2", name(slot(menu, LAYOUT.next())));

        // Next works again and the history was not corrupted by the failure.
        click(player, menu.getInventory(), LAYOUT.next());
        assertEquals(100L, source.requested.getLast());
        source.last().complete(new Page<>(range(18, 20), null));
        env.tick();
        assertContent(menu, range(18, 20));
        assertEquals("prev 2/2", name(slot(menu, LAYOUT.prev())));
        click(player, menu.getInventory(), LAYOUT.prev());
        assertNull(source.requested.getLast());
    }

    @Test
    void onLoadErrorOnlyWhileVisible(Env env) {
        Player player = player(env);
        ListMenu root = new ListMenu(player);
        ManualSource source = new ManualSource();
        root.source(source);
        root.show();
        ListMenu child = new ListMenu(player);
        child.items(List.of());
        root.openChild(child);

        source.last().completeExceptionally(new IllegalStateException("covered"));
        env.tick();
        assertTrue(root.errors.isEmpty());
        assertFalse(root.loading());

        tickPastGuard(env);
        click(player, child.getInventory(), LAYOUT.back());
        assertTrue(shows(root, "empty"));

        root.reload();
        player.closeInventory();
        source.last().completeExceptionally(new IllegalStateException("closed"));
        env.tick();
        assertTrue(root.errors.isEmpty());
        assertFalse(root.loading());
    }

    @Test
    void synchronousThrowFromLoadIsAFailedLoad(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.source(cursor -> { throw new IllegalStateException("sync"); });
        menu.show();
        env.tick();

        assertFalse(menu.loading());
        assertTrue(shows(menu, "empty"));
        assertEquals(1, menu.errors.size());
        assertEquals("sync", menu.errors.getFirst().getMessage());
    }

    @Test
    void nullFutureFromLoadIsAFailedLoad(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.source(cursor -> null);
        menu.show();
        env.tick();

        assertFalse(menu.loading());
        assertTrue(shows(menu, "empty"));
        assertEquals(1, menu.errors.size());
        assertInstanceOf(NullPointerException.class, menu.errors.getFirst());
    }

    @Test
    void throwingOnLoadErrorStillRedraws(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.throwOnError = true;
        ManualSource source = new ManualSource();
        menu.source(source);
        menu.show();

        source.last().completeExceptionally(new IllegalStateException("boom"));
        assertDoesNotThrow(() -> env.tick());

        assertEquals(1, menu.errors.size());
        assertFalse(menu.loading());
        assertFalse(shows(menu, "loading"));
        assertTrue(shows(menu, "empty"));
    }

    @Test
    void loadAsyncRunsOnlyWhileCurrentAndRoutesErrors(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.items(List.of());
        menu.show();

        AtomicReference<String> rendered = new AtomicReference<>();
        AtomicReference<Throwable> failed = new AtomicReference<>();
        CompletableFuture<String> ok = new CompletableFuture<>();
        CompletableFuture<String> bad = new CompletableFuture<>();
        menu.loadAsync(ok, rendered::set);
        menu.loadAsync(bad, rendered::set, failed::set);

        ok.complete("hello");
        assertNull(rendered.get(), "runs on the player's next tick, not inline");
        env.tick();
        assertEquals("hello", rendered.get());

        bad.completeExceptionally(new IllegalStateException("boom"));
        env.tick();
        assertInstanceOf(IllegalStateException.class, failed.get());

        AtomicInteger late = new AtomicInteger();
        CompletableFuture<String> stale = new CompletableFuture<>();
        menu.loadAsync(stale, s -> late.incrementAndGet(), t -> late.incrementAndGet());
        player.closeInventory();
        stale.complete("late");
        env.tick();
        assertEquals(0, late.get());
    }

    @Test
    void throwingFooterOnLoadingDrawStillShowsLoadedPage(Env env) {
        Player player = player(env);
        ListMenu menu = new ListMenu(player);
        menu.show();
        ManualSource source = new ManualSource();
        menu.failFooterDraws = 1;

        assertDoesNotThrow(() -> menu.source(source));
        source.last().complete(new Page<>(range(0, 5), null));
        env.tick();

        assertFalse(menu.loading());
        assertContent(menu, range(0, 5));
        assertEquals(ItemStack.of(Material.HOPPER), slot(menu, FOOTER_EXTRA_SLOT));
    }
}
