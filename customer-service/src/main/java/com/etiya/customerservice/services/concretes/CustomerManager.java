package com.etiya.customerservice.services.concretes;

import com.etiya.customerservice.entities.Customer;
import com.etiya.customerservice.events.CustomerEvent;
import com.etiya.customerservice.events.CustomerEventType;
import com.etiya.customerservice.outbox.OutboxService;
import com.etiya.customerservice.repositories.CustomerRepository;
import com.etiya.customerservice.services.abstracts.CustomerService;
import com.etiya.customerservice.services.dtos.requests.CreateCustomerRequest;
import com.etiya.customerservice.services.dtos.requests.UpdateCustomerRequest;
import com.etiya.customerservice.services.dtos.responses.CreatedCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.DeletedCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.GetAllCustomersResponse;
import com.etiya.customerservice.services.dtos.responses.GetByIdCustomerResponse;
import com.etiya.customerservice.services.dtos.responses.UpdatedCustomerResponse;
import com.etiya.customerservice.services.exceptions.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business layer implementation. Maps between request/response DTOs and the entity, applies business
 * rules, and — in the same transaction as the DB write — records a {@link CustomerEvent} in the
 * outbox so the change is published to other services without any synchronous call.
 */
@Service
public class CustomerManager implements CustomerService {

    /** Output binding customer lifecycle events are relayed to (topic mapped in application.yml). */
    private static final String CUSTOMER_EVENTS_BINDING = "customerEvents-out-0";
    private static final String AGGREGATE_TYPE = "Customer";

    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;

    public CustomerManager(CustomerRepository customerRepository, OutboxService outboxService) {
        this.customerRepository = customerRepository;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public CreatedCustomerResponse add(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("A customer already exists with email: " + request.getEmail());
        }

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        Customer saved = customerRepository.save(customer);

        recordEvent(CustomerEventType.CREATED, saved);

        return new CreatedCustomerResponse(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getPhone());
    }

    @Override
    @Transactional
    public UpdatedCustomerResponse update(UpdateCustomerRequest request) {
        Customer customer = findCustomerOrThrow(request.getId());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        Customer saved = customerRepository.save(customer);

        recordEvent(CustomerEventType.UPDATED, saved);

        return new UpdatedCustomerResponse(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getPhone());
    }

    @Override
    @Transactional
    public DeletedCustomerResponse delete(int id) {
        Customer customer = findCustomerOrThrow(id);
        customerRepository.deleteById(id);

        recordEvent(CustomerEventType.DELETED, customer);

        return new DeletedCustomerResponse(customer.getId(), customer.getEmail());
    }

    @Override
    public List<GetAllCustomersResponse> getAll() {
        return customerRepository.findAll().stream()
                .map(customer -> new GetAllCustomersResponse(
                        customer.getId(),
                        customer.getFirstName(),
                        customer.getLastName(),
                        customer.getEmail(),
                        customer.getPhone()))
                .toList();
    }

    @Override
    public GetByIdCustomerResponse getById(int id) {
        Customer customer = findCustomerOrThrow(id);
        return new GetByIdCustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone());
    }

    /**
     * Queues a {@link CustomerEvent} envelope in the outbox for the given lifecycle change. Runs
     * inside the caller's transaction, so the event is only relayed if the DB write commits.
     */
    private void recordEvent(CustomerEventType type, Customer customer) {
        CustomerEvent event = new CustomerEvent(
                UUID.randomUUID().toString(),
                type,
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone());

        outboxService.record(
                AGGREGATE_TYPE,
                String.valueOf(customer.getId()),
                "Customer" + capitalize(type),
                CUSTOMER_EVENTS_BINDING,
                event);
    }

    private Customer findCustomerOrThrow(int id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found with id: " + id));
    }

    private static String capitalize(CustomerEventType type) {
        String name = type.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
