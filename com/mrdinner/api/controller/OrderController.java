package com.mrdinner.api.controller;

import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.order.OrderStatus;
import com.mrdinner.domain.customer.Customer;
import com.mrdinner.domain.common.Address;
import com.mrdinner.service.OrderService;
import com.mrdinner.service.CustomerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST API Controller for Order operations
 * 앱/웹에서 주문 관련 기능을 처리하는 API
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final CustomerService customerService;
    
    public OrderController(OrderService orderService, CustomerService customerService) {
        this.orderService = orderService;
        this.customerService = customerService;
    }
    
    /**
     * 새 주문 생성 API
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            // 고객 정보 조회
            Customer customer = customerService.getCustomerById(request.getCustomerId());
            if (customer == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "고객 정보를 찾을 수 없습니다"
                ));
            }
            
            // 배달 주소 생성
            Address deliveryAddress = new Address(
                request.getDeliveryAddress().getStreetAddress(),
                request.getDeliveryAddress().getCity(),
                request.getDeliveryAddress().getState(),
                request.getDeliveryAddress().getPostalCode(),
                request.getDeliveryAddress().getCountry()
            );
            
            // 주문 생성
            Order order = orderService.createOrder(customer, deliveryAddress);
            
            // 메뉴 아이템 추가
            for (OrderItemRequest itemRequest : request.getItems()) {
                orderService.addMenuItemToOrder(
                    order, 
                    itemRequest.getMenuItemId(), 
                    itemRequest.getQuantity()
                );
            }
            
            // 디너 추가
            for (DinnerItemRequest dinnerRequest : request.getDinners()) {
                orderService.addDinnerToOrder(
                    order,
                    dinnerRequest.getDinnerType(),
                    dinnerRequest.getServingStyle(),
                    dinnerRequest.getQuantity()
                );
            }
            
            // 주문 확정
            order.confirm();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "주문이 성공적으로 생성되었습니다",
                "order", Map.of(
                    "orderId", order.getOrderId(),
                    "status", order.getStatus().toString(),
                    "totalAmount", order.getTotalAmount().toString(),
                    "estimatedDeliveryTime", order.getEstimatedDeliveryTime(),
                    "orderTime", order.getOrderTime()
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 생성 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 주문 조회 API
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "order", Map.of(
                        "orderId", order.getOrderId(),
                        "customerName", order.getCustomer().getName(),
                        "status", order.getStatus().toString(),
                        "orderTime", order.getOrderTime(),
                        "estimatedDeliveryTime", order.getEstimatedDeliveryTime(),
                        "subtotal", order.getSubtotal().toString(),
                        "tax", order.getTax().toString(),
                        "deliveryFee", order.getDeliveryFee().toString(),
                        "totalAmount", order.getTotalAmount().toString(),
                        "items", order.getOrderItems().stream().map(item -> Map.of(
                            "itemName", item.getItemName(),
                            "quantity", item.getQuantity(),
                            "unitPrice", item.getUnitPrice().toString(),
                            "totalPrice", item.getTotalPrice().toString()
                        )).toList(),
                        "deliveryAddress", Map.of(
                            "streetAddress", order.getDeliveryAddress().getStreetAddress(),
                            "city", order.getDeliveryAddress().getCity(),
                            "state", order.getDeliveryAddress().getState(),
                            "postalCode", order.getDeliveryAddress().getPostalCode(),
                            "country", order.getDeliveryAddress().getCountry()
                        ),
                        "notes", order.getNotes()
                    )
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 고객 주문 내역 조회 API
     * GET /api/orders/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerOrders(@PathVariable String customerId) {
        try {
            List<Map<String, Object>> orders = orderService.getOrdersByCustomerId(customerId);
            
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
    
    /**
     * 주문 상태 업데이트 API
     * PUT /api/orders/{orderId}/status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String orderId, 
            @RequestBody UpdateStatusRequest request) {
        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order != null) {
                OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
                order.setStatus(newStatus);
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "주문 상태가 업데이트되었습니다",
                    "orderId", orderId,
                    "newStatus", newStatus.toString()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 상태 업데이트 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 주문 취소 API
     * DELETE /api/orders/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order != null) {
                order.cancel();
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "주문이 취소되었습니다",
                    "orderId", orderId,
                    "status", order.getStatus().toString()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 취소 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 주문 수정 API (주문 후 음식 항목 추가/변경/삭제)
     * PUT /api/orders/{orderId}/items
     */
    @PutMapping("/{orderId}/items")
    public ResponseEntity<Map<String, Object>> modifyOrderItems(
            @PathVariable String orderId,
            @RequestBody ModifyOrderRequest request) {
        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order != null && order.isModifiable()) {
                // 기존 아이템 제거
                for (String itemId : request.getRemoveItems()) {
                    orderService.removeMenuItemFromOrder(order, itemId);
                }
                
                // 새 아이템 추가
                for (OrderItemRequest itemRequest : request.getAddItems()) {
                    orderService.addMenuItemToOrder(
                        order,
                        itemRequest.getMenuItemId(),
                        itemRequest.getQuantity()
                    );
                }
                
                // 수량 변경
                for (OrderItemRequest itemRequest : request.getUpdateItems()) {
                    orderService.updateMenuItemQuantity(
                        order,
                        itemRequest.getMenuItemId(),
                        itemRequest.getQuantity()
                    );
                }
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "주문이 수정되었습니다",
                    "orderId", orderId,
                    "newTotalAmount", order.getTotalAmount().toString()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "주문을 수정할 수 없습니다"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 수정 실패: " + e.getMessage()
            ));
        }
    }
    
    // Request DTOs
    public static class CreateOrderRequest {
        private String customerId;
        private List<OrderItemRequest> items;
        private List<DinnerItemRequest> dinners;
        private AddressRequest deliveryAddress;
        private String notes;
        
        // Getters and Setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public List<OrderItemRequest> getItems() { return items; }
        public void setItems(List<OrderItemRequest> items) { this.items = items; }
        
        public List<DinnerItemRequest> getDinners() { return dinners; }
        public void setDinners(List<DinnerItemRequest> dinners) { this.dinners = dinners; }
        
        public AddressRequest getDeliveryAddress() { return deliveryAddress; }
        public void setDeliveryAddress(AddressRequest deliveryAddress) { this.deliveryAddress = deliveryAddress; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    public static class OrderItemRequest {
        private String menuItemId;
        private int quantity;
        
        // Getters and Setters
        public String getMenuItemId() { return menuItemId; }
        public void setMenuItemId(String menuItemId) { this.menuItemId = menuItemId; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    
    public static class DinnerItemRequest {
        private String dinnerType;
        private String servingStyle;
        private int quantity;
        
        // Getters and Setters
        public String getDinnerType() { return dinnerType; }
        public void setDinnerType(String dinnerType) { this.dinnerType = dinnerType; }
        
        public String getServingStyle() { return servingStyle; }
        public void setServingStyle(String servingStyle) { this.servingStyle = servingStyle; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    
    public static class AddressRequest {
        private String streetAddress;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        
        // Getters and Setters
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
    
    public static class UpdateStatusRequest {
        private String status;
        
        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class ModifyOrderRequest {
        private List<String> removeItems;
        private List<OrderItemRequest> addItems;
        private List<OrderItemRequest> updateItems;
        
        // Getters and Setters
        public List<String> getRemoveItems() { return removeItems; }
        public void setRemoveItems(List<String> removeItems) { this.removeItems = removeItems; }
        
        public List<OrderItemRequest> getAddItems() { return addItems; }
        public void setAddItems(List<OrderItemRequest> addItems) { this.addItems = addItems; }
        
        public List<OrderItemRequest> getUpdateItems() { return updateItems; }
        public void setUpdateItems(List<OrderItemRequest> updateItems) { this.updateItems = updateItems; }
    }
}
