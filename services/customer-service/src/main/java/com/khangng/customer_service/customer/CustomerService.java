package com.khangng.customer_service.customer;

import com.khangng.customer_service.entity.Customer;
import com.khangng.customer_service.exception.CustomerNotFoundException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final JwtDecoder jwtDecoder;
    
    public CustomerResponse createCustomer(String bearerToken) {
        Jwt jwt = jwtDecoder.decode(bearerToken.replace("Bearer ", ""));
        String customerId = jwt.getClaimAsString("sub");
        Optional<Customer> existedCustomer = customerRepository.findByCustomerId(customerId);
        if (existedCustomer.isPresent()) {
            return new CustomerResponse(existedCustomer.get());
        }
        
        String userEmail = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        
        Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
        Collection<String> roleList = realmAccess.get("roles");
        
        Customer customer = Customer.builder()
                .customerId(customerId)
                .username(username)
                .role(roleList.contains("admin") ? Role.ADMIN : Role.USER)
                .firstName(firstName)
                .lastName(lastName)
                .email(userEmail)
                .build();
        return new CustomerResponse(customerRepository.save(customer));
    }
    
    public void updateCustomer(String bearerToken, CustomerRequest customerRequest) {
        Jwt jwt = jwtDecoder.decode(bearerToken.replace("Bearer ", ""));
        String customerId = jwt.getClaimAsString("sub");
        Customer existedCustomer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(String.format("No customer found with the provided customerId: %s", customerId)));
        
        if (StringUtils.isNotBlank(customerRequest.firstName())) {
            existedCustomer.setFirstName(customerRequest.firstName());
        }
        if (StringUtils.isNotBlank(customerRequest.lastName())) {
            existedCustomer.setLastName(customerRequest.lastName());
        }
        if (StringUtils.isNotBlank(customerRequest.email())) {
            existedCustomer.setEmail(customerRequest.email());
        }
        if (StringUtils.isNotBlank(customerRequest.address())) {
            existedCustomer.setAddress(customerRequest.address());
        }
        if (StringUtils.isNotBlank(customerRequest.phoneNumber())) {
            existedCustomer.setPhoneNumber(customerRequest.phoneNumber());
        }
        this.customerRepository.save(existedCustomer);
    }
    
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
            .stream()
            .map(CustomerResponse::new)
            .collect(Collectors.toList());
    }
    
    public CustomerResponse getCustomerById(String customerId) {
        return customerRepository.findById(customerId)
            .map(CustomerResponse::new)
            .orElseThrow(() -> new CustomerNotFoundException(String.format("No customer found with the provided ID: %s", customerId)));
    }
    
    public void deleteCustomer(String customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(String.format("No customer found with the provided ID: %s", customerId)));
        customerRepository.deleteById(customerId);
    }
}
