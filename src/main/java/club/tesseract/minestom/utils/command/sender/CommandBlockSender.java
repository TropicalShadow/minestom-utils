package club.tesseract.minestom.utils.command.sender;

import lombok.Getter;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;

public final class CommandBlockSender implements CommandSender {
    private static final ComponentLogger LOGGER = ComponentLogger.logger(ConsoleSender.class);
    private static final Component DEFAULT_NAME = Component.text("@");

    @Getter
    private final Block block;
    private final Vec position;

    private final TagHandler tagHandler = TagHandler.newHandler();

    private final Identity identity = Identity.nil();
    private final Pointers pointers = Pointers.builder()
            .withStatic(Identity.UUID, this.identity.uuid())
            .build();


    public CommandBlockSender(Block block, Vec position) {
        this.block = block;
        this.position = position;
    }


    public Vec getVec() {
        return position;
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        LOGGER.info(Component.join(
                JoinConfiguration.spaces(),
                DEFAULT_NAME,
                message
        ));
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }

    @Override
    public @NotNull TagHandler tagHandler() {
        return tagHandler;
    }

    @NotNull
    public Pointers getPointers() {
        return pointers;
    }
}
