package club.tesseract.minestom.utils.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The menus a player can go {@link Menu#back() back} to, newest last. One per player, kept on the player in
 * {@code Menu.NAV_TAG}, so it goes away with the player. Internal to the menu framework.
 */
final class NavStack {
    private final Deque<Menu> menus = new ArrayDeque<>();

    public void push(@NotNull Menu menu) {
        this.menus.push(menu);
    }

    /** Removes and returns the newest menu, or null when empty. */
    @Nullable
    public Menu pop() {
        return this.menus.poll();
    }

    /** True when {@code menu} is anywhere in the stack. */
    public boolean contains(@NotNull Menu menu) {
        return this.menus.contains(menu);
    }

    public boolean isEmpty() {
        return this.menus.isEmpty();
    }

    public int size() {
        return this.menus.size();
    }

    public void clear() {
        this.menus.clear();
    }
}
