package com.etiya.cartservice.customers;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA access to the local customer replica table (MySQL).
 */
public interface CustomerReplicaRepository extends JpaRepository<CustomerReplica, Integer> {
}
