package club.tesseract.minestom.test;

import club.tesseract.minestom.utils.feature.FeatureContainer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

public class FeatureHolder implements club.tesseract.minestom.utils.feature.FeatureHolder {

    private final EventNode<Event> eventNode;

    public FeatureHolder(EventNode<Event> eventNode) {
        this.eventNode = eventNode;
    }

    @Override
    public FeatureContainer getFeatureContainer() {
        return new FeatureContainer(eventNode);
    }

}
