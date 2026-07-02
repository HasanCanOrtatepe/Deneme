package com.etiya.cartservice.events;

/**
 * Lifecycle event kinds carried by a {@link CustomerEvent} envelope (mirrors customer-service).
 */
public enum CustomerEventType {
    CREATED,
    UPDATED,
    DELETED
}
