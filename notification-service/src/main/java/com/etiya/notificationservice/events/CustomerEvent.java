package com.etiya.notificationservice.events;

/**
 * Envelope consumed from Kafka topic "customer-events". Mirrors the customer-service producer
 * payload; deserialized by field name from the JSON message.
 *
 * <p>{@link #eventId} is the idempotency key used by the Inbox pattern so a re-delivered message
 * triggers a notification only once.</p>
 */
public record CustomerEvent(
        String eventId,
        CustomerEventType eventType,
        int id,
        String firstName,
        String lastName,
        String email,
        String phone) {
}
