package com.etiya.notificationservice.messaging;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.etiya.notificationservice.events.CartCheckedOutEvent;
import com.etiya.notificationservice.events.CustomerEvent;
import com.etiya.notificationservice.events.OrderCreatedEvent;

/**
 * Spring Cloud Stream consumers. The bean names {@code orderCreated}, {@code customerEvents} and
 * {@code cartCheckedOut} are listed in {@code spring.cloud.function.definition} and each bound to
 * its {@code *-in-0} input binding (Kafka topics order-created / customer-events / cart-checked-out)
 * in application.yml.
 *
 * <p>The beans stay thin adapters: they delegate to {@link NotificationEventHandler} so the actual
 * notify-and-checkpoint work runs inside {@code @Transactional} methods (idempotency / Inbox).</p>
 */
@Configuration
public class NotificationEventConsumer {

    @Bean
    public Consumer<OrderCreatedEvent> orderCreated(NotificationEventHandler handler) {
        return handler::handleOrderCreated;
    }

    @Bean
    public Consumer<CustomerEvent> customerEvents(NotificationEventHandler handler) {
        return handler::handleCustomerEvent;
    }

    @Bean
    public Consumer<CartCheckedOutEvent> cartCheckedOut(NotificationEventHandler handler) {
        return handler::handleCartCheckedOut;
    }
}
