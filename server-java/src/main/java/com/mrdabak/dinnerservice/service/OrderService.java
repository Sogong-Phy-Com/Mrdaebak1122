package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.dto.OrderItemDto;
import com.mrdabak.dinnerservice.dto.OrderRequest;
import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DinnerTypeRepository dinnerTypeRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                       DinnerTypeRepository dinnerTypeRepository, MenuItemRepository menuItemRepository,
                       UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(transactionManager = "orderTransactionManager", rollbackFor = Exception.class)
    public Order createOrder(Long userId, OrderRequest request) {
        int maxRetries = 5;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                return createOrderInternal(userId, request);
            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : "";
                String causeMessage = e.getCause() != null && e.getCause().getMessage() != null 
                    ? e.getCause().getMessage() : "";
                
                boolean isLocked = errorMessage.contains("database is locked") 
                    || errorMessage.contains("SQLITE_BUSY")
                    || causeMessage.contains("database is locked")
                    || causeMessage.contains("SQLITE_BUSY");
                
                if (isLocked && retryCount < maxRetries - 1) {
                    retryCount++;
                    System.out.println("[OrderService] Database locked, retrying... (" + retryCount + "/" + maxRetries + ")");
                    try {
                        Thread.sleep(200 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Order creation interrupted", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Failed to create order after " + maxRetries + " retries");
    }
    
    private Order createOrderInternal(Long userId, OrderRequest request) {
        // Read from main database (menu, dinner types)
        DinnerType dinner = dinnerTypeRepository.findById(request.getDinnerTypeId())
                .orElseThrow(() -> new RuntimeException("Invalid dinner type"));

        // Validate serving style for Champagne Feast
        if (dinner.getName().contains("샴페인") && !request.getServingStyle().equals("grand") && !request.getServingStyle().equals("deluxe")) {
            throw new RuntimeException("Champagne Feast dinner can only be ordered with grand or deluxe style");
        }

        // Calculate price
        Map<String, Double> styleMultipliers = Map.of(
                "simple", 1.0,
                "grand", 1.3,
                "deluxe", 1.6
        );
        double basePrice = dinner.getBasePrice() * styleMultipliers.getOrDefault(request.getServingStyle(), 1.0);

        // Add item prices
        double itemsPrice = 0;
        for (OrderItemDto item : request.getItems()) {
            if (item.getMenuItemId() == null) {
                throw new RuntimeException("Menu item ID is required");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Menu item quantity must be greater than 0");
            }
            MenuItem menuItem = menuItemRepository.findById(item.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Invalid menu item ID: " + item.getMenuItemId()));
            itemsPrice += menuItem.getPrice() * item.getQuantity();
        }

        double totalPrice = basePrice + itemsPrice;

        // Apply loyalty discount (10% for returning customers) - read from order database
        long orderCount = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> "paid".equals(o.getPaymentStatus()))
                .count();
        if (orderCount > 0) {
            totalPrice = totalPrice * 0.9;
        }

        // Create order - save to order database
        Order order = new Order();
        order.setUserId(userId);
        order.setDinnerTypeId(request.getDinnerTypeId());
        order.setServingStyle(request.getServingStyle());
        order.setDeliveryTime(request.getDeliveryTime());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setTotalPrice((int) Math.round(totalPrice));
        order.setPaymentStatus("pending");
        order.setPaymentMethod(request.getPaymentMethod());

        Order savedOrder = orderRepository.save(order);

        // Add order items - save to order database
        for (OrderItemDto item : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setMenuItemId(item.getMenuItemId());
            orderItem.setQuantity(item.getQuantity());
            orderItemRepository.save(orderItem);
        }

        return savedOrder;
    }

    public List<Order> getUserOrders(Long userId) {
        System.out.println("[OrderService] getUserOrders 호출 - 사용자 ID: " + userId);
        
        try {
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            System.out.println("[OrderService] 주문 조회 완료: " + orders.size() + "개");
            
            if (orders.isEmpty()) {
                System.out.println("[OrderService] 주의: 주문이 없습니다.");
            } else {
                System.out.println("[OrderService] 주문 ID 목록: " + orders.stream()
                    .map(o -> o.getId().toString())
                    .collect(java.util.stream.Collectors.joining(", ")));
            }
            
            return orders;
        } catch (Exception e) {
            System.out.println("[OrderService] 주문 조회 중 오류 발생");
            System.out.println("[OrderService] 예외 타입: " + e.getClass().getName());
            System.out.println("[OrderService] 예외 메시지: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch user orders: " + e.getMessage(), e);
        }
    }

    public Order getOrder(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}

