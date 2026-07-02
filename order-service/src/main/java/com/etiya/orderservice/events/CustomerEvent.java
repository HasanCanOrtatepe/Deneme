package com.etiya.orderservice.events;

/**
 * Envelope consumed from Kafka topic "customer-events". Mirrors the customer-service producer
 * payload; deserialized by field name from the JSON message.
 *
 * <p>{@link #eventId} is the idempotency key used by the Inbox pattern so a re-delivered message is
 * applied to the local customer replica only once. For a {@link CustomerEventType#DELETED} event
 * only {@link #id} is guaranteed to be populated.</p>
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
