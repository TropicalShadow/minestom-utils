package club.tesseract.minestom.utils.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The menus a player can go {@link Menu#back() back} to, newest last. One per player, kept on the player in
 * {@link Menu#NAV_TAG}, so it goes away with the player.
 */
public final class NavStack {
    private final Deque<Menu> menus = new ArrayDeque<>();

    public void push(@NotNull Menu menu) {
        this.menus.push(menu);
    }

    /** Removes and returns the newest menu, or null when empty. */
    @Nullable
    public Menu pop() {
        return this.menus.poll();
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
