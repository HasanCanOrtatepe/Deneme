package com.etiya.cartservice.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shopping cart aggregate persisted in H2 (table {@code carts}). A customer has at most one
 * {@link CartStatus#ACTIVE} cart at a time; checking it out flips it to
 * {@link CartStatus#CHECKED_OUT} and a fresh active cart is created on the next add.
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** Owning customer; validated against the local customer replica before a cart is created. */
    @Column(nullable = false)
    private int customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CartStatus status;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected Cart() {
    }

    public Cart(int customerId) {
        this.customerId = customerId;
        this.status = CartStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    /**
     * Adds {@code quantity} of the product to the cart, merging into an existing line for the same
     * product (and refreshing its unit price) instead of creating a duplicate line.
     */
    public void addItem(int productId, int quantity, BigDecimal unitPrice) {
        Optional<CartItem> existing = findItem(productId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.addQuantity(quantity);
            item.setUnitPrice(unitPrice);
        } else {
            items.add(new CartItem(this, productId, quantity, unitPrice));
        }
    }

    /** Removes the line for the given product, if present. */
    public void removeItem(int productId) {
        items.removeIf(item -> item.getProductId() == productId);
    }

    public void clear() {
        items.clear();
    }

    public void checkout() {
        this.status = CartStatus.CHECKED_OUT;
    }

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> findItem(int productId) {
        return items.stream()
                .filter(item -> item.getProductId() == productId)
                .findFirst();
    }

    public int getId() {
        return id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public CartStatus getStatus() {
        return status;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
