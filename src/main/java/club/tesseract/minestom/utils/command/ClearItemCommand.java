package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.args.PlayerArgument;
import club.tesseract.minestom.utils.command.condition.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentItemStack;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Clear Inventory Command from Vanilla Minecraft<br/>
 * Usage: /clear [players] [item] [maxCount]<br/>
 * Permission: minecraft.command.clear or Operator
 *
 * @author TropicalShadow
 * @implNote ItemStack Predicates are not how they are in vanilla and require an exact match
 */
public final class ClearItemCommand extends Command {

    private static final Function<String, Component> ERROR_SINGLE = (name) -> Component.translatable("clear.failed.single", NamedTextColor.RED).arguments(Component.text(name));
    private static final Function<Integer, Component> ERROR_MULTIPLE = (count) -> Component.translatable("clear.failed.multiple", NamedTextColor.RED).arguments(Component.text(count));

    private static final Component PLAYER_ONLY_ERROR = Component.translatable("permissions.requires.player", NamedTextColor.RED);
    private static final PlayerArgument PLAYER_ARGUMENT = new PlayerArgument("players", false);
    private static final ArgumentItemStack ITEM_ARGUMENT = ArgumentType.ItemStack("item");
    private static final ArgumentInteger MAX_COUNT_ARGUMENT = ArgumentType.Integer("maxCount");

    public ClearItemCommand() {
        super("clear");

        setCondition(
                ExtraConditions.or(
                        ExtraConditions.hasPermission("minecraft.command.clear"),
                        ExtraConditions.isOp()
                )
        );

        setDefaultExecutor((sender, _) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(PLAYER_ONLY_ERROR);
                return;
            }
            clearUnlimited(sender, List.of(player), ignored -> true);
        });

        addSyntax((sender, context) -> {
            final List<Player> targets = PLAYER_ARGUMENT.getPlayers(sender, context);

            clearUnlimited(sender, targets, ignored -> true);
        }, PLAYER_ARGUMENT);

        addSyntax((sender, context) -> {
            final List<Player> targets = PLAYER_ARGUMENT.getPlayers(sender, context);
            final ItemStack itemStack = context.get(ITEM_ARGUMENT);

            clearUnlimited(sender, targets, stack -> stack.isSimilar(itemStack));
        }, PLAYER_ARGUMENT, ITEM_ARGUMENT);

        addSyntax((sender, context) -> {
            final List<Player> targets = PLAYER_ARGUMENT.getPlayers(sender, context);
            final ItemStack itemStack = context.get(ITEM_ARGUMENT);
            final int maxCount = context.get(MAX_COUNT_ARGUMENT);

            clearInventory(sender, targets, stack -> stack.isSimilar(itemStack), maxCount);
        }, PLAYER_ARGUMENT, ITEM_ARGUMENT, MAX_COUNT_ARGUMENT);
    }

    private void clearUnlimited(@NotNull CommandSender sender, @NotNull List<@NotNull Player> players, @NotNull Predicate<@NotNull ItemStack> predicate) {
        clearInventory(sender, players, predicate, -1);
    }

    private void clearInventory(@NotNull CommandSender sender, @NotNull List<@NotNull Player> players, @NotNull Predicate<@NotNull ItemStack> predicate, int maxCount) {
        final AtomicInteger totalCount = new AtomicInteger();

        players.forEach(player -> totalCount.addAndGet(clearOrCountSimilarItems(player.getInventory(), predicate, maxCount)));

        if (totalCount.get() == 0) {
            if (players.size() == 1) {
                sender.sendMessage(ERROR_SINGLE.apply(players.getFirst().getUsername()));
            } else {
                sender.sendMessage(ERROR_MULTIPLE.apply(players.size()));
            }
            return;
        }

        final int finalCount = totalCount.get();
        if (maxCount == 0) {
            if (players.size() == 1) {
                sender.sendMessage(
                        Component.translatable("commands.clear.test.single", NamedTextColor.GRAY).arguments(Component.text(finalCount), players.getFirst().getName())
                );
                return;
            }
            sender.sendMessage(
                    Component.translatable("commands.clear.test.multiple", NamedTextColor.GRAY).arguments(Component.text(finalCount), Component.text(players.size()))
            );
        } else if (players.size() == 1) {
            sender.sendMessage(
                    Component.translatable("commands.clear.success.single", NamedTextColor.GRAY).arguments(Component.text(finalCount), players.getFirst().getName())
            );
            return;
        } else {
            sender.sendMessage(
                    Component.translatable("commands.clear.success.multiple", NamedTextColor.GRAY).arguments(Component.text(finalCount), Component.text(players.size()))
            );
        }
    }


    private int clearOrCountSimilarItems(PlayerInventory inventory, Predicate<ItemStack> predicate, int maxCount) {
        int count = 0;
        boolean isDryRun = maxCount == 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack itemStack = inventory.getItemStack(slot);
            if (itemStack.isAir()) continue;
            if (predicate.test(itemStack)) {
                int itemCount = itemStack.amount();
                if (maxCount < 0 || count + itemCount <= maxCount) {
                    if (!isDryRun) {
                        inventory.setItemStack(slot, ItemStack.AIR);
                    }
                    count += itemCount;
                } else {
                    int toRemove = maxCount - count;
                    if (!isDryRun) {
                        inventory.setItemStack(slot, itemStack.withAmount(itemCount - toRemove));
                    }
                    count += toRemove;
                    break; // Reached maxCount
                }
            }
        }
        return count;
    }

}
