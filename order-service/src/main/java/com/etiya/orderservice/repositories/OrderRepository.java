package com.etiya.orderservice.repositories;

import com.etiya.orderservice.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer. Spring Data JPA repository backed by the MySQL {@code orders} table.
 */
public interface OrderRepository extends JpaRepository<Order, Integer> {
}
