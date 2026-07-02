package com.etiya.cartservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.etiya.cartservice.services.abstracts.CartService;
import com.etiya.cartservice.services.dtos.requests.AddCartItemRequest;
import com.etiya.cartservice.services.dtos.responses.CartResponse;
import com.etiya.cartservice.services.dtos.responses.CheckedOutCartResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carts")
public class CartsController {

    private final CartService cartService;

    public CartsController(CartService cartService) {
        this.cartService = cartService;
    }

    /** Adds an item to the customer's active cart (creating the cart on first add). */
    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(request);
    }

    @GetMapping("/{customerId}")
    public CartResponse getActiveCart(@PathVariable int customerId) {
        return cartService.getActiveCart(customerId);
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    public CartResponse removeItem(@PathVariable int customerId, @PathVariable int productId) {
        return cartService.removeItem(customerId, productId);
    }

    @DeleteMapping("/{customerId}")
    public CartResponse clear(@PathVariable int customerId) {
        return cartService.clear(customerId);
    }

    @PostMapping("/{customerId}/checkout")
    @ResponseStatus(HttpStatus.OK)
    public CheckedOutCartResponse checkout(@PathVariable int customerId) {
        return cartService.checkout(customerId);
    }
}
