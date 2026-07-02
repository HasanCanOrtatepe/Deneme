package com.etiya.orderservice.messaging;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.etiya.orderservice.events.CustomerEvent;

/**
 * Spring Cloud Stream consumer. The bean name {@code customerEvents} is referenced by
 * {@code spring.cloud.function.definition} and bound to the input binding {@code customerEvents-in-0}
 * (Kafka topic "customer-events") in application.yml.
 *
 * <p>The bean stays a thin adapter: it delegates to {@link CustomerEventHandler} so the actual
 * apply-and-checkpoint work runs inside a {@code @Transactional} method (idempotency / Inbox).</p>
 */
@Configuration
public class CustomerEventConsumer {

    @Bean
    public Consumer<CustomerEvent> customerEvents(CustomerEventHandler handler) {
        return handler::handle;
    }
}
