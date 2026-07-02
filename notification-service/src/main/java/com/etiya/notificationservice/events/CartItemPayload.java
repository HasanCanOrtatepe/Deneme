package com.etiya.notificationservice.events;

import java.math.BigDecimal;

/**
 * A single line carried inside a {@link CartCheckedOutEvent} (mirrors cart-service).
 */
public record CartItemPayload(
        int productId,
        int quantity,
        BigDecimal unitPrice) {
}
