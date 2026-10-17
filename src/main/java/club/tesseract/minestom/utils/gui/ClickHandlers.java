package club.tesseract.minestom.utils.gui;

import net.minestom.server.inventory.click.Click;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Click handlers for one menu slot, split by click type.
 * <p>
 * {@link #handle(Click)} runs the most specific handler: {@code left} for a left click, {@code right} for a right
 * click, {@code shift} for a left or right shift-click, and {@code any} for everything else (number keys, drop key,
 * double click, ...) or when the specific handler is not set.
 *
 * <pre>{@code
 * set(13, item, ClickHandlers.any(c -> buy()).right(c -> preview()));
 * }</pre>
 */
public final class ClickHandlers {

    private static final Consumer<Click> NOOP = c -> {};

    private final @NotNull Consumer<Click> any;
    private @Nullable Consumer<Click> left;
    private @Nullable Consumer<Click> right;
    private @Nullable Consumer<Click> shift;

    private ClickHandlers(@NotNull Consumer<Click> any) {
        this.any = any;
    }

    /** Handlers whose fallback for every click type is {@code handler}. */
    public static @NotNull ClickHandlers any(@NotNull Consumer<Click> handler) {
        return new ClickHandlers(handler);
    }

    /** Handlers that do nothing; the click is still cancelled by the menu. */
    public static @NotNull ClickHandlers none() {
        return new ClickHandlers(NOOP);
    }

    public @NotNull ClickHandlers left(@NotNull Consumer<Click> handler) {
        this.left = handler;
        return this;
    }

    public @NotNull ClickHandlers right(@NotNull Consumer<Click> handler) {
        this.right = handler;
        return this;
    }

    public @NotNull ClickHandlers shift(@NotNull Consumer<Click> handler) {
        this.shift = handler;
        return this;
    }

    /** Runs the most specific handler for {@code click}. */
    public void handle(@NotNull Click click) {
        Consumer<Click> specific = switch (click) {
            case Click.Left _ -> left;
            case Click.Right _ -> right;
            case Click.LeftShift _, Click.RightShift _ -> shift;
            default -> null;
        };
        (specific != null ? specific : any).accept(click);
    }
}
