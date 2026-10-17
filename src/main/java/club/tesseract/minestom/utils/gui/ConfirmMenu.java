package club.tesseract.minestom.utils.gui;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A yes/no prompt: confirm on slot 11, the subject being confirmed on slot 13, cancel on slot 15, filler around.
 * <p>
 * Exactly one of the callbacks runs, once. The first click on confirm or cancel decides: the decision is recorded
 * before any callback runs (so rapid extra clicks do nothing), then the callback runs, then the menu is left with
 * {@link #back()} if it has a parent, else by closing the inventory. Closing without a click (Esc, or another
 * inventory replacing the menu) runs {@code onCancel} on the player's next tick (the decision itself is recorded at
 * once), because Minestom is still closing the inventory when the menu hears about it; the close that follows a click
 * does not run it. A click decides and runs its callback synchronously.
 * A callback that throws is logged at warn level; the menu is still left and the other callback does not run.
 * Rendering the menu again (show, refresh, back) starts a new decision. Either callback may open another inventory:
 * after a click the menu is then not closed over it, and after Esc the deferred {@code onCancel} opens it once the
 * close has finished, so it stays open.
 * When another inventory replaced the menu, that inventory is already open when {@code onCancel} runs; check
 * {@code player.getOpenInventory()} before opening one.
 * Like {@link Menu#onClose(Player)}, {@code onCancel} does not run if the player disconnects.
 */
public class ConfirmMenu extends Menu {
    public static final int CONFIRM_SLOT = 11;
    public static final int SUBJECT_SLOT = 13;
    public static final int CANCEL_SLOT = 15;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmMenu.class);

    private final ItemStack subject;
    private final ButtonStyle style;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private boolean decided = false;

    public ConfirmMenu(@NotNull Player player, @NotNull Component title, @NotNull ItemStack subject,
                       @NotNull ButtonStyle style, @NotNull Runnable onConfirm, @NotNull Runnable onCancel) {
        super(player, InventoryType.CHEST_3_ROW, title);
        this.subject = subject;
        this.style = style;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void render() {
        this.decided = false; // every show, refresh or back() into this menu starts a new decision
        fillBorder(this.style.filler()); // first: it wipes anything already on the border slots
        set(CONFIRM_SLOT, this.style.confirm(), click -> decide(this.onConfirm));
        set(SUBJECT_SLOT, this.subject);
        set(CANCEL_SLOT, this.style.cancel(), click -> decide(this.onCancel));
    }

    @Override
    protected void onClose(@NotNull Player player) {
        if (this.decided) return;
        this.decided = true;
        // Inside Minestom's close: it clears the open inventory after this returns, so opening a menu here would be lost.
        player.scheduler().scheduleNextTick(() -> run(this.onCancel));
    }

    private void decide(Runnable callback) {
        if (this.decided) return;
        this.decided = true;
        try {
            run(callback);
        } finally {
            if (hasParent()) back();
            else if (isCurrent()) this.player.closeInventory(); // the callback may have opened something else
        }
    }

    private static void run(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException e) {
            LOGGER.warn("Confirm menu callback failed", e);
        }
    }
}
