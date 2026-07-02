package com.etiya.cartservice.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA access to the inbox table, keyed by the source event id.
 */
public interface InboxRepository extends JpaRepository<InboxMessage, String> {
}
