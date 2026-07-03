package com.etiya.cartservice.repositories;

import com.etiya.cartservice.entities.Cart;
import com.etiya.cartservice.entities.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA access to the {@code carts} table (MySQL).
 */
public interface CartRepository extends JpaRepository<Cart, Integer> {

    Optional<Cart> findByCustomerIdAndStatus(int customerId, CartStatus status);
}
