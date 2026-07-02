package com.etiya.customerservice.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.etiya.customerservice.services.abstracts.CustomerService;
import com.etiya.customerservice.services.dtos.requests.CreateCustomerRequest;
import com.etiya.customerservice.services.dtos.requests.UpdateCustomerRequest;
import com.etiya.customerservice.services.dtos.responses.CreatedCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.DeletedCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.GetAllCustomersResponse;
import com.etiya.customerservice.services.dtos.responses.GetByIdCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.UpdatedCustomerResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomersController {

    private final CustomerService customerService;

    public CustomersController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<GetAllCustomersResponse> getAll() {
        return customerService.getAll();
    }

    @GetMapping("/{id}")
    public GetByIdCustomerResponse getById(@PathVariable int id) {
        return customerService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedCustomerResponse add(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.add(request);
    }

    @PutMapping("/{id}")
    public UpdatedCustomerResponse update(@PathVariable int id, @Valid @RequestBody UpdateCustomerRequest request) {
        request.setId(id);
        return customerService.update(request);
    }

    @DeleteMapping("/{id}")
    public DeletedCustomerResponse delete(@PathVariable int id) {
        return customerService.delete(id);
    }
}
