package club.tesseract.minestom.utils.command;

import club.tesseract.minestom.utils.command.vanilla.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);
    private static final Map<CommandCategory, List<Command>> commands = new EnumMap<>(CommandCategory.class);
    private static final List<Command> registeredCommands = new ArrayList<>();
    private static boolean initialized = false;

    private CommandRegistry() {}

    /**
     * Initialize the registry with all available commands
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warn("CommandRegistry already initialized!");
            return;
        }

        LOGGER.info("Initializing command registry...");

        // Register all commands with metadata
        registerCommand(new StatsCommand());
        registerCommand(new StopCommand());
        registerCommand(new SudoCommand());
        registerCommand(new TpsCommand());
        registerCommand(new TeleportCommand());
        registerCommand(new GameModeCommand());
        registerCommand(new FlyCommand());
        registerCommand(new HealCommand());
        registerCommand(new NukeCommand());
        registerCommand(new PlaySoundCommand());
        registerCommand(new GiveItemCommand());
        registerCommand(new ClearItemCommand());
        registerCommand(new KillCommand());
        registerCommand(new ParticleCommand());

        initialized = true;
        LOGGER.info("Command registry initialized with {} commands", registeredCommands.size());
    }

    /**
     * Register a single command with automatic categorization
     */
    private static void registerCommand(Command command) {
        CommandMetadata metadata = command.getClass().getAnnotation(CommandMetadata.class);

        if (metadata == null) {
            LOGGER.warn("Command {} missing @CommandMetadata annotation, skipping",
                    command.getClass().getSimpleName());
            return;
        }

        if (!metadata.enabled()) {
            LOGGER.debug("Command {} is disabled, skipping", command.getClass().getSimpleName());
            return;
        }

        // Register command in all specified categories
        for (CommandCategory category : metadata.categories()) {
            commands.computeIfAbsent(category, k -> new ArrayList<>()).add(command);
            LOGGER.debug("Registered command: {} in category [{}]",
                    command.getClass().getSimpleName(), category.getDisplayName());
        }

        // Add to global list only once
        registeredCommands.add(command);
    }

    /**
     * Register all commands with the server
     */
    public static void registerAll() {
        if (!initialized) {
            initialize();
        }

        LOGGER.info("Registering all commands with MinecraftServer...");

        // Sort by priority before registering
        List<Command> sortedCommands = registeredCommands.stream()
                .sorted(Comparator.comparingInt(cmd -> {
                    CommandMetadata meta = cmd.getClass().getAnnotation(CommandMetadata.class);
                    return meta != null ? meta.priority() : 0;
                }))
                .collect(Collectors.toList());

        MinecraftServer.getCommandManager().register(
                sortedCommands.toArray(new Command[0])
        );

        LOGGER.info("Successfully registered {} commands", sortedCommands.size());
    }

    /**
     * Register commands by category
     */
    public static void registerCategory(CommandCategory category) {
        if (!initialized) {
            initialize();
        }

        List<Command> categoryCommands = commands.get(category);
        if (categoryCommands == null || categoryCommands.isEmpty()) {
            LOGGER.warn("No commands found for category: {}", category.getDisplayName());
            return;
        }

        LOGGER.info("Registering {} commands from category: {}",
                categoryCommands.size(), category.getDisplayName());

        MinecraftServer.getCommandManager().register(
                categoryCommands.toArray(new Command[0])
        );
    }

    /**
     * Register multiple categories at once
     */
    public static void registerCategories(CommandCategory... categories) {
        for (CommandCategory category : categories) {
            registerCategory(category);
        }
    }

    /**
     * Get all commands in a specific category
     */
    public static List<Command> getCommandsByCategory(CommandCategory category) {
        return Collections.unmodifiableList(
                commands.getOrDefault(category, Collections.emptyList())
        );
    }

    /**
     * Get command statistics
     */
    public static Map<CommandCategory, Integer> getCommandStats() {
        return commands.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().size(),
                        (a, b) -> a,
                        () -> new EnumMap<>(CommandCategory.class)
                ));
    }

    /**
     * Print command registry information
     */
    public static void printStats() {
        LOGGER.info("=== Command Registry Statistics ===");
        commands.forEach((category, cmds) ->
                LOGGER.info("  {}: {} commands", category.getDisplayName(), cmds.size())
        );
        LOGGER.info("  Total: {} commands", registeredCommands.size());
    }

    /**
     * Clear all registered commands (mainly for testing)
     */
    public static void reset() {
        commands.clear();
        registeredCommands.clear();
        initialized = false;
        LOGGER.info("Command registry reset");
    }
}