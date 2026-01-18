package club.tesseract.minestom.utils.feature;

import club.tesseract.minestom.utils.feature.listener.ListenerFeature;
import lombok.Getter;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class FeatureContainer {
    private final EventNode<? extends Event> eventNode;
    private final HashMap<Class<? extends Feature>, Feature> features = new HashMap<>();
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public FeatureContainer(EventNode<? extends Event> eventNode, Feature... feature) {
        this.eventNode = eventNode;
        for (Feature f : feature) {
            this.features.put(f.getClass(), f);
        }
    }

    public void register(Feature feature){
        features.put(feature.getClass(), feature);
        if(enabled.get()) feature.onEnable();
    }

    public void onEnable() {
        if (enabled.getAndSet(true)) return;
        features.values().forEach(Feature::onEnable);
        features.values().stream().filter(feature -> feature instanceof ListenerFeature).forEach(feature -> ((ListenerFeature)feature).onEventRegistration(eventNode));
    }

    public void onDisable() {
        if (!enabled.getAndSet(false)) return;
        features.values().forEach(Feature::onDisable);
        features.values().stream().filter(feature -> feature instanceof ListenerFeature).forEach(feature -> ((ListenerFeature)feature).onEventDeregistration(eventNode));
    }

}
