package com.etiya.cartservice.events;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event published to Kafka topic "cart-checked-out" when a customer checks out their cart.
 * Carries the full snapshot so downstream services (e.g. notification-service) can react without
 * calling back into cart-service.
 *
 * <p>{@link #eventId} is a globally unique id generated once when the event is recorded in the
 * outbox; consumers use it as the idempotency (Inbox) key.</p>
 */
public record CartCheckedOutEvent(
        String eventId,
        int cartId,
        int customerId,
        BigDecimal totalPrice,
        List<CartItemPayload> items) {
}
