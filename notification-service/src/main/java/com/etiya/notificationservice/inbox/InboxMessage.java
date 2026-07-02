package com.etiya.notificationservice.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Idempotency checkpoint for the Inbox pattern (H2).
 *
 * <p>Before turning an incoming event into a notification, the consumer checks whether its
 * {@code messageId} already exists here. Because the broker delivers <em>at-least-once</em>, this
 * guards against sending the same notification twice. The row is written in the same transaction
 * that persists the notification, so the effect and the checkpoint commit together.</p>
 */
@Entity
@Table(name = "inbox_messages")
public class InboxMessage {

    /**
     * Deduplication key for the source event. For events that carry a globally unique id this is
     * that id; for events that do not (e.g. OrderCreated) it is a stable natural key such as
     * {@code "OrderCreated:" + orderId}.
     */
    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private String messageId;

    /** Logical event type handled, e.g. {@code "OrderCreated"}; kept for diagnostics. */
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
