package com.etiya.notificationservice.events;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event consumed from Kafka topic "cart-checked-out" (published by cart-service).
 * Mirrors the producer payload; deserialized by field name from the JSON message.
 *
 * <p>{@link #eventId} is the idempotency key used by the Inbox pattern.</p>
 */
public record CartCheckedOutEvent(
        String eventId,
        int cartId,
        int customerId,
        BigDecimal totalPrice,
        List<CartItemPayload> items) {
}
