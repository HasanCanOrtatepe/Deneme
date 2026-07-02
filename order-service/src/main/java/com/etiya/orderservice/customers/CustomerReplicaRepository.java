package com.etiya.orderservice.customers;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA access to the local customer replica table (H2).
 */
public interface CustomerReplicaRepository extends JpaRepository<CustomerReplica, Integer> {
}
