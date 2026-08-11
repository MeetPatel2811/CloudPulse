package com.cloudpulse.observer;

import com.cloudpulse.model.StatusEvent;

/**
 * Observer pattern — the common interface for every component that reacts to a
 * service health-state transition (dashboard refresh, alerting, history).
 *
 * <p>Subscribers register with {@link StatusEventPublisher} and are notified through
 * this single {@link #onEvent(StatusEvent)} entry point, so the publisher never needs
 * to know which concrete reactions exist.</p>
 */
public interface EventSubscriber {
    void onEvent(StatusEvent event);
}
