package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.dto.AuthRequest;
import com.mrdabak.dinnerservice.dto.AuthResponse;
import com.mrdabak.dinnerservice.dto.UserDto;
import com.mrdabak.dinnerservice.model.User;
import com.mrdabak.dinnerservice.repository.UserRepository;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import com.mrdabak.dinnerservice.model.Order;
import com.mrdabak.dinnerservice.service.JwtService;
import com.mrdabak.dinnerservice.service.DeliverySchedulingService;
import com.mrdabak.dinnerservice.service.TravelTimeEstimator;
import com.mrdabak.dinnerservice.service.OrderService;
import com.mrdabak.dinnerservice.model.DeliverySchedule;
import com.mrdabak.dinnerservice.repository.DeliveryScheduleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OrderRepository orderRepository;
    private final DeliverySchedulingService deliverySchedulingService;
    private final DeliveryScheduleRepository deliveryScheduleRepository;
    private final TravelTimeEstimator travelTimeEstimator;
    private final OrderService orderService;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          JwtService jwtService, OrderRepository orderRepository,
                          DeliverySchedulingService deliverySchedulingService,
                          DeliveryScheduleRepository deliveryScheduleRepository,
                          TravelTimeEstimator travelTimeEstimator,
                          OrderService orderService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.orderRepository = orderRepository;
        this.deliverySchedulingService = deliverySchedulingService;
        this.deliveryScheduleRepository = deliveryScheduleRepository;
        this.travelTimeEstimator = travelTimeEstimator;
        this.orderService = orderService;
    }

    @PostMapping("/create-employee")
    public ResponseEntity<?> createEmployee(@Valid @RequestBody AuthRequest request, Authentication authentication) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "User already exists"));
            }

            User employee = new User();
            employee.setEmail(request.getEmail());
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
            employee.setName(request.getName());
            employee.setAddress(request.getAddress() != null ? request.getAddress() : "");
            employee.setPhone(request.getPhone() != null ? request.getPhone() : "");
            employee.setRole("employee");

            User savedEmployee = userRepository.save(employee);
            String token = jwtService.generateToken(savedEmployee.getId(), savedEmployee.getEmail(), savedEmployee.getRole());

            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                    "Employee created successfully",
                    token,
                    new UserDto(savedEmployee.getId(), savedEmployee.getEmail(), savedEmployee.getName(),
                            savedEmployee.getAddress(), savedEmployee.getPhone(), savedEmployee.getRole(), savedEmployee.getApprovalStatus())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(user -> {
                    UserDto dto = new UserDto(user.getId(), user.getEmail(), user.getName(),
                            user.getAddress(), user.getPhone(), user.getRole(), user.getApprovalStatus());
                    if (user.getEmployeeType() != null) {
                        dto.setEmployeeType(user.getEmployeeType());
                    }
                    return dto;
                })
                .toList());
    }

    @GetMapping("/employees")
    public ResponseEntity<?> getEmployees() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(user -> "employee".equals(user.getRole()))
                .map(user -> {
                    UserDto dto = new UserDto(user.getId(), user.getEmail(), user.getName(),
                            user.getAddress(), user.getPhone(), user.getRole(), user.getApprovalStatus());
                    if (user.getEmployeeType() != null) {
                        dto.setEmployeeType(user.getEmployeeType());
                    }
                    return dto;
                })
                .toList());
    }
    
    @PatchMapping("/employees/{employeeId}/type")
    public ResponseEntity<?> updateEmployeeType(@PathVariable Long employeeId, @RequestBody Map<String, String> request) {
        try {
            User employee = userRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            
            if (!"employee".equals(employee.getRole()) && !"admin".equals(employee.getRole())) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not an employee or admin"));
            }
            
            String employeeType = request.get("employeeType");
            if (employeeType != null && !employeeType.isEmpty()) {
                if (!"cooking".equals(employeeType) && !"delivery".equals(employeeType)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid employee type. Must be 'cooking' or 'delivery'"));
                }
                employee.setEmployeeType(employeeType);
                userRepository.save(employee);
            } else {
                // Remove employee type if null or empty
                employee.setEmployeeType(null);
                userRepository.save(employee);
            }
            
            UserDto dto = new UserDto(employee.getId(), employee.getEmail(), employee.getName(),
                    employee.getAddress(), employee.getPhone(), employee.getRole(), employee.getApprovalStatus());
            if (employee.getEmployeeType() != null) {
                dto.setEmployeeType(employee.getEmployeeType());
            }
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/customers")
    public ResponseEntity<?> getCustomers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(user -> "customer".equals(user.getRole()))
                .map(user -> new UserDto(user.getId(), user.getEmail(), user.getName(),
                        user.getAddress(), user.getPhone(), user.getRole(), user.getApprovalStatus()))
                .toList());
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<?> getPendingApprovals() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(user -> "pending".equals(user.getApprovalStatus()))
                .map(user -> Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "phone", user.getPhone(),
                        "address", user.getAddress(),
                        "role", user.getRole(),
                        "approvalStatus", user.getApprovalStatus(),
                        "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
                ))
                .toList());
    }

    @PostMapping("/approve-user/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"pending".equals(user.getApprovalStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not pending approval"));
            }
            
            user.setApprovalStatus("approved");
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                    "message", "User approved successfully",
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "name", user.getName(),
                            "role", user.getRole(),
                            "approvalStatus", user.getApprovalStatus()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reject-user/{userId}")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"pending".equals(user.getApprovalStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not pending approval"));
            }
            
            user.setApprovalStatus("rejected");
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                    "message", "User rejected successfully",
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "name", user.getName(),
                            "role", user.getRole(),
                            "approvalStatus", user.getApprovalStatus()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/orders/{orderId}/assign")
    public ResponseEntity<?> assignOrderEmployees(
            @PathVariable Long orderId,
            @RequestBody Map<String, Long> request) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            Long cookingEmployeeId = request.get("cookingEmployeeId");
            Long deliveryEmployeeId = request.get("deliveryEmployeeId");
            
            // Validate employees exist and are employees
            if (cookingEmployeeId != null) {
                User cookingEmployee = userRepository.findById(cookingEmployeeId)
                        .orElseThrow(() -> new RuntimeException("Cooking employee not found"));
                if (!"employee".equals(cookingEmployee.getRole()) && !"admin".equals(cookingEmployee.getRole())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Cooking employee must be an employee or admin"));
                }
                if (!"approved".equals(cookingEmployee.getApprovalStatus())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Cooking employee is not approved"));
                }
            }
            
            if (deliveryEmployeeId != null) {
                User deliveryEmployee = userRepository.findById(deliveryEmployeeId)
                        .orElseThrow(() -> new RuntimeException("Delivery employee not found"));
                if (!"employee".equals(deliveryEmployee.getRole()) && !"admin".equals(deliveryEmployee.getRole())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Delivery employee must be an employee or admin"));
                }
                if (!"approved".equals(deliveryEmployee.getApprovalStatus())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Delivery employee is not approved"));
                }
            }
            
            // 기존 배달 스케줄 확인
            java.util.Optional<DeliverySchedule> existingSchedule = deliveryScheduleRepository.findByOrderId(orderId);
            
            // 배달 직원이 변경되면 기존 스케줄 삭제
            if (existingSchedule.isPresent() && 
                order.getDeliveryEmployeeId() != null && 
                !order.getDeliveryEmployeeId().equals(deliveryEmployeeId)) {
                deliverySchedulingService.releaseAssignmentForOrder(orderId);
                existingSchedule = java.util.Optional.empty();
            }
            
            order.setCookingEmployeeId(cookingEmployeeId);
            order.setDeliveryEmployeeId(deliveryEmployeeId);
            orderRepository.save(order);
            
            // 배달 직원이 배당되면 DeliverySchedule 생성 또는 업데이트
            if (deliveryEmployeeId != null && order.getDeliveryTime() != null && order.getDeliveryAddress() != null) {
                try {
                    System.out.println("[AdminController] 배달 스케줄 생성 시작 - 주문 ID: " + orderId + ", 배달 시간: " + order.getDeliveryTime());
                    
                    // deliveryTime 파싱 (OrderService와 동일한 로직 사용)
                    java.time.LocalDateTime deliveryDateTime = null;
                    java.time.format.DateTimeFormatter[] formatters = {
                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME,  // "2025-11-21T10:00:00"
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")  // "2025-11-21T10:00"
                    };
                    
                    boolean parsed = false;
                    for (java.time.format.DateTimeFormatter formatter : formatters) {
                        try {
                            deliveryDateTime = java.time.LocalDateTime.parse(order.getDeliveryTime(), formatter);
                            parsed = true;
                            System.out.println("[AdminController] 배달 시간 파싱 성공: " + deliveryDateTime);
                            break;
                        } catch (java.time.format.DateTimeParseException e) {
                            // 다음 포맷 시도
                        }
                    }
                    
                    if (!parsed || deliveryDateTime == null) {
                        throw new RuntimeException("잘못된 배달 시간 형식입니다: " + order.getDeliveryTime());
                    }
                    
                    // TravelTimeEstimator를 사용하여 소요 시간 계산
                    int oneWayMinutes = travelTimeEstimator.estimateOneWayMinutes(order.getDeliveryAddress(), deliveryDateTime);
                    
                    // 출발 시간과 복귀 시간 계산
                    java.time.LocalDateTime departureTime = deliveryDateTime.minusMinutes(oneWayMinutes);
                    java.time.LocalDateTime returnTime = deliveryDateTime.plusMinutes(oneWayMinutes);
                    
                    // 기존 스케줄이 있으면 업데이트, 없으면 생성
                    DeliverySchedule schedule;
                    if (existingSchedule.isPresent()) {
                        schedule = existingSchedule.get();
                        // 기존 스케줄 업데이트
                        schedule.setEmployeeId(deliveryEmployeeId);
                        schedule.setDeliveryAddress(order.getDeliveryAddress());
                        schedule.setDepartureTime(departureTime);
                        schedule.setArrivalTime(deliveryDateTime);
                        schedule.setReturnTime(returnTime);
                        schedule.setOneWayMinutes(oneWayMinutes);
                        if (!"CANCELLED".equals(schedule.getStatus())) {
                            schedule.setStatus("SCHEDULED");
                        }
                        System.out.println("[AdminController] 기존 배달 스케줄 업데이트: 주문 ID " + orderId + ", 직원 ID " + deliveryEmployeeId);
                    } else {
                        // 새 스케줄 생성
                        schedule = new DeliverySchedule();
                        schedule.setOrderId(orderId);
                        schedule.setEmployeeId(deliveryEmployeeId);
                        schedule.setDeliveryAddress(order.getDeliveryAddress());
                        schedule.setDepartureTime(departureTime);
                        schedule.setArrivalTime(deliveryDateTime);
                        schedule.setReturnTime(returnTime);
                        schedule.setOneWayMinutes(oneWayMinutes);
                        schedule.setStatus("SCHEDULED");
                        System.out.println("[AdminController] 새 배달 스케줄 생성: 주문 ID " + orderId + ", 직원 ID " + deliveryEmployeeId);
                    }
                    
                    DeliverySchedule savedSchedule = deliveryScheduleRepository.save(schedule);
                    System.out.println("[AdminController] 배달 스케줄 저장 완료: 스케줄 ID " + savedSchedule.getId());
                } catch (Exception e) {
                    // 스케줄 생성 실패는 경고만 하고 배당은 성공으로 처리
                    System.err.println("[AdminController] 배달 스케줄 생성/업데이트 실패 - 주문 ID: " + orderId);
                    System.err.println("[AdminController] 오류 메시지: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (deliveryEmployeeId == null && existingSchedule.isPresent()) {
                // 배달 직원 배당이 해제되면 스케줄 삭제
                deliverySchedulingService.releaseAssignmentForOrder(orderId);
                System.out.println("[AdminController] 배달 직원 배당 해제로 인한 스케줄 삭제: 주문 ID " + orderId);
            }
            
            return ResponseEntity.ok(Map.of(
                    "message", "Employees assigned successfully",
                    "orderId", order.getId(),
                    "cookingEmployeeId", cookingEmployeeId != null ? cookingEmployeeId : "null",
                    "deliveryEmployeeId", deliveryEmployeeId != null ? deliveryEmployeeId : "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/users/{userId}/promote")
    public ResponseEntity<?> promoteToAdmin(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"employee".equals(user.getRole())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only employees can be promoted to admin"));
            }
            
            user.setRole("admin");
            userRepository.save(user);
            
            UserDto dto = new UserDto(user.getId(), user.getEmail(), user.getName(),
                    user.getAddress(), user.getPhone(), user.getRole(), user.getApprovalStatus());
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/schedule/assign")
    @org.springframework.transaction.annotation.Transactional(transactionManager = "orderTransactionManager")
    public ResponseEntity<?> assignEmployeesForDate(@RequestBody Map<String, Object> request) {
        try {
            String dateStr = (String) request.get("date");
            @SuppressWarnings("unchecked")
            List<Integer> cookingEmployees = (List<Integer>) request.get("cookingEmployees");
            @SuppressWarnings("unchecked")
            List<Integer> deliveryEmployees = (List<Integer>) request.get("deliveryEmployees");

            if (dateStr == null || cookingEmployees == null || deliveryEmployees == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "날짜와 직원 목록이 필요합니다."));
            }

            String datePattern = dateStr; // "2025-11-22"
            
            // 해당 날짜의 모든 주문 조회 (deliveryTime이 String이므로 LIKE로 검색)
            List<Order> ordersForDate = orderRepository.findByDeliveryTimeStartingWith(datePattern);

            // Round-robin 방식으로 직원 할당
            int cookingIndex = 0;
            int deliveryIndex = 0;
            int savedCount = 0;

            for (Order order : ordersForDate) {
                boolean modified = false;
                if (!cookingEmployees.isEmpty()) {
                    Long cookingEmployeeId = Long.valueOf(cookingEmployees.get(cookingIndex % cookingEmployees.size()));
                    order.setCookingEmployeeId(cookingEmployeeId);
                    cookingIndex++;
                    modified = true;
                }
                if (!deliveryEmployees.isEmpty()) {
                    Long deliveryEmployeeId = Long.valueOf(deliveryEmployees.get(deliveryIndex % deliveryEmployees.size()));
                    order.setDeliveryEmployeeId(deliveryEmployeeId);
                    deliveryIndex++;
                    modified = true;
                }
                if (modified) {
                    Order saved = orderRepository.save(order);
                    savedCount++;
                    System.out.println("[AdminController] 주문 " + saved.getId() + "에 직원 할당 저장 완료 - 조리: " + 
                        saved.getCookingEmployeeId() + ", 배달: " + saved.getDeliveryEmployeeId());
                }

                // 배달 직원이 배당되면 DeliverySchedule 생성 또는 업데이트
                if (order.getDeliveryEmployeeId() != null && order.getDeliveryTime() != null && order.getDeliveryAddress() != null) {
                    try {
                        java.time.LocalDateTime deliveryDateTime = null;
                        java.time.format.DateTimeFormatter[] formatters = {
                            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                        };
                        
                        for (java.time.format.DateTimeFormatter formatter : formatters) {
                            try {
                                deliveryDateTime = java.time.LocalDateTime.parse(order.getDeliveryTime(), formatter);
                                break;
                            } catch (java.time.format.DateTimeParseException e) {
                                // Try next formatter
                            }
                        }
                        
                        if (deliveryDateTime != null) {
                            deliverySchedulingService.commitAssignmentForOrder(order.getId(), order.getDeliveryEmployeeId(), 
                                deliveryDateTime, order.getDeliveryAddress());
                        }
                    } catch (Exception e) {
                        System.err.println("[AdminController] 배달 스케줄 생성 실패: " + e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "직원 할당이 저장되었습니다.", 
                "assignedOrders", ordersForDate.size(),
                "savedOrders", savedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "직원 할당 저장 실패: " + e.getMessage()));
        }
    }
}

