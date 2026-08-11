package com.cloudpulse.notification;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.NotificationDelivery;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.model.WebhookProvider;
import com.cloudpulse.model.WebhookTarget;
import com.cloudpulse.repository.NotificationDeliveryRepository;
import com.cloudpulse.repository.WebhookTargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class WebhookNotifierTest {

    private WebhookTargetRepository targetRepository;
    private NotificationDeliveryRepository deliveryRepository;
    private WebhookSender sender;
    private WebhookNotifier notifier;

    @BeforeEach
    void setUp() {
        targetRepository = mock(WebhookTargetRepository.class);
        deliveryRepository = mock(NotificationDeliveryRepository.class);
        sender = mock(WebhookSender.class);
        notifier = new WebhookNotifier(
                List.of(new SlackPayloadAdapter(), new TeamsPayloadAdapter(), new DiscordPayloadAdapter()),
                targetRepository, deliveryRepository, sender);
    }

    private StatusEvent transitionTo(ServiceStatus newStatus) {
        MonitoredService service =
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK);
        return new StatusEvent(service, ServiceStatus.HEALTHY, newStatus, "changed");
    }

    @Test
    void sendsToEnabledTargetWithProviderFormatAndRecordsDelivery() {
        WebhookTarget target = new WebhookTarget(WebhookProvider.SLACK, "https://hooks.slack.test/abc", "team");
        when(targetRepository.findByEnabledTrue()).thenReturn(List.of(target));
        when(sender.send(anyString(), anyString())).thenReturn(new DeliveryResult(true, 200, null));

        notifier.onEvent(transitionTo(ServiceStatus.DOWN));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("https://hooks.slack.test/abc"), body.capture());
        assertTrue(body.getValue().contains("\"text\""), "should use the Slack payload format");

        ArgumentCaptor<NotificationDelivery> saved = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryRepository).save(saved.capture());
        assertTrue(saved.getValue().isSuccess());
        assertEquals(WebhookProvider.SLACK, saved.getValue().getProvider());
    }

    @Test
    void recordsFailedDelivery() {
        WebhookTarget target = new WebhookTarget(WebhookProvider.DISCORD, "https://discord.test/x", null);
        when(targetRepository.findByEnabledTrue()).thenReturn(List.of(target));
        when(sender.send(anyString(), anyString())).thenReturn(new DeliveryResult(false, 500, "HTTP 500"));

        notifier.onEvent(transitionTo(ServiceStatus.DEGRADED));

        ArgumentCaptor<NotificationDelivery> saved = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryRepository).save(saved.capture());
        assertFalse(saved.getValue().isSuccess());
        assertEquals(500, saved.getValue().getHttpStatus());
    }

    @Test
    void doesNothingForNonNotifiableStatus() {
        notifier.onEvent(transitionTo(ServiceStatus.HEALTHY));

        verify(targetRepository, never()).findByEnabledTrue();
        verifyNoInteractions(sender, deliveryRepository);
    }
}
