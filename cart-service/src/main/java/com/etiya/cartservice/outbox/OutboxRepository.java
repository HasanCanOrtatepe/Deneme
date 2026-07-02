package com.etiya.cartservice.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA access to the outbox table.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Oldest-first batch of events in the given status, used by the polling relay
     * to publish {@link OutboxStatus#PENDING} messages in insertion order.
     */
    List<OutboxEvent> findByStatusOrderByIdAsc(OutboxStatus status, Pageable pageable);
}
