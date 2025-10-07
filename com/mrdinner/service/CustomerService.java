package com.mrdinner.service;

import com.mrdinner.domain.customer.Customer;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.common.Address;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing customer operations
 * 고객 관련 비즈니스 로직을 처리하는 서비스
 */
public class CustomerService {
    private List<Customer> customers;
    
    public CustomerService() {
        this.customers = new ArrayList<>();
        initializeSampleCustomers();
    }
    
    /**
     * 고객 회원가입
     */
    public Customer register(Customer customer) {
        // 이메일 중복 체크
        if (getCustomerByEmail(customer.getEmail()) != null) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다: " + customer.getEmail());
        }
        
        customers.add(customer);
        System.out.println("고객 회원가입 완료: " + customer.getName() + " (" + customer.getEmail() + ")");
        return customer;
    }
    
    /**
     * 고객 로그인 인증
     */
    public Customer authenticate(String email, String password) {
        Customer customer = getCustomerByEmail(email);
        
        if (customer != null && customer.authenticate(password)) {
            System.out.println("고객 로그인 성공: " + customer.getName());
            return customer;
        }
        
        return null;
    }
    
    /**
     * 고객 ID로 조회
     */
    public Customer getCustomerById(String customerId) {
        return customers.stream()
            .filter(c -> c.getCustomerId().equals(customerId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 이메일로 고객 조회
     */
    public Customer getCustomerByEmail(String email) {
        return customers.stream()
            .filter(c -> c.getEmail().equals(email))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 고객 주문 내역 조회 (최신순)
     */
    public List<Map<String, Object>> getCustomerOrders(String customerId) {
        // 실제 구현에서는 OrderService와 연동
        List<Map<String, Object>> orders = new ArrayList<>();
        
        // 샘플 데이터
        Map<String, Object> order1 = new HashMap<>();
        order1.put("orderId", "ORD-001");
        order1.put("orderTime", LocalDateTime.now().minusDays(1));
        order1.put("dinnerType", "발렌타인 디너");
        order1.put("servingStyle", "그랜드");
        order1.put("price", "₩97,750");
        order1.put("deliveryTime", LocalDateTime.now().minusDays(1).plusHours(2));
        order1.put("deliveryAddress", "서울시 강남구 테헤란로 123");
        order1.put("status", "배달완료");
        
        Map<String, Object> order2 = new HashMap<>();
        order2.put("orderId", "ORD-002");
        order2.put("orderTime", LocalDateTime.now().minusDays(7));
        order2.put("dinnerType", "프렌치 디너");
        order2.put("servingStyle", "디럭스");
        order2.put("price", "₩123,500");
        order2.put("deliveryTime", LocalDateTime.now().minusDays(7).plusHours(2));
        order2.put("deliveryAddress", "서울시 강남구 테헤란로 123");
        order2.put("status", "배달완료");
        
        orders.add(order1);
        orders.add(order2);
        
        return orders;
    }
    
    /**
     * 모든 고객 조회
     */
    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers);
    }
    
    /**
     * 활성 고객만 조회
     */
    public List<Customer> getActiveCustomers() {
        return customers.stream()
            .filter(Customer::isActive)
            .collect(Collectors.toList());
    }
    
    /**
     * 단골 고객 조회 (5회 이상 주문)
     */
    public List<Customer> getLoyaltyCustomers() {
        return customers.stream()
            .filter(Customer::isLoyaltyCustomer)
            .collect(Collectors.toList());
    }
    
    /**
     * 고객 주소 업데이트
     */
    public Customer updateCustomerAddress(String customerId, Address newAddress) {
        Customer customer = getCustomerById(customerId);
        
        if (customer != null) {
            customer.setDeliveryAddress(newAddress);
            System.out.println("고객 주소 업데이트: " + customer.getName());
            return customer;
        }
        
        throw new IllegalArgumentException("고객을 찾을 수 없습니다: " + customerId);
    }
    
    /**
     * 고객 비밀번호 변경
     */
    public Customer updateCustomerPassword(String customerId, String newPassword) {
        Customer customer = getCustomerById(customerId);
        
        if (customer != null) {
            customer.updatePassword(newPassword);
            System.out.println("고객 비밀번호 변경: " + customer.getName());
            return customer;
        }
        
        throw new IllegalArgumentException("고객을 찾을 수 없습니다: " + customerId);
    }
    
    /**
     * 고객 계정 비활성화
     */
    public void deactivateCustomer(String customerId) {
        Customer customer = getCustomerById(customerId);
        
        if (customer != null) {
            customer.deactivate();
            System.out.println("고객 계정 비활성화: " + customer.getName());
        } else {
            throw new IllegalArgumentException("고객을 찾을 수 없습니다: " + customerId);
        }
    }
    
    /**
     * 고객 통계 조회
     */
    public Map<String, Object> getCustomerStatistics() {
        long totalCustomers = customers.size();
        long activeCustomers = getActiveCustomers().size();
        long loyaltyCustomers = getLoyaltyCustomers().size();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", totalCustomers);
        stats.put("activeCustomers", activeCustomers);
        stats.put("loyaltyCustomers", loyaltyCustomers);
        stats.put("loyaltyRate", totalCustomers > 0 ? (double) loyaltyCustomers / totalCustomers : 0.0);
        
        return stats;
    }
    
    /**
     * 샘플 고객 데이터 초기화
     */
    private void initializeSampleCustomers() {
        try {
            Address address1 = new Address("서울시 강남구 테헤란로 123", "서울시", "강남구", "06292", "대한민국");
            Customer customer1 = new Customer("김철수", "kim@email.com", "010-1234-5678", address1, "password123");
            customers.add(customer1);
            
            Address address2 = new Address("서울시 서초구 강남대로 456", "서울시", "서초구", "06578", "대한민국");
            Customer customer2 = new Customer("이영희", "lee@email.com", "010-2345-6789", address2, "password456");
            customer2.incrementOrderCount(); // 단골 고객으로 설정
            customer2.incrementOrderCount();
            customer2.incrementOrderCount();
            customers.add(customer2);
            
            Address address3 = new Address("서울시 송파구 올림픽로 789", "서울시", "송파구", "05540", "대한민국");
            Customer customer3 = new Customer("박민수", "park@email.com", "010-3456-7890", address3, "password789");
            customers.add(customer3);
            
            System.out.println("샘플 고객 데이터 초기화 완료: " + customers.size() + "명");
        } catch (Exception e) {
            System.out.println("샘플 고객 데이터 초기화 실패: " + e.getMessage());
        }
    }
}
