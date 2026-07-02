package com.etiya.cartservice.events;

import java.math.BigDecimal;

/**
 * A single line carried inside a {@link CartCheckedOutEvent}.
 */
public record CartItemPayload(
        int productId,
        int quantity,
        BigDecimal unitPrice) {
}
