package com.cloudpulse.observer;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusEventPublisherTest {

    private final StatusEventPublisher publisher = new StatusEventPublisher();
    private final DashboardUpdateObserver dashboardUpdateObserver = new DashboardUpdateObserver();
    private final AlertObserver alertObserver = new AlertObserver();
    private final EventHistoryObserver eventHistoryObserver = new EventHistoryObserver();

    @Test
    void publish_notifiesAllRegisteredSubscribers() {
        publisher.register(dashboardUpdateObserver);
        publisher.register(alertObserver);
        publisher.register(eventHistoryObserver);

        MonitoredService service = new MonitoredService("Payment Service", "http://localhost:8082/health", MonitorType.HTTP);
        StatusEvent event = new StatusEvent(service, ServiceStatus.HEALTHY, ServiceStatus.DOWN, "unreachable");

        publisher.publish(event);

        assertEquals(event, dashboardUpdateObserver.getLatestStatus(service.getName()));
        assertTrue(alertObserver.getTriggeringEvents().contains(event));
        assertTrue(eventHistoryObserver.getHistory().contains(event));
    }
}
