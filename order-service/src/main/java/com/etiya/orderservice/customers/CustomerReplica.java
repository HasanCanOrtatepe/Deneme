package com.etiya.orderservice.customers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Local read-model of a customer, kept eventually consistent from {@code customer-events}.
 *
 * <p>order-service never calls customer-service synchronously. Instead it replicates the minimum
 * customer data it needs into this table (MySQL) by consuming customer lifecycle events, so it can
 * validate an order's {@code customerId} locally. The id mirrors the customer-service primary key.</p>
 */
@Entity
@Table(name = "customer_replica")
public class CustomerReplica {

    @Id
    private int id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    /** Required by JPA. */
    protected CustomerReplica() {
    }

    public CustomerReplica(int id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
