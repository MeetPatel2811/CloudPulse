package com.cloudpulse.controller;

import com.cloudpulse.model.WebhookTarget;
import com.cloudpulse.repository.NotificationDeliveryRepository;
import com.cloudpulse.repository.WebhookTargetRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

// Manages webhook targets (register / list / delete) and exposes the notification
// delivery history. Notifications themselves are sent by WebhookNotifier when a
// service changes state; this controller is the configuration + audit surface.
@RestController
public class WebhookController {

    private final WebhookTargetRepository targetRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    public WebhookController(WebhookTargetRepository targetRepository,
                             NotificationDeliveryRepository deliveryRepository) {
        this.targetRepository = targetRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @GetMapping("/api/webhooks")
    public List<WebhookTargetResponse> listTargets() {
        return targetRepository.findAll().stream().map(WebhookTargetResponse::from).toList();
    }

    @PostMapping("/api/webhooks")
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookTargetResponse register(@Valid @RequestBody RegisterWebhookRequest request) {
        WebhookTarget target = targetRepository.save(
                new WebhookTarget(request.provider(), request.url(), request.label()));
        return WebhookTargetResponse.from(target);
    }

    @DeleteMapping("/api/webhooks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        if (!targetRepository.existsById(id)) {
            throw new NoSuchElementException("Webhook target not found: " + id);
        }
        targetRepository.deleteById(id);
    }

    @GetMapping("/api/notifications")
    public List<NotificationDeliveryResponse> deliveryHistory() {
        return deliveryRepository.findByOrderBySentAtDesc().stream()
                .map(NotificationDeliveryResponse::from)
                .toList();
    }
}
