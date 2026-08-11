package com.cloudpulse.observer;

import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reacts to a {@link StatusEvent} by noting that an unhealthy transition occurred.
 * This is the Observer-side notification only: it keeps an in-memory list of the
 * events it reacted to for demonstration/testing and does not create or persist
 * {@code Alert} entities — that is owned by the handler/repository packages.
 */
@Component
public class AlertObserver implements EventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(AlertObserver.class);

    private final List<StatusEvent> triggeringEvents = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(StatusEvent event) {
        if (event.getNewStatus() == ServiceStatus.DOWN || event.getNewStatus() == ServiceStatus.DEGRADED) {
            triggeringEvents.add(event);
            log.warn("Alert-worthy transition: service '{}' went from {} to {}",
                    event.getService().getName(), event.getPreviousStatus(), event.getNewStatus());
        }
    }

    /** The events this observer has reacted to, in the order they were received. */
    public List<StatusEvent> getTriggeringEvents() {
        return Collections.unmodifiableList(triggeringEvents);
    }
}
