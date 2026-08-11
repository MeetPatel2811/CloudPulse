package com.cloudpulse.observer;

import com.cloudpulse.model.StatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reacts to a {@link StatusEvent} by appending it to an in-memory chronological
 * timeline, mirroring the event history the dashboard's timeline view displays.
 * This class does not persist events — that is owned by the repository package.
 */
@Component
public class EventHistoryObserver implements EventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(EventHistoryObserver.class);

    private final List<StatusEvent> history = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(StatusEvent event) {
        history.add(event);
        log.info("Event history recorded: service '{}' {} -> {}",
                event.getService().getName(), event.getPreviousStatus(), event.getNewStatus());
    }

    /** The events recorded so far, in the order they were received. */
    public List<StatusEvent> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
