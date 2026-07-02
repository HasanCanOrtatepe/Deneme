package com.etiya.cartservice.entities;

/**
 * Lifecycle state of a {@link Cart}.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — the customer's current cart, still being filled.</li>
 *   <li>{@link #CHECKED_OUT} — finalized; a {@code CartCheckedOut} event has been queued.</li>
 * </ul>
 */
public enum CartStatus {
    ACTIVE,
    CHECKED_OUT
}
