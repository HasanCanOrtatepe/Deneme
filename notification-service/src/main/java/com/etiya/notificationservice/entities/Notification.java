package com.etiya.notificationservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Audit log of a notification the service "sent" (here: logged), persisted in PostgreSQL.
 * One row is written per successfully handled inbound event.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Logical notification type, e.g. {@code "ORDER_PLACED"}. */
    @Column(nullable = false)
    private String type;

    /** Customer the notification is addressed to. */
    @Column(nullable = false)
    private int customerId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected Notification() {
    }

    public Notification(String type, int customerId, String message, Instant createdAt) {
        this.type = type;
        this.customerId = customerId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
