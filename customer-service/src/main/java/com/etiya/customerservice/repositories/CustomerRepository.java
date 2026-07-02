package com.etiya.customerservice.repositories;

import com.etiya.customerservice.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA access to the {@code customers} table (H2).
 */
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    boolean existsByEmail(String email);
}
