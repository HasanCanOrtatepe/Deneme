package com.etiya.orderservice.outbox;

/**
 * Lifecycle state of an {@link OutboxEvent}.
 *
 * <ul>
 *   <li>{@link #PENDING} — written by the business operation, not yet relayed to Kafka.</li>
 *   <li>{@link #PUBLISHED} — successfully sent to the broker by the polling relay.</li>
 *   <li>{@link #FAILED} — could not be relayed after exhausting the configured max retries;
 *       terminal, no longer polled. The last error is kept in {@code EXCEPTION_MESSAGE}.</li>
 * </ul>
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
