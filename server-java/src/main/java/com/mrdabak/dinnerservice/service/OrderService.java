package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.dto.OrderItemDto;
import com.mrdabak.dinnerservice.dto.OrderRequest;
import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import com.mrdabak.dinnerservice.repository.order.OrderRepository;
import com.mrdabak.dinnerservice.repository.order.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DinnerTypeRepository dinnerTypeRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryService inventoryService;
    private final DeliverySchedulingService deliverySchedulingService;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                       DinnerTypeRepository dinnerTypeRepository, MenuItemRepository menuItemRepository,
                       InventoryService inventoryService, DeliverySchedulingService deliverySchedulingService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.menuItemRepository = menuItemRepository;
        this.inventoryService = inventoryService;
        this.deliverySchedulingService = deliverySchedulingService;
    }

    public Order createOrder(Long userId, OrderRequest request) {
        int maxRetries = 10;
        int retryCount = 0;
        long baseDelay = 100; // Start with 100ms
        
        while (retryCount < maxRetries) {
            try {
                return createOrderWithTransaction(userId, request);
            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                String causeMessage = "";
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    causeMessage = e.getCause().getMessage().toLowerCase();
                }
                
                // Check for various SQLite lock errors
                boolean isLocked = errorMessage.contains("database is locked") 
                    || errorMessage.contains("sqlite_busy")
                    || errorMessage.contains("sqlite_busy_snapshot")
                    || causeMessage.contains("database is locked")
                    || causeMessage.contains("sqlite_busy")
                    || causeMessage.contains("sqlite_busy_snapshot");
                
                if (isLocked && retryCount < maxRetries - 1) {
                    retryCount++;
                    long delay = baseDelay * (long) Math.pow(2, retryCount - 1); // Exponential backoff: 100ms, 200ms, 400ms, 800ms...
                    System.out.println("[OrderService] Database locked (SQLITE_BUSY_SNAPSHOT), retrying... (" + retryCount + "/" + maxRetries + ") after " + delay + "ms");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Order creation interrupted", ie);
                    }
                } else {
                    // Not a lock error or max retries reached
                    if (isLocked) {
                        throw new RuntimeException("Failed to create order after " + maxRetries + " retries due to database lock", e);
                    } else {
                        throw e;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to create order after " + maxRetries + " retries");
    }
    
    @Transactional(transactionManager = "orderTransactionManager", rollbackFor = Exception.class)
    private Order createOrderWithTransaction(Long userId, OrderRequest request) {
        return createOrderInternal(userId, request);
    }
    
    private Order createOrderInternal(Long userId, OrderRequest request) {
        // Validate input
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
            throw new RuntimeException("배달 주소는 필수입니다.");
        }
        if (request.getDeliveryTime() == null || request.getDeliveryTime().trim().isEmpty()) {
            throw new RuntimeException("배달 시간은 필수입니다.");
        }

        // Read from main database (menu, dinner types)
        DinnerType dinner = dinnerTypeRepository.findById(request.getDinnerTypeId())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 디너 타입입니다."));

        // Validate serving style for Champagne Feast
        if (dinner.getName().contains("샴페인") && !request.getServingStyle().equals("grand") && !request.getServingStyle().equals("deluxe")) {
            throw new RuntimeException("샴페인 축제 디너는 그랜드 또는 디럭스 스타일만 주문 가능합니다.");
        }

        LocalDateTime deliveryDateTime = parseDeliveryTime(request.getDeliveryTime());

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

        InventoryService.InventoryReservationPlan inventoryPlan =
                inventoryService.prepareReservations(request.getItems(), deliveryDateTime);
        
        // 주문 생성 시 자동 배달 스케줄 할당 제거 - 관리자가 나중에 할당하도록 함
        // DeliverySchedulingService.DeliveryAssignmentPlan assignmentPlan =
        //         deliverySchedulingService.prepareAssignment(request.getDeliveryAddress(), deliveryDateTime);

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

        // 주문 생성 시 직원 자동 할당 제거 - 관리자가 나중에 할당하도록 함
        // 주문은 하나만 생성되며, 직원 할당은 관리자가 스케줄 관리에서 할당
        String threadId = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        System.out.println("[OrderService] 주문 생성 시작 - 사용자 ID: " + userId + ", 디너 타입: " + request.getDinnerTypeId());
        System.out.println("[OrderService] 스레드: " + threadId);
        System.out.println("[OrderService] 배달 시간: " + request.getDeliveryTime());
        System.out.println("[OrderService] 배달 주소: " + request.getDeliveryAddress());
        
        // 중복 주문 확인: 동일한 사용자가 동일한 배달 시간과 주소로 최근 10초 이내에 주문을 생성했는지 확인
        String deliveryTimeStr = request.getDeliveryTime();
        String deliveryAddressStr = request.getDeliveryAddress();
        List<Order> recentOrders = orderRepository.findByUserIdAndDeliveryTimeAndDeliveryAddress(userId, deliveryTimeStr, deliveryAddressStr);
        
        // 최근 10초 이내에 동일한 주문이 있는지 확인
        long tenSecondsAgo = System.currentTimeMillis() - 10000;
        for (Order recentOrder : recentOrders) {
            if (recentOrder.getCreatedAt() != null && recentOrder.getCreatedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() > tenSecondsAgo) {
                System.out.println("[OrderService] 경고: 최근 10초 이내에 동일한 주문이 이미 존재합니다 - 주문 ID: " + recentOrder.getId());
                System.out.println("[OrderService] 기존 주문 생성 시간: " + recentOrder.getCreatedAt());
                System.out.println("[OrderService] 기존 주문 배달 시간: " + recentOrder.getDeliveryTime());
                System.out.println("[OrderService] 기존 주문 배달 주소: " + recentOrder.getDeliveryAddress());
                // 중복 주문이면 예외 발생 (중복 방지)
                throw new RuntimeException("동일한 주문이 최근에 생성되었습니다. 주문 ID: " + recentOrder.getId());
            }
        }
        
        Order savedOrder = orderRepository.save(order);
        System.out.println("[OrderService] 주문 저장 완료 - 주문 ID: " + savedOrder.getId());
        System.out.println("[OrderService] 스레드: " + threadId);

        // Add order items - save to order database
        for (OrderItemDto item : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setMenuItemId(item.getMenuItemId());
            orderItem.setQuantity(item.getQuantity());
            orderItemRepository.save(orderItem);
        }

        boolean inventoryCommitted = false;
        try {
            inventoryService.commitReservations(savedOrder.getId(), inventoryPlan);
            inventoryCommitted = true;
            // 주문 생성 시 자동 배달 스케줄 할당 제거 - 관리자가 나중에 할당하도록 함
            // deliverySchedulingService.commitAssignment(savedOrder.getId(), assignmentPlan);
        } catch (RuntimeException e) {
            // Rollback in reverse order
            try {
                if (inventoryCommitted) {
                    inventoryService.releaseReservationsForOrder(savedOrder.getId());
                }
            } catch (Exception rollbackEx) {
                System.err.println("[OrderService] Failed to rollback inventory: " + rollbackEx.getMessage());
            }
            try {
                orderItemRepository.deleteByOrderId(savedOrder.getId());
                orderRepository.deleteById(savedOrder.getId());
            } catch (Exception rollbackEx) {
                System.err.println("[OrderService] Failed to rollback order: " + rollbackEx.getMessage());
            }
            throw new RuntimeException("주문 생성에 실패했습니다: " + e.getMessage(), e);
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

    @Transactional(transactionManager = "orderTransactionManager", rollbackFor = Exception.class)
    public Order cancelOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        // For admin, allow cancelling any order; for regular users, only their own orders
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        if ("delivered".equals(order.getStatus())) {
            throw new RuntimeException("이미 배달 완료된 주문은 취소할 수 없습니다.");
        }
        if ("cancelled".equals(order.getStatus())) {
            throw new RuntimeException("이미 취소된 주문입니다.");
        }

        // Cancel inventory reservations (main database)
        boolean inventoryCancelled = false;
        try {
            inventoryService.releaseReservationsForOrder(orderId);
            inventoryCancelled = true;
            System.out.println("[OrderService] 주문 " + orderId + "의 재고 예약이 취소되었습니다.");
        } catch (Exception e) {
            System.err.println("[OrderService] 재고 예약 취소 실패: " + e.getMessage());
            e.printStackTrace();
            // Continue with cancellation even if inventory release fails
            // This is acceptable as the order cancellation should proceed
        }

        // Cancel delivery schedule (main database)
        boolean scheduleCancelled = false;
        try {
            deliverySchedulingService.cancelScheduleForOrder(orderId);
            scheduleCancelled = true;
            System.out.println("[OrderService] 주문 " + orderId + "의 배달 스케줄이 취소되었습니다.");
        } catch (Exception e) {
            System.err.println("[OrderService] 배달 스케줄 취소 실패: " + e.getMessage());
            e.printStackTrace();
            // Continue with cancellation even if schedule cancellation fails
        }

        // Update order status (order database)
        try {
            order.setStatus("cancelled");
            Order cancelledOrder = orderRepository.save(order);
            System.out.println("[OrderService] 주문 " + orderId + "가 취소되었습니다. (재고: " + 
                    (inventoryCancelled ? "취소됨" : "실패") + ", 스케줄: " + 
                    (scheduleCancelled ? "취소됨" : "실패") + ")");
            return cancelledOrder;
        } catch (Exception e) {
            System.err.println("[OrderService] 주문 상태 업데이트 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("주문 취소 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(transactionManager = "orderTransactionManager", rollbackFor = Exception.class)
    public void markOrderAsDelivered(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        if ("cancelled".equals(order.getStatus())) {
            throw new RuntimeException("취소된 주문은 배달 완료 처리할 수 없습니다.");
        }
        if ("delivered".equals(order.getStatus())) {
            System.out.println("[OrderService] 주문 " + orderId + "는 이미 배달 완료 상태입니다.");
            return; // Already delivered, no need to process again
        }

        // 재고는 주문 생성 시 이미 차감되었으므로, 배달 완료 시에는 추가 처리 불필요
        // (매일 자정에 전날 예약이 자동으로 삭제됨)
        System.out.println("[OrderService] 주문 " + orderId + " 배달 완료 - 재고는 주문 시 이미 차감되었습니다.");

        // Update order status (order database)
        try {
            order.setStatus("delivered");
            orderRepository.save(order);
            System.out.println("[OrderService] 주문 " + orderId + "가 배달 완료로 처리되었습니다.");
        } catch (Exception e) {
            System.err.println("[OrderService] 주문 상태 업데이트 실패: " + e.getMessage());
            e.printStackTrace();
            // If order status update fails after inventory consumption, 
            // we should ideally rollback, but since we're using separate databases,
            // we'll log the error and throw exception
            throw new RuntimeException("주문 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(transactionManager = "orderTransactionManager", rollbackFor = Exception.class)
    public Order modifyOrder(Long orderId, Long userId, OrderRequest request) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        // 주문 소유자 확인
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("이 주문을 수정할 권한이 없습니다.");
        }

        // pending 상태인 주문만 수정 가능
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("주문 수정은 주문 접수 상태에서만 가능합니다.");
        }

        // 기존 가격 저장
        int oldPrice = order.getTotalPrice();

        // 배달 시간 업데이트
        if (request.getDeliveryTime() != null && !request.getDeliveryTime().isEmpty()) {
            order.setDeliveryTime(request.getDeliveryTime());
        }

        // 배달 주소 업데이트
        if (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isEmpty()) {
            order.setDeliveryAddress(request.getDeliveryAddress());
        }

        // 주문 항목 업데이트
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // 기존 주문 항목 삭제
            orderItemRepository.deleteByOrderId(orderId);

            // 새 주문 항목 추가
            for (OrderItemDto itemDto : request.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(orderId);
                orderItem.setMenuItemId(itemDto.getMenuItemId());
                orderItem.setQuantity(itemDto.getQuantity());
                orderItemRepository.save(orderItem);
            }

            // 가격 재계산
            DinnerType dinner = dinnerTypeRepository.findById(order.getDinnerTypeId())
                    .orElseThrow(() -> new RuntimeException("디너 타입을 찾을 수 없습니다."));

            Map<String, Double> styleMultipliers = Map.of(
                    "simple", 1.0,
                    "grand", 1.3,
                    "deluxe", 1.6
            );
            double basePrice = dinner.getBasePrice() * styleMultipliers.getOrDefault(order.getServingStyle(), 1.0);

            // 항목 가격 추가
            double itemsPrice = 0;
            for (OrderItemDto item : request.getItems()) {
                MenuItem menuItem = menuItemRepository.findById(item.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("메뉴 항목을 찾을 수 없습니다: " + item.getMenuItemId()));
                itemsPrice += menuItem.getPrice() * item.getQuantity();
            }

            double totalPrice = basePrice + itemsPrice;

            // 단골 고객 할인 (10%)
            List<Order> previousOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (previousOrders.stream().anyMatch(o -> "paid".equals(o.getPaymentStatus()))) {
                totalPrice = totalPrice * 0.9;
            }

            // 수정 수수료 계산 (변경된 금액의 10%)
            int newPrice = (int) Math.round(totalPrice);
            int priceDifference = Math.abs(newPrice - oldPrice);
            int modificationFee = (int) Math.round(priceDifference * 0.1);
            int finalPrice = newPrice + modificationFee;

            order.setTotalPrice(finalPrice);
        }

        orderRepository.save(order);

        return order;
    }

    private LocalDateTime parseDeliveryTime(String deliveryTime) {
        // datetime-local 형식 지원 (예: "2025-11-21T10:00" 또는 "2025-11-21T10:00:00")
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,  // "2025-11-21T10:00:00"
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")  // "2025-11-21T10:00"
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(deliveryTime, formatter);
            } catch (DateTimeParseException e) {
                // 다음 포맷 시도
            }
        }
        
        throw new RuntimeException("잘못된 배달 시간 형식입니다. (예: 2025-11-19T18:00 또는 2025-11-19T18:00:00)");
    }
}
