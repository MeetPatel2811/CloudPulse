package com.cloudpulse.observer;

import com.cloudpulse.notification.WebhookNotifier;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Wires the dashboard/alert/history observers into the shared {@link StatusEventPublisher}
 * on startup. {@link StatusEventPublisher} keeps its subscriber list private and only
 * grown through the manual {@link StatusEventPublisher#register(EventSubscriber)} method
 * (it does not auto-collect Spring beans), so something has to call that for each
 * observer bean — this class is that something.
 */
@Component
public class ObserverRegistrar {

    private final StatusEventPublisher publisher;
    private final DashboardUpdateObserver dashboardUpdateObserver;
    private final AlertObserver alertObserver;
    private final EventHistoryObserver eventHistoryObserver;
    private final WebhookNotifier webhookNotifier;

    public ObserverRegistrar(StatusEventPublisher publisher,
                             DashboardUpdateObserver dashboardUpdateObserver,
                             AlertObserver alertObserver,
                             EventHistoryObserver eventHistoryObserver,
                             WebhookNotifier webhookNotifier) {
        this.publisher = publisher;
        this.dashboardUpdateObserver = dashboardUpdateObserver;
        this.alertObserver = alertObserver;
        this.eventHistoryObserver = eventHistoryObserver;
        this.webhookNotifier = webhookNotifier;
    }

    @PostConstruct
    void registerObservers() {
        publisher.register(dashboardUpdateObserver);
        publisher.register(alertObserver);
        publisher.register(eventHistoryObserver);
        publisher.register(webhookNotifier);
    }
}
