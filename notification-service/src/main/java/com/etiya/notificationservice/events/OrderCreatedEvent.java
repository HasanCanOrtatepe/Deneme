package com.etiya.notificationservice.events;

import java.math.BigDecimal;

/**
 * Event consumed from Kafka topic "order-created" (published by order-service).
 * Mirrors the producer payload; deserialized by field name from the JSON message.
 */
public record OrderCreatedEvent(
        int orderId,
        int customerId,
        int productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String address) {
}
