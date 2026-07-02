package com.etiya.customerservice.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A message queued in the Transactional Outbox table (H2).
 *
 * <p>The business layer records a row here in the same transaction that writes the customer,
 * instead of publishing to Kafka directly. A {@link OutboxMessageRelay polling relay} later reads
 * {@link OutboxStatus#PENDING} rows and forwards the {@link #payload} (already serialized JSON) to
 * the {@link #destination} binding, flipping the row to {@link OutboxStatus#PUBLISHED} once the
 * broker accepts it.</p>
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Domain aggregate the event belongs to, e.g. {@code "Customer"}. */
    @Column(nullable = false)
    private String aggregateType;

    /** Identifier of the aggregate instance, e.g. the customer id. */
    @Column(nullable = false)
    private String aggregateId;

    /** Logical event name, e.g. {@code "CustomerCreated"}. */
    @Column(nullable = false)
    private String eventType;

    /** Spring Cloud Stream output binding the relay sends this message to. */
    @Column(nullable = false)
    private String destination;

    /** Serialized (JSON) event body, relayed to the broker as-is. */
    @Column(nullable = false, length = 4000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    /** Number of failed relay attempts; useful for diagnostics / backoff. */
    @Column(nullable = false)
    private int retryCount;

    /** Last error seen when the event was moved to {@link OutboxStatus#FAILED}; null otherwise. */
    @Column(name = "EXCEPTION_MESSAGE", length = 2000)
    private String exceptionMessage;

    /** Required by JPA. */
    protected OutboxEvent() {
    }

    public OutboxEvent(String aggregateType, String aggregateId, String eventType,
                       String destination, String payload, Instant createdAt) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.destination = destination;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = createdAt;
        this.retryCount = 0;
    }

    public void markPublished(Instant when) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = when;
    }

    /**
     * Marks the event as permanently failed after retries are exhausted, keeping the last error.
     * The message is truncated to the {@code EXCEPTION_MESSAGE} column width.
     */
    public void markFailed(String exceptionMessage) {
        this.status = OutboxStatus.FAILED;
        this.exceptionMessage = truncate(exceptionMessage, 2000);
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public Long getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDestination() {
        return destination;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
