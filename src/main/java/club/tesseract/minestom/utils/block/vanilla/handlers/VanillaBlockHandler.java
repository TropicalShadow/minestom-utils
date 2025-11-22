package club.tesseract.minestom.utils.block.vanilla.handlers;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTag;
import net.minestom.server.registry.TagKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class VanillaBlockHandler {


    public static BlockManager registerVanillaBlockHandlers() {
        BlockManager blockManager = MinecraftServer.getBlockManager();

        RegistryTag<@NotNull Block> tag = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:all_signs"));
        SignHandler signHandler = new SignHandler();
        for (RegistryKey<@NotNull Block> key : Objects.requireNonNull(tag)) {
            blockManager.registerHandler(key.key(), () -> signHandler);
        }
        blockManager.registerHandler(SignHandler.KEY, SignHandler::getINSTANCE);
        blockManager.registerHandler(HangingSign.KEY, HangingSign::getINSTANCE);
        blockManager.registerHandler(SkullHandler.KEY, SkullHandler::getINSTANCE);
        blockManager.registerHandler(PlayerHead.KEY, PlayerHead::getINSTANCE);
        blockManager.registerHandler(BannerHandler.KEY, BannerHandler::getINSTANCE);
        return blockManager;
    }

}
