package com.etiya.customerservice.services.dtos.responses;

public class DeletedCustomerResponse {

    private int id;
    private String email;

    public DeletedCustomerResponse() {
    }

    public DeletedCustomerResponse(int id, String email) {
        this.id = id;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
