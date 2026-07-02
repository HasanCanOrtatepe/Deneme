package com.etiya.orderservice.services.concretes;

import com.etiya.orderservice.customers.CustomerReplicaRepository;
import com.etiya.orderservice.entities.Order;
import com.etiya.orderservice.events.OrderCreatedEvent;
import com.etiya.orderservice.outbox.OutboxService;
import com.etiya.orderservice.repositories.OrderRepository;
import com.etiya.orderservice.services.abstracts.OrderService;
import com.etiya.orderservice.services.dtos.requests.CreateOrderRequest;
import com.etiya.orderservice.services.dtos.requests.UpdateOrderRequest;
import com.etiya.orderservice.services.dtos.responses.CreatedOrderResponse;
import com.etiya.orderservice.services.dtos.responses.DeletedOrderResponse;
import com.etiya.orderservice.services.dtos.responses.GetAllOrdersResponse;
import com.etiya.orderservice.services.dtos.responses.GetByIdOrderResponse;
import com.etiya.orderservice.services.dtos.responses.UpdatedOrderResponse;
import com.etiya.orderservice.services.exceptions.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business layer implementation. Maps between request/response DTOs and the entity,
 * and applies business rules before delegating to the data access layer.
 */
@Service
public class OrderManager implements OrderService {

    /** Output binding the OrderCreated message is relayed to (topic mapped in application.yml). */
    private static final String ORDER_CREATED_BINDING = "orderCreated-out-0";

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final CustomerReplicaRepository customerReplicaRepository;

    public OrderManager(OrderRepository orderRepository,
                        OutboxService outboxService,
                        CustomerReplicaRepository customerReplicaRepository) {
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
        this.customerReplicaRepository = customerReplicaRepository;
    }

    @Override
    public CreatedOrderResponse add(CreateOrderRequest request) {
        // Async cross-service check: the customer is validated against the locally replicated view
        // kept up to date from "customer-events" — no synchronous call to customer-service.
        if (!customerReplicaRepository.existsById(request.getCustomerId())) {
            throw new BusinessException(
                    "Customer not found with id: " + request.getCustomerId()
                            + " (unknown or not yet replicated from customer-service)");
        }

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(request.getUnitPrice());
        order.setTotalPrice(calculateTotalPrice(request.getUnitPrice(), request.getQuantity()));
        order.setAddress(request.getAddress());

        Order saved = orderRepository.save(order);

        // Transactional Outbox: queue OrderCreated in the outbox table instead of publishing to
        // Kafka inline. The polling relay (OutboxMessageRelay) forwards it to product-service.
        outboxService.record(
                "Order",
                String.valueOf(saved.getId()),
                "OrderCreated",
                ORDER_CREATED_BINDING,
                new OrderCreatedEvent(
                        saved.getId(),
                        saved.getCustomerId(),
                        saved.getProductId(),
                        saved.getQuantity(),
                        saved.getUnitPrice(),
                        saved.getTotalPrice(),
                        saved.getAddress()));

        return new CreatedOrderResponse(
                saved.getId(),
                saved.getCustomerId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getUnitPrice(),
                saved.getTotalPrice(),
                saved.getAddress());
    }

    @Override
    public UpdatedOrderResponse update(UpdateOrderRequest request) {
        Order order = findOrderOrThrow(request.getId());
        order.setCustomerId(request.getCustomerId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(request.getUnitPrice());
        order.setTotalPrice(calculateTotalPrice(request.getUnitPrice(), request.getQuantity()));
        order.setAddress(request.getAddress());

        Order saved = orderRepository.save(order);

        return new UpdatedOrderResponse(
                saved.getId(),
                saved.getCustomerId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getUnitPrice(),
                saved.getTotalPrice(),
                saved.getAddress());
    }

    @Override
    public DeletedOrderResponse delete(int id) {
        Order order = findOrderOrThrow(id);
        orderRepository.deleteById(id);
        return new DeletedOrderResponse(order.getId(), order.getCustomerId());
    }

    @Override
    public List<GetAllOrdersResponse> getAll() {
        return orderRepository.findAll().stream()
                .map(order -> new GetAllOrdersResponse(
                        order.getId(),
                        order.getCustomerId(),
                        order.getProductId(),
                        order.getQuantity(),
                        order.getUnitPrice(),
                        order.getTotalPrice(),
                        order.getAddress()))
                .toList();
    }

    @Override
    public GetByIdOrderResponse getById(int id) {
        Order order = findOrderOrThrow(id);
        return new GetByIdOrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalPrice(),
                order.getAddress());
    }

    private Order findOrderOrThrow(int id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Order not found with id: " + id));
    }

    private BigDecimal calculateTotalPrice(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
