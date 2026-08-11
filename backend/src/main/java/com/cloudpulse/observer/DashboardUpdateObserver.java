package com.cloudpulse.observer;

import com.cloudpulse.model.StatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reacts to a {@link StatusEvent} by refreshing the in-memory view the React dashboard
 * polls. This class only tracks the latest known status per service (keyed by service
 * name, which is always set); it does not touch any repository — persistence is owned
 * by the model/repository packages.
 */
@Component
public class DashboardUpdateObserver implements EventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(DashboardUpdateObserver.class);

    private final Map<String, StatusEvent> latestByService = new ConcurrentHashMap<>();

    @Override
    public void onEvent(StatusEvent event) {
        latestByService.put(event.getService().getName(), event);
        log.info("Dashboard updated: service '{}' is now {}",
                event.getService().getName(), event.getNewStatus());
    }

    /** The most recently seen status event for a service, or {@code null} if none yet. */
    public StatusEvent getLatestStatus(String serviceName) {
        return latestByService.get(serviceName);
    }
}
