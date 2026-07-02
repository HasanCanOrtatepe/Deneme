package com.etiya.cartservice.services.abstracts;

import com.etiya.cartservice.services.dtos.requests.AddCartItemRequest;
import com.etiya.cartservice.services.dtos.responses.CartResponse;
import com.etiya.cartservice.services.dtos.responses.CheckedOutCartResponse;

/**
 * Business layer contract. Controllers depend on this abstraction, never on the concrete manager.
 */
public interface CartService {

    CartResponse addItem(AddCartItemRequest request);

    CartResponse getActiveCart(int customerId);

    CartResponse removeItem(int customerId, int productId);

    CartResponse clear(int customerId);

    CheckedOutCartResponse checkout(int customerId);
}
