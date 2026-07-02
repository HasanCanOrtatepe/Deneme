package com.etiya.customerservice.services.abstracts;

import com.etiya.customerservice.services.dtos.requests.CreateCustomerRequest;
import com.etiya.customerservice.services.dtos.requests.UpdateCustomerRequest;
import com.etiya.customerservice.services.dtos.responses.CreatedCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.DeletedCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.GetAllCustomersResponse;
import com.etiya.customerservice.services.dtos.responses.GetByIdCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.UpdatedCustomerResponse;

import java.util.List;

/**
 * Business layer contract. Controllers depend on this abstraction, never on the concrete manager.
 */
public interface CustomerService {

    CreatedCustomerResponse add(CreateCustomerRequest request);

    UpdatedCustomerResponse update(UpdateCustomerRequest request);

    DeletedCustomerResponse delete(int id);

    List<GetAllCustomersResponse> getAll();

    GetByIdCustomerResponse getById(int id);
}
