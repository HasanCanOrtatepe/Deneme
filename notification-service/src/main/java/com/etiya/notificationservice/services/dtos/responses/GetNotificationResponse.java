package com.etiya.notificationservice.services.dtos.responses;

import java.time.Instant;

public class GetNotificationResponse {

    private Long id;
    private String type;
    private int customerId;
    private String message;
    private Instant createdAt;

    public GetNotificationResponse() {
    }

    public GetNotificationResponse(Long id, String type, int customerId, String message, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.customerId = customerId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
