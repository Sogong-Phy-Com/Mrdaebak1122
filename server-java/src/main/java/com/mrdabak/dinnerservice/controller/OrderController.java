package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.dto.OrderRequest;
import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.model.Order;
import com.mrdabak.dinnerservice.model.OrderItem;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import com.mrdabak.dinnerservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    // 중복 주문 생성 방지를 위한 임시 저장소 (요청 ID 기반)
    private final java.util.concurrent.ConcurrentHashMap<String, Long> pendingOrders = new java.util.concurrent.ConcurrentHashMap<>();

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderController(OrderService orderService, OrderItemRepository orderItemRepository,
                          MenuItemRepository menuItemRepository) {
        this.orderService = orderService;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUserOrders(Authentication authentication) {
        System.out.println("[주문 목록 조회 API] 요청 시작");
        
        try {
            // 1단계: 인증 정보 확인
            if (authentication == null) {
                System.out.println("[에러 1] Authentication 객체가 null입니다.");
                return ResponseEntity.status(401).body(List.of(Map.of("error", "Authentication is null")));
            }
            
            String authName = authentication.getName();
            System.out.println("[1단계] Authentication.getName(): " + authName);
            
            if (authName == null || authName.isEmpty()) {
                System.out.println("[에러 2] Authentication.getName()이 null이거나 비어있습니다.");
                return ResponseEntity.status(401).body(List.of(Map.of("error", "User ID is null or empty")));
            }
            
            // 2단계: 사용자 ID 파싱
            Long userId;
            try {
                userId = Long.parseLong(authName);
                System.out.println("[2단계] 사용자 ID 파싱 성공: " + userId);
            } catch (NumberFormatException e) {
                System.out.println("[에러 3] 사용자 ID 파싱 실패: " + authName);
                System.out.println("[에러 3] 예외: " + e.getMessage());
                return ResponseEntity.status(401).body(List.of(Map.of("error", "Invalid user ID format: " + authName)));
            }
            
            // 3단계: 주문 조회
            System.out.println("[3단계] 주문 조회 시작 (사용자 ID: " + userId + ")");
            List<Order> orders = orderService.getUserOrders(userId);
            System.out.println("[3단계] 주문 조회 완료: " + orders.size() + "개 주문 발견");
            
            // 4단계: 주문 데이터 변환
            System.out.println("[4단계] 주문 데이터 변환 시작");
            List<Map<String, Object>> orderDtos = orders.stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("dinner_type_id", order.getDinnerTypeId());
            orderMap.put("serving_style", order.getServingStyle());
            orderMap.put("delivery_time", order.getDeliveryTime());
            orderMap.put("delivery_address", order.getDeliveryAddress());
            orderMap.put("total_price", order.getTotalPrice());
            orderMap.put("status", order.getStatus());
            orderMap.put("payment_status", order.getPaymentStatus());
            orderMap.put("created_at", order.getCreatedAt());
            orderMap.put("admin_approval_status", order.getAdminApprovalStatus());

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
        
        System.out.println("[4단계] 주문 데이터 변환 완료: " + orderDtos.size() + "개");
        System.out.println("[성공] 주문 목록 조회 API 완료");
        
        return ResponseEntity.ok(orderDtos);
        } catch (Exception e) {
            System.out.println("[에러 4] 예상치 못한 오류 발생");
            System.out.println("[에러 4] 예외 타입: " + e.getClass().getName());
            System.out.println("[에러 4] 예외 메시지: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Internal server error: " + e.getMessage())));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOrderStats(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            Long userId = Long.parseLong(authentication.getName());
            List<Order> orders = orderService.getUserOrders(userId);
            
            long totalOrders = orders.size();
            long deliveredOrders = orders.stream().filter(o -> "delivered".equals(o.getStatus())).count();
            long pendingOrders = orders.stream().filter(o -> 
                "pending".equals(o.getStatus()) || 
                "cooking".equals(o.getStatus()) || 
                "ready".equals(o.getStatus()) || 
                "out_for_delivery".equals(o.getStatus())
            ).count();
            
            return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "deliveredOrders", deliveredOrders,
                "pendingOrders", pendingOrders
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public synchronized ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderRequest request, 
            Authentication authentication,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        String threadId = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        System.out.println("========== [주문 생성 API] 요청 시작 ==========");
        System.out.println("[주문 생성 API] 스레드: " + threadId);
        System.out.println("[주문 생성 API] Request ID: " + (requestId != null ? requestId : "없음"));
        System.out.println("[주문 생성 API] Authentication 객체: " + (authentication != null ? "존재" : "null"));
        
        // SecurityContext에서 직접 확인
        org.springframework.security.core.Authentication contextAuth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("[주문 생성 API] SecurityContext 인증: " + (contextAuth != null ? "존재" : "null"));
        if (contextAuth != null) {
            System.out.println("[주문 생성 API] SecurityContext 사용자: " + contextAuth.getName());
            System.out.println("[주문 생성 API] SecurityContext 권한: " + contextAuth.getAuthorities());
        }
        
        try {
            // authentication 파라미터가 null이면 SecurityContext에서 가져오기 시도
            if (authentication == null) {
                System.out.println("[주문 생성 API] 경고: Authentication 파라미터가 null입니다. SecurityContext에서 확인합니다.");
                authentication = contextAuth;
            }
            
            if (authentication == null) {
                System.out.println("[주문 생성 API] 에러: Authentication 객체가 null입니다.");
                System.out.println("==========================================");
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            
            if (authentication.getName() == null || authentication.getName().isEmpty()) {
                System.out.println("[주문 생성 API] 에러: Authentication.getName()이 null이거나 비어있습니다.");
                System.out.println("[주문 생성 API] Authentication 정보: " + authentication);
                System.out.println("[주문 생성 API] Authentication 권한: " + authentication.getAuthorities());
                System.out.println("==========================================");
                return ResponseEntity.status(401).body(Map.of("error", "User ID not found in authentication"));
            }
            
            System.out.println("[주문 생성 API] 인증된 사용자: " + authentication.getName());
            System.out.println("[주문 생성 API] 인증 권한: " + authentication.getAuthorities());
            
            Long userId = Long.parseLong(authentication.getName());
            
            // Validate request
            if (request.getDinnerTypeId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Dinner type is required"));
            }
            if (request.getServingStyle() == null || request.getServingStyle().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Serving style is required"));
            }
            if (request.getDeliveryTime() == null || request.getDeliveryTime().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Delivery time is required"));
            }
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Delivery address is required"));
            }
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order items are required"));
            }
            
            // 중복 주문 생성 방지: Request ID 또는 동일한 요청이 30초 이내에 들어오면 거부
            long currentTime = System.currentTimeMillis();
            String requestKey;
            
            if (requestId != null && !requestId.trim().isEmpty()) {
                // Request ID가 있으면 이를 사용 (프론트엔드에서 전송한 고유 ID)
                requestKey = userId + "|" + requestId.trim();
                System.out.println("[주문 생성 API] Request ID 사용: " + requestId);
            } else {
                // Request ID가 없으면 기존 방식 사용 (타임스탬프 포함)
                requestKey = userId + "|" + request.getDeliveryTime() + "|" + request.getDeliveryAddress() + "|" + (currentTime / 30000); // 30초 단위로 그룹화
                System.out.println("[주문 생성 API] Request ID 없음, 타임스탬프 기반 키 사용: " + requestKey);
            }
            
            Long existingOrderId = pendingOrders.get(requestKey);
            if (existingOrderId != null) {
                System.out.println("[주문 생성 API] 중복 요청 감지 - 요청 키: " + requestKey + ", 기존 주문 ID: " + existingOrderId);
                System.out.println("[주문 생성 API] 현재 시간: " + currentTime);
                return ResponseEntity.status(409).body(Map.of(
                        "error", "동일한 주문이 이미 처리 중입니다.",
                        "order_id", existingOrderId
                ));
            }
            
            // 추가 검증: 최근 30초 이내에 동일한 사용자가 동일한 주문을 생성했는지 확인
            String baseRequestKey = userId + "|" + request.getDeliveryTime() + "|" + request.getDeliveryAddress();
            for (Map.Entry<String, Long> entry : pendingOrders.entrySet()) {
                if (entry.getKey().startsWith(baseRequestKey + "|")) {
                    System.out.println("[주문 생성 API] 중복 요청 감지 (기본 키) - 요청 키: " + entry.getKey() + ", 기존 주문 ID: " + entry.getValue());
                    return ResponseEntity.status(409).body(Map.of(
                            "error", "동일한 주문이 이미 처리 중입니다.",
                            "order_id", entry.getValue()
                    ));
                }
            }
            
            System.out.println("[주문 생성 API] 주문 서비스 호출 전 - 사용자 ID: " + userId);
            System.out.println("[주문 생성 API] 스레드: " + threadId);
            System.out.println("[주문 생성 API] Request ID: " + (requestId != null ? requestId : "없음"));
            System.out.println("[주문 생성 API] 배달 시간: " + request.getDeliveryTime());
            System.out.println("[주문 생성 API] 배달 주소: " + request.getDeliveryAddress());
            
            Order order = orderService.createOrder(userId, request);
            
            System.out.println("[주문 생성 API] 주문 서비스 호출 완료 - 주문 ID: " + order.getId());
            System.out.println("[주문 생성 API] 스레드: " + threadId);
            System.out.println("[주문 생성 API] Request ID: " + (requestId != null ? requestId : "없음"));
            System.out.println("[주문 생성 API] 주문은 1개만 생성되었습니다.");
            
            // 주문 생성 완료 후 30초 후에 임시 저장소에서 제거
            pendingOrders.put(requestKey, order.getId());
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    pendingOrders.remove(requestKey);
                    System.out.println("[주문 생성 API] 중복 방지 키 제거: " + requestKey);
                }
            }, 30000);
            
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Order created successfully",
                    "order_id", order.getId(),
                    "total_price", order.getTotalPrice()
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid user ID"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/modify")
    public ResponseEntity<?> modifyOrder(@PathVariable Long orderId, 
                                        @Valid @RequestBody OrderRequest request, 
                                        Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            Long userId = Long.parseLong(authentication.getName());
            Order modifiedOrder = orderService.modifyOrder(orderId, userId, request);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Order modified successfully",
                    "order_id", modifiedOrder.getId(),
                    "total_price", modifiedOrder.getTotalPrice()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            Long userId = Long.parseLong(authentication.getName());
            Order cancelledOrder = orderService.cancelOrder(orderId, userId);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Order cancelled successfully",
                    "order_id", cancelledOrder.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}

