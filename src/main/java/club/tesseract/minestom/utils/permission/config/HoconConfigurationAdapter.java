package club.tesseract.minestom.utils.permission.config;

import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class HoconConfigurationAdapter extends ModernConfigurateConfigAdapter implements ConfigurationAdapter {

    public HoconConfigurationAdapter(LuckPermsPlugin plugin) {
        super(plugin, ((LPMinestomPlugin) plugin).resolveConfig("luckperms.conf"));
    }

    @Override
    protected ConfigurationLoader<? extends @NotNull ConfigurationNode> createLoader(Path path) {
        HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder();
        builder.setPath(path);
        return builder.build();
    }

}