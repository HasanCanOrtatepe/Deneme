package com.etiya.productservice.repositories;

import com.etiya.productservice.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer. Spring Data JPA repository backed by the PostgreSQL {@code products} table.
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {
}
