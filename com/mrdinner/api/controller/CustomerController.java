package com.mrdinner.api.controller;

import com.mrdinner.domain.customer.Customer;
import com.mrdinner.domain.common.Address;
import com.mrdinner.service.CustomerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Customer operations
 * 앱/웹에서 고객 관련 기능을 처리하는 API
 */
@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*") // 앱/웹에서 접근 허용
public class CustomerController {
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * 회원가입 API
     * POST /api/customers/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody CustomerRegistrationRequest request) {
        try {
            Address address = new Address(
                request.getStreetAddress(),
                request.getCity(),
                request.getState(),
                request.getPostalCode(),
                request.getCountry()
            );
            
            Customer customer = new Customer(
                request.getName(),
                request.getEmail(),
                request.getPhoneNumber(),
                address,
                request.getPassword()
            );
            
            Customer savedCustomer = customerService.register(customer);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "회원가입이 완료되었습니다",
                "customer", Map.of(
                    "customerId", savedCustomer.getCustomerId(),
                    "name", savedCustomer.getName(),
                    "email", savedCustomer.getEmail(),
                    "orderCount", savedCustomer.getOrderCount(),
                    "loyaltyDiscount", savedCustomer.getLoyaltyDiscountRate()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "회원가입 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 로그인 API
     * POST /api/customers/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            Customer customer = customerService.authenticate(request.getEmail(), request.getPassword());
            
            if (customer != null && customer.isActive()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "로그인 성공",
                    "customer", Map.of(
                        "customerId", customer.getCustomerId(),
                        "name", customer.getName(),
                        "email", customer.getEmail(),
                        "phoneNumber", customer.getPhoneNumber(),
                        "orderCount", customer.getOrderCount(),
                        "loyaltyDiscount", customer.getLoyaltyDiscountRate(),
                        "isLoyaltyCustomer", customer.isLoyaltyCustomer(),
                        "lastLoginDate", customer.getLastLoginDate()
                    )
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "이메일 또는 비밀번호가 올바르지 않습니다"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "message", "로그인 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 고객 정보 조회 API
     * GET /api/customers/{customerId}
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable String customerId) {
        try {
            Customer customer = customerService.getCustomerById(customerId);
            
            if (customer != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "customer", Map.of(
                        "customerId", customer.getCustomerId(),
                        "name", customer.getName(),
                        "email", customer.getEmail(),
                        "phoneNumber", customer.getPhoneNumber(),
                        "orderCount", customer.getOrderCount(),
                        "loyaltyDiscount", customer.getLoyaltyDiscountRate(),
                        "isLoyaltyCustomer", customer.isLoyaltyCustomer(),
                        "registrationDate", customer.getRegistrationDate(),
                        "lastLoginDate", customer.getLastLoginDate(),
                        "address", Map.of(
                            "streetAddress", customer.getDeliveryAddress().getStreetAddress(),
                            "city", customer.getDeliveryAddress().getCity(),
                            "state", customer.getDeliveryAddress().getState(),
                            "postalCode", customer.getDeliveryAddress().getPostalCode(),
                            "country", customer.getDeliveryAddress().getCountry()
                        )
                    )
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "고객 정보 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 고객 주문 내역 조회 API
     * GET /api/customers/{customerId}/orders
     */
    @GetMapping("/{customerId}/orders")
    public ResponseEntity<Map<String, Object>> getCustomerOrders(@PathVariable String customerId) {
        try {
            List<Map<String, Object>> orders = customerService.getCustomerOrders(customerId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "orders", orders,
                "totalCount", orders.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 내역 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    // Request DTOs
    public static class CustomerRegistrationRequest {
        private String name;
        private String email;
        private String phoneNumber;
        private String password;
        private String streetAddress;
        private String city;
        private String state;
        private String postalCode;
        private String country = "대한민국";
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getStreetAddress() { return streetAddress; }
        public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }
    
    public static class LoginRequest {
        private String email;
        private String password;
        
        // Getters and Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
