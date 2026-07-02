package com.etiya.orderservice.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Idempotency checkpoint for the Inbox pattern (H2).
 *
 * <p>Before applying an incoming event, the consumer checks whether its {@code eventId} already
 * exists here. Because the broker delivers <em>at-least-once</em>, this guards against re-processing
 * the same message twice. The row is written in the same transaction that applies the event, so the
 * effect and the checkpoint commit together.</p>
 */
@Entity
@Table(name = "inbox_messages")
public class InboxMessage {

    /** Globally unique id of the source event (its outbox {@code eventId}); the deduplication key. */
    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private String messageId;

    /** Logical event type applied, e.g. {@code "CREATED"}; kept for diagnostics. */
    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant receivedAt;

    /** Required by JPA. */
    protected InboxMessage() {
    }

    public InboxMessage(String messageId, String eventType, Instant receivedAt) {
        this.messageId = messageId;
        this.eventType = eventType;
        this.receivedAt = receivedAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
