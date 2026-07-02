package com.etiya.notificationservice.messaging;

import com.etiya.notificationservice.entities.Notification;
import com.etiya.notificationservice.events.CartCheckedOutEvent;
import com.etiya.notificationservice.events.CustomerEvent;
import com.etiya.notificationservice.events.OrderCreatedEvent;
import com.etiya.notificationservice.inbox.InboxMessage;
import com.etiya.notificationservice.inbox.InboxRepository;
import com.etiya.notificationservice.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Turns inbound domain events from across the system into notifications, idempotently (Inbox
 * pattern).
 *
 * <p>Each {@code handle*} method runs in one transaction: the inbox checkpoint and the notification
 * row commit together. Because the brokers deliver <em>at-least-once</em>, the {@link InboxRepository}
 * lookup makes sure a re-delivered event never produces a duplicate notification.</p>
 */
@Service
public class NotificationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final InboxRepository inboxRepository;
    private final NotificationRepository notificationRepository;

    public NotificationEventHandler(InboxRepository inboxRepository,
                                    NotificationRepository notificationRepository) {
        this.inboxRepository = inboxRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        // OrderCreated carries no unique event id, so derive a stable natural key: one per order.
        String messageId = "OrderCreated:" + event.orderId();
        if (alreadyProcessed(messageId, "OrderCreated")) {
            return;
        }
        String message = String.format(
                "Order #%d placed successfully. Total: %s. It will be shipped to: %s",
                event.orderId(), event.totalPrice(), event.address());
        send("ORDER_PLACED", event.customerId(), message, messageId, "OrderCreated");
    }

    @Transactional
    public void handleCustomerEvent(CustomerEvent event) {
        if (event == null || event.eventId() == null) {
            log.warn("Discarding customer event with no eventId: {}", event);
            return;
        }
        if (alreadyProcessed(event.eventId(), event.eventType().name())) {
            return;
        }
        // Only a newly created customer is welcomed; other lifecycle changes are checkpointed only.
        if (event.eventType() == com.etiya.notificationservice.events.CustomerEventType.CREATED) {
            String message = String.format(
                    "Welcome %s %s! Your account has been created.",
                    event.firstName(), event.lastName());
            send("CUSTOMER_WELCOME", event.id(), message, event.eventId(), event.eventType().name());
        } else {
            checkpoint(event.eventId(), event.eventType().name());
        }
    }

    @Transactional
    public void handleCartCheckedOut(CartCheckedOutEvent event) {
        if (event == null || event.eventId() == null) {
            log.warn("Discarding cart-checked-out event with no eventId: {}", event);
            return;
        }
        if (alreadyProcessed(event.eventId(), "CartCheckedOut")) {
            return;
        }
        String message = String.format(
                "Your cart #%d has been checked out. Total: %s (%d item type(s)).",
                event.cartId(), event.totalPrice(),
                event.items() == null ? 0 : event.items().size());
        send("CART_CHECKED_OUT", event.customerId(), message, event.eventId(), "CartCheckedOut");
    }

    private boolean alreadyProcessed(String messageId, String eventType) {
        if (inboxRepository.existsById(messageId)) {
            log.debug("Duplicate {} event {} ignored", eventType, messageId);
            return true;
        }
        return false;
    }

    /** Persists the notification and its inbox checkpoint in the same transaction. */
    private void send(String type, int customerId, String message, String messageId, String eventType) {
        notificationRepository.save(new Notification(type, customerId, message, Instant.now()));
        checkpoint(messageId, eventType);
        log.info("Notification [{}] for customerId={}: {}", type, customerId, message);
    }

    private void checkpoint(String messageId, String eventType) {
        inboxRepository.save(new InboxMessage(messageId, eventType, Instant.now()));
    }
}
