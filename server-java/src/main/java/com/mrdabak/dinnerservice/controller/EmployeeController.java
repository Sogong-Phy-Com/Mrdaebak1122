package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import com.mrdabak.dinnerservice.service.DeliverySchedulingService;
import com.mrdabak.dinnerservice.service.ExcelExportService;
import com.mrdabak.dinnerservice.service.OrderService;
import com.mrdabak.dinnerservice.service.InventoryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class EmployeeController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final DinnerTypeRepository dinnerTypeRepository;
    private final MenuItemRepository menuItemRepository;
    private final ExcelExportService excelExportService;
    private final DeliverySchedulingService deliverySchedulingService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final com.mrdabak.dinnerservice.repository.DeliveryScheduleRepository deliveryScheduleRepository;

    public EmployeeController(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             UserRepository userRepository, DinnerTypeRepository dinnerTypeRepository,
                             MenuItemRepository menuItemRepository, ExcelExportService excelExportService,
                             DeliverySchedulingService deliverySchedulingService,
                             OrderService orderService,
                             InventoryService inventoryService,
                             com.mrdabak.dinnerservice.repository.DeliveryScheduleRepository deliveryScheduleRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.menuItemRepository = menuItemRepository;
        this.excelExportService = excelExportService;
        this.deliverySchedulingService = deliverySchedulingService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.deliveryScheduleRepository = deliveryScheduleRepository;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders(@RequestParam(required = false) String status) {
        // 당일 배달 예정인 주문만 필터링
        LocalDate today = LocalDate.now();
        List<Order> allOrders;
        if (status != null && !status.isEmpty()) {
            allOrders = orderRepository.findByStatus(status);
        } else {
            allOrders = orderRepository.findAll();
        }
        
        // 당일 배달 예정인 주문만 필터링
        List<Order> orders = allOrders.stream()
            .filter(order -> {
                try {
                    // delivery_time 파싱 (예: "2025-11-21T18:00" 또는 "2025-11-21T18:00:00")
                    String deliveryTimeStr = order.getDeliveryTime();
                    if (deliveryTimeStr == null || deliveryTimeStr.isEmpty()) {
                        return false;
                    }
                    
                    // 날짜 부분만 추출 (T 이전 부분)
                    String datePart = deliveryTimeStr.split("T")[0];
                    LocalDate deliveryDate = LocalDate.parse(datePart);
                    
                    return deliveryDate.equals(today);
                } catch (Exception e) {
                    // 파싱 실패 시 제외
                    return false;
                }
            })
            .toList();

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
            orderMap.put("cooking_employee_id", order.getCookingEmployeeId());
            orderMap.put("delivery_employee_id", order.getDeliveryEmployeeId());
            
            // Add employee names if assigned
            if (order.getCookingEmployeeId() != null) {
                User cookingEmployee = userRepository.findById(order.getCookingEmployeeId()).orElse(null);
                if (cookingEmployee != null) {
                    orderMap.put("cooking_employee_name", cookingEmployee.getName());
                }
            }
            if (order.getDeliveryEmployeeId() != null) {
                User deliveryEmployee = userRepository.findById(order.getDeliveryEmployeeId()).orElse(null);
                if (deliveryEmployee != null) {
                    orderMap.put("delivery_employee_name", deliveryEmployee.getName());
                }
            }

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

    @GetMapping("/delivery-schedule")
    public ResponseEntity<?> getDeliverySchedule(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long employeeId,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }
        
        LocalDate targetDate;
        try {
            targetDate = date != null && !date.trim().isEmpty() 
                    ? LocalDate.parse(date) 
                    : LocalDate.now();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 날짜 형식입니다. (예: 2025-01-15)"));
        }
        
        try {
            Long requesterId = Long.parseLong(authentication.getName());
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

            // 관리자가 특정 직원을 선택한 경우 해당 직원의 스케줄만 반환
            List<com.mrdabak.dinnerservice.model.DeliverySchedule> schedules;
            if (isAdmin && employeeId != null) {
                // 관리자가 특정 직원 선택 시 해당 직원의 스케줄만 조회
                // shift 시간은 application.properties에서 가져오거나 기본값 사용
                java.time.LocalTime shiftStart = java.time.LocalTime.parse("15:00");
                java.time.LocalTime shiftEnd = java.time.LocalTime.parse("22:00");
                java.time.LocalDateTime start = java.time.LocalDateTime.of(targetDate, shiftStart);
                java.time.LocalDateTime end = java.time.LocalDateTime.of(targetDate, shiftEnd);
                schedules = deliveryScheduleRepository.findByEmployeeIdAndDepartureTimeBetween(employeeId, start, end);
            } else {
                // 일반 조회 (관리자는 전체, 직원은 자신의 스케줄)
                schedules = deliverySchedulingService.getSchedulesForUser(requesterId, isAdmin, targetDate);
            }

            List<Map<String, Object>> response = schedules.stream()
                    .map(schedule -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", schedule.getId());
                        map.put("order_id", schedule.getOrderId());
                        map.put("employee_id", schedule.getEmployeeId());
                        map.put("delivery_address", schedule.getDeliveryAddress());
                        map.put("departure_time", schedule.getDepartureTime());
                        map.put("arrival_time", schedule.getArrivalTime());
                        map.put("return_time", schedule.getReturnTime());
                        map.put("one_way_minutes", schedule.getOneWayMinutes());
                        map.put("status", schedule.getStatus());
                        userRepository.findById(schedule.getEmployeeId())
                                .ifPresent(user -> {
                                    map.put("employee_name", user.getName());
                                    map.put("employee_phone", user.getPhone());
                                });
                        return map;
                    }).toList();

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).body(Map.of("error", "유효하지 않은 사용자 ID입니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "배달 스케줄 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PatchMapping("/delivery-schedule/{id}/status")
    public ResponseEntity<?> updateDeliveryStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, String> request,
                                                  Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }
        
        try {
            Long requesterId = Long.parseLong(authentication.getName());
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "상태 값은 필수입니다."));
            }

            DeliverySchedule updated = deliverySchedulingService.updateStatus(id, status, requesterId, isAdmin);
            return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "status", updated.getStatus()
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).body(Map.of("error", "유효하지 않은 사용자 ID입니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "배달 스케줄 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, 
                                                @RequestBody Map<String, String> request,
                                                Authentication authentication) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "주문 ID는 필수입니다."));
            }

            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
            }

            Long employeeId = Long.parseLong(authentication.getName());
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "상태 값은 필수입니다."));
            }

            status = status.trim().toLowerCase();
            if (!List.of("pending", "cooking", "ready", "out_for_delivery", "delivered", "cancelled").contains(status)) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 상태입니다. (pending, cooking, ready, out_for_delivery, delivered, cancelled 중 하나여야 합니다.)"));
            }

            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + id));

            // 권한 체크: 관리자가 아니면 할당된 직원만 상태 변경 가능
            if (!isAdmin) {
                if ("cooking".equals(status) || "ready".equals(status)) {
                    // 조리 관련 상태는 조리 직원만 변경 가능
                    if (order.getCookingEmployeeId() == null || !order.getCookingEmployeeId().equals(employeeId)) {
                        return ResponseEntity.status(403).body(Map.of("error", "이 주문의 조리 담당 직원만 상태를 변경할 수 있습니다."));
                    }
                } else if ("out_for_delivery".equals(status)) {
                    // 배달 중 상태는 배달 직원만 변경 가능
                    if (order.getDeliveryEmployeeId() == null || !order.getDeliveryEmployeeId().equals(employeeId)) {
                        return ResponseEntity.status(403).body(Map.of("error", "이 주문의 배달 담당 직원만 상태를 변경할 수 있습니다."));
                    }
                }
            }

            // Prevent invalid status transitions
            if ("cancelled".equals(order.getStatus()) && !"cancelled".equals(status)) {
                return ResponseEntity.badRequest().body(Map.of("error", "취소된 주문의 상태를 변경할 수 없습니다."));
            }
            if ("delivered".equals(order.getStatus()) && !"delivered".equals(status)) {
                return ResponseEntity.badRequest().body(Map.of("error", "배달 완료된 주문의 상태를 변경할 수 없습니다."));
            }

            // If status is being changed to cooking, consume inventory (조리 시작 시 재고 소진)
            if ("cooking".equals(status) && !"cooking".equals(order.getStatus())) {
                try {
                    inventoryService.consumeReservationsForOrder(id);
                } catch (Exception e) {
                    return ResponseEntity.status(500).body(Map.of("error", "재고 소진 처리 중 오류가 발생했습니다: " + e.getMessage()));
                }
            }
            
            // If status is being changed to delivered, consume inventory
            if ("delivered".equals(status) && !"delivered".equals(order.getStatus())) {
                try {
                    orderService.markOrderAsDelivered(id);
                    return ResponseEntity.ok(Map.of("message", "주문이 배달 완료로 처리되었습니다."));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                } catch (RuntimeException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                } catch (Exception e) {
                    return ResponseEntity.status(500).body(Map.of("error", "배달 완료 처리 중 오류가 발생했습니다: " + e.getMessage()));
                }
            }

            // For other status changes, just update the status
            order.setStatus(status);
            orderRepository.save(order);

            return ResponseEntity.ok(Map.of("message", "주문 상태가 업데이트되었습니다.", "status", status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "주문 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Authentication authentication) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "주문 ID는 필수입니다."));
            }

            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
            }

            Long adminId;
            try {
                adminId = Long.parseLong(authentication.getName());
            } catch (NumberFormatException e) {
                return ResponseEntity.status(401).body(Map.of("error", "유효하지 않은 사용자 ID입니다."));
            }

            Order cancelledOrder = orderService.cancelOrder(id, adminId);

            return ResponseEntity.ok(Map.of(
                    "message", "주문이 취소되었습니다. 재고 예약과 배달 스케줄도 함께 취소되었습니다.",
                    "order_id", cancelledOrder.getId(),
                    "status", cancelledOrder.getStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "주문 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrdersToExcel(@RequestParam(required = false) String status) {
        try {
            byte[] excelData = excelExportService.exportOrdersToExcel(status);
            
            String filename = "orders_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
