package com.etiya.orderservice.messaging;

import com.etiya.orderservice.customers.CustomerReplica;
import com.etiya.orderservice.customers.CustomerReplicaRepository;
import com.etiya.orderservice.events.CustomerEvent;
import com.etiya.orderservice.inbox.InboxMessage;
import com.etiya.orderservice.inbox.InboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Applies incoming {@link CustomerEvent}s to the local customer replica, idempotently (Inbox
 * pattern).
 *
 * <p>The whole method runs in one transaction: the inbox checkpoint, the replica change all commit
 * together. If the same event is delivered again (the outbox relay is at-least-once), the
 * {@link InboxRepository} lookup short-circuits it, so each event is applied exactly once.</p>
 */
@Service
public class CustomerEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventHandler.class);

    private final InboxRepository inboxRepository;
    private final CustomerReplicaRepository customerReplicaRepository;

    public CustomerEventHandler(InboxRepository inboxRepository,
                                CustomerReplicaRepository customerReplicaRepository) {
        this.inboxRepository = inboxRepository;
        this.customerReplicaRepository = customerReplicaRepository;
    }

    @Transactional
    public void handle(CustomerEvent event) {
        if (event == null || event.eventId() == null) {
            log.warn("Discarding customer event with no eventId: {}", event);
            return;
        }

        // Idempotency guard: skip anything we have already applied.
        if (inboxRepository.existsById(event.eventId())) {
            log.debug("Duplicate customer event {} ({}) ignored", event.eventId(), event.eventType());
            return;
        }

        switch (event.eventType()) {
            case CREATED, UPDATED -> upsertReplica(event);
            case DELETED -> customerReplicaRepository.deleteById(event.id());
        }

        inboxRepository.save(new InboxMessage(
                event.eventId(), event.eventType().name(), Instant.now()));

        log.info("Applied customer event {} ({}) for customerId={}",
                event.eventId(), event.eventType(), event.id());
    }

    private void upsertReplica(CustomerEvent event) {
        CustomerReplica replica = customerReplicaRepository.findById(event.id())
                .orElseGet(() -> new CustomerReplica(
                        event.id(), event.firstName(), event.lastName(), event.email()));
        replica.setFirstName(event.firstName());
        replica.setLastName(event.lastName());
        replica.setEmail(event.email());
        customerReplicaRepository.save(replica);
    }
}
