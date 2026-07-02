package com.etiya.customerservice.events;

/**
 * Envelope published to Kafka topic "customer-events" for every customer lifecycle change.
 *
 * <p>A single event type is used (instead of one topic per change) so downstream services keep a
 * single ordered stream and a single idempotency (Inbox) checkpoint per {@link #eventId}. The
 * {@link #eventType} tells consumers whether to upsert or remove their local view. For a
 * {@link CustomerEventType#DELETED} event only {@link #id} is guaranteed to be populated.</p>
 *
 * <p>{@link #eventId} is a globally unique id generated once when the event is recorded in the
 * outbox; consumers use it as the deduplication key so re-delivered messages are applied once.</p>
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
