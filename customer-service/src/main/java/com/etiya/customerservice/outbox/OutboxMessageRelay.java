package com.etiya.customerservice.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Polling publisher for the Transactional Outbox.
 *
 * <p>On a fixed schedule it reads {@link OutboxStatus#PENDING} rows oldest-first and relays each
 * one's JSON {@code payload} to its {@code destination} binding via {@link StreamBridge}. The raw
 * JSON is sent with a {@code application/json} content type so downstream consumers deserialize it
 * exactly as before. Successfully sent rows flip to {@link OutboxStatus#PUBLISHED}; the mutation is
 * flushed on transaction commit (the entities are managed within {@link #publishPending()}).</p>
 *
 * <p>Delivery is <em>at-least-once</em>: if the JVM dies after the broker accepts a message but
 * before the row is committed as published, the next poll re-sends it. Consumers must therefore be
 * idempotent (see the Inbox pattern in order-service).</p>
 *
 * <p>A relay attempt that throws (or is rejected by the binding) increments {@code retryCount} and
 * leaves the row {@link OutboxStatus#PENDING} for the next poll. Once {@code retryCount} reaches the
 * configured {@code outbox.poller.max-retry}, the row is moved to the terminal
 * {@link OutboxStatus#FAILED} state and the last error is stored in its {@code EXCEPTION_MESSAGE}
 * column so it is no longer polled.</p>
 */
@Component
public class OutboxMessageRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxMessageRelay.class);

    private final OutboxRepository outboxRepository;
    private final StreamBridge streamBridge;
    private final int batchSize;
    private final int maxRetry;

    public OutboxMessageRelay(OutboxRepository outboxRepository,
                              StreamBridge streamBridge,
                              @Value("${outbox.poller.batch-size:100}") int batchSize,
                              @Value("${outbox.poller.max-retry:5}") int maxRetry) {
        this.outboxRepository = outboxRepository;
        this.streamBridge = streamBridge;
        this.batchSize = batchSize;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "${outbox.poller.fixed-delay:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByIdAsc(
                OutboxStatus.PENDING, PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return;
        }
        log.debug("Outbox poll: relaying {} pending event(s)", pending.size());
        for (OutboxEvent event : pending) {
            relay(event);
        }
    }

    private void relay(OutboxEvent event) {
        try {
            Message<byte[]> message = MessageBuilder
                    .withPayload(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                    .build();

            boolean sent = streamBridge.send(event.getDestination(), message);
            if (sent) {
                event.markPublished(Instant.now());
                log.info("Outbox event #{} ({}) published to '{}'",
                        event.getId(), event.getEventType(), event.getDestination());
            } else {
                handleFailure(event,
                        "StreamBridge rejected the message (send returned false)", null);
            }
        } catch (Exception e) {
            handleFailure(event, e.getMessage(), e);
        }
    }

    /**
     * Records a failed relay attempt. Keeps the event {@link OutboxStatus#PENDING} for a later poll
     * until {@code retryCount} reaches {@link #maxRetry}, after which it becomes terminal
     * {@link OutboxStatus#FAILED} with {@code reason} stored in {@code EXCEPTION_MESSAGE}.
     *
     * @param reason human-readable error; {@code cause} is the exception when available (may be null)
     */
    private void handleFailure(OutboxEvent event, String reason, Exception cause) {
        event.incrementRetryCount();
        if (event.getRetryCount() >= maxRetry) {
            event.markFailed(reason);
            log.error("Outbox event #{} exhausted max retries ({}), marked FAILED: {}",
                    event.getId(), maxRetry, reason, cause);
        } else {
            log.warn("Outbox event #{} relay failed (attempt {}/{}), will retry: {}",
                    event.getId(), event.getRetryCount(), maxRetry, reason, cause);
        }
    }
}
