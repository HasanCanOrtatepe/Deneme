package com.etiya.notificationservice.repositories;

import com.etiya.notificationservice.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA access to the {@code notifications} table (H2).
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByCustomerIdOrderByIdDesc(int customerId);
}
