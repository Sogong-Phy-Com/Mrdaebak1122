package com.mrdinner.api.controller;

import com.mrdinner.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST API Controller for Admin operations
 * 웹 관리자 페이지용 API
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private final OrderService orderService;
    private final CustomerService customerService;
    private final MenuService menuService;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;
    
    public AdminController(OrderService orderService, CustomerService customerService,
                          MenuService menuService, InventoryService inventoryService,
                          DeliveryService deliveryService) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.menuService = menuService;
        this.inventoryService = inventoryService;
        this.deliveryService = deliveryService;
    }
    
    /**
     * 관리자 대시보드 통계 API
     * GET /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 주문 통계
            stats.put("totalOrders", orderService.getTotalOrderCount());
            stats.put("todayOrders", orderService.getTodayOrderCount());
            stats.put("pendingOrders", orderService.getPendingOrderCount());
            stats.put("completedOrders", orderService.getCompletedOrderCount());
            
            // 매출 통계 (원화 기준)
            stats.put("todayRevenue", orderService.getTodayRevenue());
            stats.put("monthlyRevenue", orderService.getMonthlyRevenue());
            
            // 고객 통계
            Map<String, Object> customerStats = customerService.getCustomerStatistics();
            stats.put("customerStats", customerStats);
            
            // 배달 통계
            stats.put("activeDeliveries", deliveryService.getActiveDeliveryCount());
            stats.put("completedDeliveries", deliveryService.getCompletedDeliveryCount());
            
            // 재고 통계
            stats.put("lowStockItems", inventoryService.getLowStockItemsCount());
            stats.put("outOfStockItems", inventoryService.getOutOfStockItemsCount());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "대시보드 데이터 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 모든 주문 목록 조회 API
     * GET /api/admin/orders
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        try {
            List<Map<String, Object>> orders = orderService.getAllOrders();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "orders", orders,
                "totalCount", orders.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 목록 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 주문 상세 정보 조회 API
     * GET /api/admin/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable String orderId) {
        try {
            Map<String, Object> orderDetails = orderService.getOrderDetailsForAdmin(orderId);
            
            if (orderDetails != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "order", orderDetails
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "주문 상세 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 고객 목록 조회 API
     * GET /api/admin/customers
     */
    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getAllCustomers() {
        try {
            List<Map<String, Object>> customers = customerService.getAllCustomersForAdmin();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "customers", customers,
                "totalCount", customers.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "고객 목록 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 단골 고객 목록 조회 API
     * GET /api/admin/customers/loyalty
     */
    @GetMapping("/customers/loyalty")
    public ResponseEntity<Map<String, Object>> getLoyaltyCustomers() {
        try {
            List<Map<String, Object>> loyaltyCustomers = customerService.getLoyaltyCustomersForAdmin();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "loyaltyCustomers", loyaltyCustomers,
                "totalCount", loyaltyCustomers.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "단골 고객 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 재고 현황 조회 API
     * GET /api/admin/inventory
     */
    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventoryStatus() {
        try {
            List<Map<String, Object>> inventoryItems = inventoryService.getInventoryStatus();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "inventory", inventoryItems,
                "totalCount", inventoryItems.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "재고 현황 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 배달 현황 조회 API
     * GET /api/admin/deliveries
     */
    @GetMapping("/deliveries")
    public ResponseEntity<Map<String, Object>> getDeliveryStatus() {
        try {
            List<Map<String, Object>> deliveries = deliveryService.getAllDeliveriesForAdmin();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "deliveries", deliveries,
                "totalCount", deliveries.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "배달 현황 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 배달원 배정 API
     * PUT /api/admin/deliveries/{deliveryId}/assign
     */
    @PutMapping("/deliveries/{deliveryId}/assign")
    public ResponseEntity<Map<String, Object>> assignCourier(
            @PathVariable String deliveryId,
            @RequestBody AssignCourierRequest request) {
        try {
            boolean success = deliveryService.assignCourierToDelivery(deliveryId, request.getCourierId());
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "배달원이 성공적으로 배정되었습니다"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "배달원 배정 실패"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "배달원 배정 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 메뉴 관리 API
     * GET /api/admin/menu
     */
    @GetMapping("/menu")
    public ResponseEntity<Map<String, Object>> getMenuManagement() {
        try {
            Map<String, Object> menuData = new HashMap<>();
            menuData.put("dinners", menuService.getAllDinners());
            menuData.put("servingStyles", menuService.getServingStyles());
            menuData.put("items", menuService.getAvailableItems());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "menu", menuData
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "메뉴 관리 데이터 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 재고 업데이트 API
     * PUT /api/admin/inventory/{itemId}
     */
    @PutMapping("/inventory/{itemId}")
    public ResponseEntity<Map<String, Object>> updateInventory(
            @PathVariable String itemId,
            @RequestBody UpdateInventoryRequest request) {
        try {
            boolean success = inventoryService.updateStockQuantity(itemId, request.getNewQuantity());
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "재고가 업데이트되었습니다"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "재고 업데이트 실패"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "재고 업데이트 실패: " + e.getMessage()
            ));
        }
    }
    
    // Request DTOs
    public static class AssignCourierRequest {
        private String courierId;
        
        // Getters and Setters
        public String getCourierId() { return courierId; }
        public void setCourierId(String courierId) { this.courierId = courierId; }
    }
    
    public static class UpdateInventoryRequest {
        private int newQuantity;
        
        // Getters and Setters
        public int getNewQuantity() { return newQuantity; }
        public void setNewQuantity(int newQuantity) { this.newQuantity = newQuantity; }
    }
}
