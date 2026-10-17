package club.tesseract.minestom.utils.gui;

import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An on/off item for one menu slot. A click flips the value, tells {@code onChange} about the new value and redraws
 * only that slot; the rest of the menu is left alone.
 * <p>
 * It reaches {@code Menu.set} through the protected (hence same-package) access of {@link Menu}, so the public API of
 * {@code Menu} does not grow.
 */
public final class Toggle {

    private Toggle() {
    }

    /**
     * Draws the toggle on {@code slot} and wires its click. Call it from {@code render()}: {@code refresh()} wipes every
     * handler, so a toggle set elsewhere stops working after a refresh.
     *
     * @param state    the current value, read on every draw and click
     * @param render   the item for a value
     * @param onChange called with the flipped value (the opposite of {@code state} at click time); it should store
     *                 it so {@code state} returns it afterwards. If it throws, the slot is not redrawn
     */
    public static void set(@NotNull Menu menu, int slot, @NotNull Supplier<Boolean> state,
                           @NotNull Function<Boolean, ItemStack> render, @NotNull Consumer<Boolean> onChange) {
        menu.set(slot, render.apply(state.get()), click -> {
            onChange.accept(!state.get());
            set(menu, slot, state, render, onChange);
        });
    }
}
