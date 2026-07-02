package com.etiya.notificationservice.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.etiya.notificationservice.entities.Notification;
import com.etiya.notificationservice.repositories.NotificationRepository;
import com.etiya.notificationservice.services.dtos.responses.GetNotificationResponse;

/**
 * Read-only view over the notification log. notification-service has no write endpoints — it is
 * driven entirely by events; these endpoints just expose what it has produced.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {

    private final NotificationRepository notificationRepository;

    public NotificationsController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public List<GetNotificationResponse> getAll() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{customerId}")
    public List<GetNotificationResponse> getByCustomer(@PathVariable int customerId) {
        return notificationRepository.findByCustomerIdOrderByIdDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private GetNotificationResponse toResponse(Notification notification) {
        return new GetNotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getCustomerId(),
                notification.getMessage(),
                notification.getCreatedAt());
    }
}
