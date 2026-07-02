package com.etiya.notificationservice.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA access to the inbox table, keyed by the source event's deduplication key.
 */
public interface InboxRepository extends JpaRepository<InboxMessage, String> {
}
