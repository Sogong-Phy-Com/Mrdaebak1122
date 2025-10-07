package com.mrdinner.domain.customer;

import com.mrdinner.domain.common.Address;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer entity representing a customer in the system
 * 미스터 대박 디너 서비스의 고객 정보 관리
 */
public class Customer {
    private final String customerId;
    private String name;
    private String email;
    private String phoneNumber;
    private Address deliveryAddress;
    private CustomerStatus status;
    private String password; // 로그인용 비밀번호
    private LocalDateTime registrationDate; // 회원가입일
    private LocalDateTime lastLoginDate; // 마지막 로그인일
    private int orderCount; // 총 주문 횟수 (단골 고객 할인용)

    public Customer(String name, String email, String phoneNumber, Address deliveryAddress, String password) {
        this.customerId = UUID.randomUUID().toString();
        this.name = validateAndTrim(name, "Name");
        this.email = validateEmail(email);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "Delivery address cannot be null");
        this.password = validatePassword(password);
        this.status = CustomerStatus.ACTIVE;
        this.registrationDate = LocalDateTime.now();
        this.orderCount = 0;
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    private String validateEmail(String email) {
        String trimmed = validateAndTrim(email, "Email");
        if (!trimmed.contains("@") || !trimmed.contains(".")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return trimmed;
    }

    private String validatePhoneNumber(String phoneNumber) {
        String trimmed = validateAndTrim(phoneNumber, "Phone number");
        if (!trimmed.matches("\\d{10,15}")) {
            throw new IllegalArgumentException("Phone number must contain 10-15 digits");
        }
        return trimmed;
    }

    private String validatePassword(String password) {
        String trimmed = validateAndTrim(password, "Password");
        if (trimmed.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        return trimmed;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateAndTrim(name, "Name");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = validateEmail(email);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = validatePhoneNumber(phoneNumber);
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "Delivery address cannot be null");
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
    }

    public void activate() {
        this.status = CustomerStatus.ACTIVE;
    }

    // 로그인 관련 메서드
    public boolean authenticate(String password) {
        if (password == null || !this.password.equals(password)) {
            return false;
        }
        this.lastLoginDate = LocalDateTime.now();
        return true;
    }

    public void updatePassword(String newPassword) {
        this.password = validatePassword(newPassword);
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void incrementOrderCount() {
        this.orderCount++;
    }

    // 단골 고객 할인율 계산 (주문 횟수에 따른 할인)
    public double getLoyaltyDiscountRate() {
        if (orderCount >= 20) return 0.15; // 15% 할인 (20회 이상)
        if (orderCount >= 10) return 0.10; // 10% 할인 (10회 이상)
        if (orderCount >= 5) return 0.05;  // 5% 할인 (5회 이상)
        return 0.0; // 할인 없음
    }

    public boolean isLoyaltyCustomer() {
        return orderCount >= 5;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer customer = (Customer) obj;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return String.format("Customer{id='%s', name='%s', email='%s', status=%s}", 
            customerId, name, email, status);
    }

    public enum CustomerStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}

