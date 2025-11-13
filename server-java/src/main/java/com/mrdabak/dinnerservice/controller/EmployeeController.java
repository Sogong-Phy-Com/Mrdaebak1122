package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class EmployeeController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final DinnerTypeRepository dinnerTypeRepository;
    private final MenuItemRepository menuItemRepository;

    public EmployeeController(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             UserRepository userRepository, DinnerTypeRepository dinnerTypeRepository,
                             MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders(@RequestParam(required = false) String status) {
        List<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }

        List<Map<String, Object>> orderDtos = orders.stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("user_id", order.getUserId());
            orderMap.put("dinner_type_id", order.getDinnerTypeId());
            orderMap.put("serving_style", order.getServingStyle());
            orderMap.put("delivery_time", order.getDeliveryTime());
            orderMap.put("delivery_address", order.getDeliveryAddress());
            orderMap.put("total_price", order.getTotalPrice());
            orderMap.put("status", order.getStatus());
            orderMap.put("payment_status", order.getPaymentStatus());
            orderMap.put("created_at", order.getCreatedAt());

            // Add customer information
            User customer = userRepository.findById(order.getUserId()).orElse(null);
            if (customer != null) {
                orderMap.put("customer_name", customer.getName());
                orderMap.put("customer_phone", customer.getPhone());
            }

            // Add dinner type information
            DinnerType dinner = dinnerTypeRepository.findById(order.getDinnerTypeId()).orElse(null);
            if (dinner != null) {
                orderMap.put("dinner_name", dinner.getName());
                orderMap.put("dinner_name_en", dinner.getNameEn());
            }

            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            List<Map<String, Object>> itemDtos = items.stream().map(item -> {
                MenuItem menuItem = menuItemRepository.findById(item.getMenuItemId()).orElse(null);
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("menu_item_id", item.getMenuItemId());
                itemMap.put("quantity", item.getQuantity());
                if (menuItem != null) {
                    itemMap.put("name", menuItem.getName());
                    itemMap.put("name_en", menuItem.getNameEn());
                    itemMap.put("price", menuItem.getPrice());
                }
                return itemMap;
            }).toList();
            orderMap.put("items", itemDtos);
            return orderMap;
        }).toList();

        return ResponseEntity.ok(orderDtos);
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        if (status == null || !List.of("pending", "cooking", "ready", "out_for_delivery", "delivered", "cancelled").contains(status)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);
        orderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Order status updated successfully"));
    }
}

