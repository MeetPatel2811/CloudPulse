package com.cloudpulse.observer;

import com.cloudpulse.model.StatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Observer pattern — the subject. Holds every registered {@link EventSubscriber} and
 * notifies all of them whenever a {@link StatusEvent} is published, so the components
 * that react to a health-state transition (dashboard, alerts, history) stay decoupled
 * from whatever produced the event.
 */
@Component
public class StatusEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StatusEventPublisher.class);

    private final List<EventSubscriber> subscribers = new ArrayList<>();

    /** Adds a subscriber that will be notified on every future {@link #publish(StatusEvent)}. */
    public void register(EventSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Notifies every registered subscriber, in registration order, of the given event.
     * Each subscriber is isolated from the others: one throwing does not stop the rest
     * from being notified, it is only caught and logged.
     */
    public void publish(StatusEvent event) {
        for (EventSubscriber subscriber : subscribers) {
            try {
                subscriber.onEvent(event);
            } catch (Exception e) {
                log.warn("Subscriber {} failed to handle status event {}",
                        subscriber.getClass().getSimpleName(), event.getId(), e);
            }
        }
    }
}
