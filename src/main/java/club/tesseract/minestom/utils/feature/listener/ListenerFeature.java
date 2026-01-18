package club.tesseract.minestom.utils.feature.listener;

import club.tesseract.minestom.utils.feature.Feature;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

public interface ListenerFeature extends Feature {

    void onEventRegistration(EventNode<? extends Event> eventNode);

    void onEventDeregistration(EventNode<? extends Event> eventNode);
}
