package com.cloudpulse.notification;

import com.cloudpulse.model.NotificationDelivery;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.Severity;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.model.WebhookProvider;
import com.cloudpulse.model.WebhookTarget;
import com.cloudpulse.observer.EventSubscriber;
import com.cloudpulse.repository.NotificationDeliveryRepository;
import com.cloudpulse.repository.WebhookTargetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

// Observer subscriber: when a service transitions to an alert-worthy state, post a
// notification to every enabled webhook target (Slack/Teams/Discord) using that
// provider's Adapter, and record each attempt as a NotificationDelivery.
@Component
public class WebhookNotifier implements EventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);

    private final List<WebhookPayloadAdapter> adapters;
    private final WebhookTargetRepository targetRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final WebhookSender sender;

    public WebhookNotifier(List<WebhookPayloadAdapter> adapters,
                           WebhookTargetRepository targetRepository,
                           NotificationDeliveryRepository deliveryRepository,
                           WebhookSender sender) {
        this.adapters = adapters;
        this.targetRepository = targetRepository;
        this.deliveryRepository = deliveryRepository;
        this.sender = sender;
    }

    @Override
    public void onEvent(StatusEvent event) {
        ServiceStatus status = event.getNewStatus();
        if (!isNotifiable(status)) {
            return;
        }

        List<WebhookTarget> targets = targetRepository.findByEnabledTrue();
        if (targets.isEmpty()) {
            return;
        }

        NotificationMessage message = buildMessage(event, status);
        for (WebhookTarget target : targets) {
            WebhookPayloadAdapter adapter = adapterFor(target.getProvider());
            if (adapter == null) {
                log.warn("No payload adapter for provider {}, skipping target {}", target.getProvider(), target.getId());
                continue;
            }
            String body = adapter.buildBody(message);
            DeliveryResult result = sender.send(target.getUrl(), body);
            deliveryRepository.save(new NotificationDelivery(
                    target.getProvider(), target.getUrl(), null,
                    message.serviceName(), message.text(),
                    result.success(), result.httpStatus(), result.error()));
            if (!result.success()) {
                log.warn("Webhook to {} ({}) failed: {}", target.getProvider(), target.getUrl(), result.error());
            }
        }
    }

    // Notify on problems (DOWN / DEGRADED) and on recovery, but not on the initial
    // UNKNOWN -> HEALTHY startup transition.
    private boolean isNotifiable(ServiceStatus status) {
        return status == ServiceStatus.DOWN
                || status == ServiceStatus.DEGRADED
                || status == ServiceStatus.RECOVERED;
    }

    private NotificationMessage buildMessage(StatusEvent event, ServiceStatus status) {
        String serviceName = event.getService().getName();
        Severity severity = switch (status) {
            case DOWN -> Severity.CRITICAL;
            case DEGRADED -> Severity.WARNING;
            default -> Severity.INFO;
        };
        String text = "[" + severity + "] " + serviceName + " is " + status
                + " (was " + event.getPreviousStatus() + ")";
        return new NotificationMessage(serviceName, status, severity, text);
    }

    private WebhookPayloadAdapter adapterFor(WebhookProvider provider) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(provider))
                .findFirst()
                .orElse(null);
    }
}
