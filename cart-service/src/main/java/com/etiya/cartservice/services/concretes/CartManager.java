package com.etiya.cartservice.services.concretes;

import com.etiya.cartservice.customers.CustomerReplicaRepository;
import com.etiya.cartservice.entities.Cart;
import com.etiya.cartservice.entities.CartItem;
import com.etiya.cartservice.entities.CartStatus;
import com.etiya.cartservice.events.CartCheckedOutEvent;
import com.etiya.cartservice.events.CartItemPayload;
import com.etiya.cartservice.outbox.OutboxService;
import com.etiya.cartservice.repositories.CartRepository;
import com.etiya.cartservice.services.abstracts.CartService;
import com.etiya.cartservice.services.dtos.requests.AddCartItemRequest;
import com.etiya.cartservice.services.dtos.responses.CartItemResponse;
import com.etiya.cartservice.services.dtos.responses.CartResponse;
import com.etiya.cartservice.services.dtos.responses.CheckedOutCartResponse;
import com.etiya.cartservice.services.exceptions.BusinessException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business layer implementation. Manages a customer's active cart and, on checkout, records a
 * {@link CartCheckedOutEvent} in the outbox (same transaction as the DB write) so it is published
 * to other services without any synchronous call.
 *
 * <p>The active cart is cached in Redis keyed by {@code customerId} (cache {@code activeCarts});
 * see {@link com.etiya.cartservice.config.CacheConfig}.</p>
 */
@Service
public class CartManager implements CartService {

    /** Output binding CartCheckedOut is relayed to (topic mapped in application.yml). */
    private static final String CART_CHECKED_OUT_BINDING = "cartCheckedOut-out-0";

    private final CartRepository cartRepository;
    private final CustomerReplicaRepository customerReplicaRepository;
    private final OutboxService outboxService;

    public CartManager(CartRepository cartRepository,
                       CustomerReplicaRepository customerReplicaRepository,
                       OutboxService outboxService) {
        this.cartRepository = cartRepository;
        this.customerReplicaRepository = customerReplicaRepository;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "activeCarts", key = "#request.customerId")
    public CartResponse addItem(AddCartItemRequest request) {
        // Async cross-service check: the customer is validated against the locally replicated view
        // kept up to date from "customer-events" — no synchronous call to customer-service.
        ensureCustomerExists(request.getCustomerId());

        Cart cart = cartRepository.findByCustomerIdAndStatus(request.getCustomerId(), CartStatus.ACTIVE)
                .orElseGet(() -> new Cart(request.getCustomerId()));
        cart.addItem(request.getProductId(), request.getQuantity(), request.getUnitPrice());

        return toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Cacheable(cacheNames = "activeCarts", key = "#customerId")
    public CartResponse getActiveCart(int customerId) {
        return toCartResponse(findActiveCartOrThrow(customerId));
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "activeCarts", key = "#customerId")
    public CartResponse removeItem(int customerId, int productId) {
        Cart cart = findActiveCartOrThrow(customerId);
        cart.removeItem(productId);
        return toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "activeCarts", key = "#customerId")
    public CartResponse clear(int customerId) {
        Cart cart = findActiveCartOrThrow(customerId);
        cart.clear();
        return toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "activeCarts", key = "#customerId")
    public CheckedOutCartResponse checkout(int customerId) {
        Cart cart = findActiveCartOrThrow(customerId);
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cannot checkout an empty cart for customer: " + customerId);
        }

        cart.checkout();
        Cart saved = cartRepository.save(cart);

        List<CartItemPayload> items = saved.getItems().stream()
                .map(item -> new CartItemPayload(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList();

        outboxService.record(
                "Cart",
                String.valueOf(saved.getId()),
                "CartCheckedOut",
                CART_CHECKED_OUT_BINDING,
                new CartCheckedOutEvent(
                        UUID.randomUUID().toString(),
                        saved.getId(),
                        saved.getCustomerId(),
                        saved.getTotalPrice(),
                        items));

        return new CheckedOutCartResponse(
                saved.getId(),
                saved.getCustomerId(),
                saved.getStatus().name(),
                saved.getTotalPrice());
    }

    private void ensureCustomerExists(int customerId) {
        if (!customerReplicaRepository.existsById(customerId)) {
            throw new BusinessException(
                    "Customer not found with id: " + customerId
                            + " (unknown or not yet replicated from customer-service)");
        }
    }

    private Cart findActiveCartOrThrow(int customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("No active cart for customer: " + customerId));
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                cart.getStatus().name(),
                items,
                cart.getTotalPrice());
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
