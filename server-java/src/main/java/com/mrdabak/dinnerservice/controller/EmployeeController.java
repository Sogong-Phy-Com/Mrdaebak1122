package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import com.mrdabak.dinnerservice.repository.schedule.EmployeeWorkAssignmentRepository;
import com.mrdabak.dinnerservice.model.EmployeeWorkAssignment;
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
    private final com.mrdabak.dinnerservice.repository.schedule.DeliveryScheduleRepository deliveryScheduleRepository;
    private final EmployeeWorkAssignmentRepository employeeWorkAssignmentRepository;

    public EmployeeController(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             UserRepository userRepository, DinnerTypeRepository dinnerTypeRepository,
                             MenuItemRepository menuItemRepository, ExcelExportService excelExportService,
                             DeliverySchedulingService deliverySchedulingService,
                             OrderService orderService,
                             InventoryService inventoryService,
                             com.mrdabak.dinnerservice.repository.schedule.DeliveryScheduleRepository deliveryScheduleRepository,
                             EmployeeWorkAssignmentRepository employeeWorkAssignmentRepository) {
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
        this.employeeWorkAssignmentRepository = employeeWorkAssignmentRepository;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders(@RequestParam(required = false) String status) {
        // Get all orders (no date filtering)
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

            // If admin selects specific employee, return only that employee's schedule
            List<com.mrdabak.dinnerservice.model.DeliverySchedule> schedules;
            if (isAdmin && employeeId != null) {
                // Admin selected specific employee - return only that employee's schedule
                // Shift times from application.properties or use default values
                java.time.LocalTime shiftStart = java.time.LocalTime.parse("15:00");
                java.time.LocalTime shiftEnd = java.time.LocalTime.parse("22:00");
                java.time.LocalDateTime start = java.time.LocalDateTime.of(targetDate, shiftStart);
                java.time.LocalDateTime end = java.time.LocalDateTime.of(targetDate, shiftEnd);
                schedules = deliveryScheduleRepository.findByEmployeeIdAndDepartureTimeBetween(employeeId, start, end);
            } else {
                // General query (admin sees all, employee sees own schedule)
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

            // 권한 체크: 관리자가 아니면 관리자가 할당한 작업에 해당하는 직원만 상태 변경 가능
            if (!isAdmin) {
                // 주문의 배달 시간에서 날짜 추출
                LocalDate orderDate = null;
                try {
                    LocalDateTime deliveryDateTime = LocalDateTime.parse(order.getDeliveryTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    orderDate = deliveryDateTime.toLocalDate();
                } catch (Exception e) {
                    try {
                        // 다른 형식 시도
                        orderDate = LocalDate.parse(order.getDeliveryTime().split("T")[0]);
                    } catch (Exception e2) {
                        return ResponseEntity.status(400).body(Map.of("error", "주문의 배달 시간 형식이 올바르지 않습니다."));
                    }
                }
                
                // 해당 날짜에 관리자가 할당한 작업 확인
                List<EmployeeWorkAssignment> assignments = employeeWorkAssignmentRepository.findByEmployeeIdAndWorkDate(employeeId, orderDate);
                
                boolean hasCookingAssignment = assignments.stream()
                    .anyMatch(a -> "COOKING".equalsIgnoreCase(a.getTaskType()));
                boolean hasDeliveryAssignment = assignments.stream()
                    .anyMatch(a -> "DELIVERY".equalsIgnoreCase(a.getTaskType()));
                
                if ("cooking".equals(status) || "ready".equals(status)) {
                    // 조리 관련 상태는 조리 작업이 할당된 직원만 변경 가능
                    if (!hasCookingAssignment) {
                        return ResponseEntity.status(403).body(Map.of("error", "이 주문의 조리 담당 직원만 상태를 변경할 수 있습니다."));
                    }
                } else if ("out_for_delivery".equals(status) || "delivered".equals(status)) {
                    // 배달 관련 상태는 배달 작업이 할당된 직원만 변경 가능
                    if (!hasDeliveryAssignment) {
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

    @GetMapping("/schedule/assignments")
    public ResponseEntity<?> getEmployeeScheduleAssignments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }
        
        try {
            Long employeeId = Long.parseLong(authentication.getName());
            
            // 날짜 범위로 조회하는 경우 (월 전체 할당 조회)
            if (startDate != null && endDate != null && !startDate.trim().isEmpty() && !endDate.trim().isEmpty()) {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                
                List<EmployeeWorkAssignment> assignments = employeeWorkAssignmentRepository.findByEmployeeIdAndWorkDateBetween(employeeId, start, end);
                
                Map<String, Map<String, Object>> assignmentsByDate = new HashMap<>();
                for (EmployeeWorkAssignment assignment : assignments) {
                    String dateStr = assignment.getWorkDate().toString();
                    if (!assignmentsByDate.containsKey(dateStr)) {
                        assignmentsByDate.put(dateStr, new HashMap<>());
                        assignmentsByDate.get(dateStr).put("date", dateStr);
                        assignmentsByDate.get(dateStr).put("isWorking", true);
                        assignmentsByDate.get(dateStr).put("tasks", new java.util.ArrayList<String>());
                    }
                    @SuppressWarnings("unchecked")
                    List<String> tasks = (List<String>) assignmentsByDate.get(dateStr).get("tasks");
                    if ("COOKING".equals(assignment.getTaskType())) {
                        if (!tasks.contains("조리")) {
                            tasks.add("조리");
                        }
                    } else if ("DELIVERY".equals(assignment.getTaskType())) {
                        if (!tasks.contains("배달")) {
                            tasks.add("배달");
                        }
                    }
                }
                
                return ResponseEntity.ok(assignmentsByDate);
            }
            
            // 단일 날짜로 조회하는 경우
            java.time.LocalDate targetDate;
            if (date != null && !date.trim().isEmpty()) {
                targetDate = java.time.LocalDate.parse(date);
            } else {
                targetDate = java.time.LocalDate.now();
            }
            
            // 스케줄 데이터베이스에서 직원 할당 조회
            List<EmployeeWorkAssignment> assignments = employeeWorkAssignmentRepository.findByEmployeeIdAndWorkDate(employeeId, targetDate);
            
            List<String> tasks = new java.util.ArrayList<>();
            for (EmployeeWorkAssignment assignment : assignments) {
                if ("COOKING".equals(assignment.getTaskType())) {
                    tasks.add("조리");
                } else if ("DELIVERY".equals(assignment.getTaskType())) {
                    tasks.add("배달");
                }
            }
            
            // 출근 여부 확인 (할당된 작업이 있으면 출근)
            boolean isWorking = !assignments.isEmpty();
            
            Map<String, Object> response = new HashMap<>();
            response.put("date", targetDate.toString());
            response.put("isWorking", isWorking);
            response.put("tasks", tasks);
            response.put("orderCount", isWorking ? 1 : 0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "스케줄 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
