package club.tesseract.minestom.utils.gui;

import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An on/off item for one menu slot. A click flips the value, tells {@code onChange} about the new value and redraws
 * only that slot; the rest of the menu is left alone.
 * <p>
 * Toggle is the supported way to attach a click handler to a menu slot from outside the menu class: it calls
 * {@code Menu.set} through same-package access.
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
    public static void set(@NotNull Menu menu, int slot, @NotNull BooleanSupplier state,
                           @NotNull Function<Boolean, ItemStack> render, @NotNull Consumer<Boolean> onChange) {
        menu.set(slot, render.apply(state.getAsBoolean()), click -> {
            onChange.accept(!state.getAsBoolean());
            set(menu, slot, state, render, onChange);
        });
    }
}
