package club.tesseract.minestom.utils.misc.lang;


import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minestom.server.ServerFlag;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author TropicalShadow
 * @see ServerFlag#AUTOMATIC_COMPONENT_TRANSLATION
 */
public final class LangUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LangUtils.class);
    private static final Map<String, MiniMessageTranslationStore> TRANSLATORS = new ConcurrentHashMap<>();

    /**
     * Registers a resource bundle as a MiniMessage translation source for the given namespace.
     * Automatically enables {@link ServerFlag#AUTOMATIC_COMPONENT_TRANSLATION} so that
     * {@link Component#translatable} components are rendered server-side before being sent to clients.
     */
    public static void registerLang(@Subst("gamemode") String namespace, String baseBundlePath, Locale... supportedLocales) {
        System.setProperty("minestom.automatic-component-translation", "true"); // @see ServerFlag.AUTOMATIC_COMPONENT_TRANSLATION
        String resourcePath = baseBundlePath.replace('.', '/') + ".properties";
        if (LangUtils.class.getClassLoader().getResource(resourcePath) == null) {
            LOGGER.warn("WARNING: Resource not found: {}", resourcePath);
            LOGGER.warn("Check if the module containing this resource is properly built");
        }

        MiniMessageTranslationStore store = TRANSLATORS.computeIfAbsent(namespace, ns -> {
            MiniMessageTranslationStore newTranslator = MiniMessageTranslationStore.create(Key.key(ns, "translations"), MiniMessageHelper.MINI_MESSAGE);
            GlobalTranslator.translator().addSource(newTranslator);
            return newTranslator;
        });

        if (supportedLocales.length == 0) {
            supportedLocales = new Locale[]{Locale.UK};
        }
        for (Locale locale : supportedLocales) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(baseBundlePath, locale);
                // MiniMessage doesn't use MessageFormat, so escapeSingleQuotes should be false
                store.registerAll(locale, bundle, false);
                LOGGER.info("Registered bundle: {} for locale: {} in namespace: {}", baseBundlePath, locale, namespace);
                LOGGER.info("Bundle keys: {}", bundle.keySet());
            } catch (Exception e) {
                LOGGER.warn("Failed to load bundle: {} for locale: {}", baseBundlePath, locale, e);
            }
        }
    }

    @NotNull
    public static Component serverRender(Audience audience, String langKey, ComponentLike... args) {
        Locale locale = audience.getOrDefault(Identity.LOCALE, Locale.UK);
        return serverRender(locale, langKey, args);
    }

    @NotNull
    public static Component serverRender(Locale locale, String langKey, ComponentLike... args) {
        return GlobalTranslator.render(Component.translatable(langKey).arguments(args), locale).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE); // prevent italics defaulting in ui elements
    }

    private LangUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}