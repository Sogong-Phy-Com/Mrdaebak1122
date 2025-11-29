package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.dto.OrderItemDto;
import com.mrdabak.dinnerservice.model.InventoryReservation;
import com.mrdabak.dinnerservice.model.MenuInventory;
import com.mrdabak.dinnerservice.model.MenuItem;
import com.mrdabak.dinnerservice.repository.inventory.InventoryReservationRepository;
import com.mrdabak.dinnerservice.repository.inventory.MenuInventoryRepository;
import com.mrdabak.dinnerservice.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class InventoryService {

    private final MenuInventoryRepository menuInventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final MenuItemRepository menuItemRepository;

    private final List<DayOfWeek> restockDays;
    private final LocalTime restockTime;
    private final int defaultCapacity;

    public InventoryService(MenuInventoryRepository menuInventoryRepository,
                            InventoryReservationRepository inventoryReservationRepository,
                            MenuItemRepository menuItemRepository,
                            @Value("${inventory.restock.days:MONDAY,FRIDAY}") String restockDaysProperty,
                            @Value("${inventory.restock.time:06:00}") String restockTimeProperty,
                            @Value("${inventory.default.capacity:20}") int defaultCapacity) {
        this.menuInventoryRepository = menuInventoryRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.menuItemRepository = menuItemRepository;
        this.restockDays = parseRestockDays(restockDaysProperty);
        this.restockTime = LocalTime.parse(restockTimeProperty);
        this.defaultCapacity = defaultCapacity;
    }

    public InventoryReservationPlan prepareReservations(List<OrderItemDto> items, LocalDateTime deliveryTime) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one menu item is required for reservation");
        }
        if (deliveryTime == null) {
            throw new IllegalArgumentException("Delivery time is required for inventory reservation");
        }

        RestockWindow window = resolveWindow(deliveryTime);
        Map<Long, Integer> aggregated = aggregateQuantities(items);

        Map<Long, MenuInventory> inventoryMap = new HashMap<>();
        for (Long menuItemId : aggregated.keySet()) {
            MenuInventory inventory = getInventory(menuItemId);
            inventoryMap.put(menuItemId, inventory);
            validateCapacity(menuItemId, inventory, aggregated.get(menuItemId), window, deliveryTime);
        }

        return new InventoryReservationPlan(window, aggregated, deliveryTime);
    }

    @Transactional("inventoryTransactionManager")
    public void commitReservations(Long orderId, InventoryReservationPlan plan) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (plan == null || plan.quantities() == null || plan.quantities().isEmpty()) {
            throw new IllegalArgumentException("재고 예약 계획은 필수입니다.");
        }

        for (Map.Entry<Long, Integer> entry : plan.quantities().entrySet()) {
            Long menuItemId = entry.getKey();
            Integer quantity = entry.getValue();
            
            // Verify menu item exists
            if (!menuItemRepository.existsById(menuItemId)) {
                throw new RuntimeException("메뉴 아이템을 찾을 수 없습니다: " + menuItemId);
            }
            
            // Re-validate capacity (race condition prevention)
            MenuInventory inventory = getInventory(menuItemId);
            validateCapacity(menuItemId, inventory, quantity, plan.window(), plan.deliveryTime());

            // 주문 시 재고 예약 저장 (조리 시작 시 소진)
            InventoryReservation reservation = new InventoryReservation();
            reservation.setOrderId(orderId);
            reservation.setMenuItemId(menuItemId);
            reservation.setQuantity(quantity);
            reservation.setWindowStart(plan.window().start());
            reservation.setWindowEnd(plan.window().end());
            reservation.setDeliveryTime(plan.deliveryTime());
            reservation.setConsumed(false);
            
            // 주류가 아닌 경우 3일 후 만료 설정
            com.mrdabak.dinnerservice.model.MenuItem menuItem = menuItemRepository.findById(menuItemId).orElse(null);
            if (menuItem != null && !isAlcoholCategory(menuItem.getCategory())) {
                reservation.setExpiresAt(plan.deliveryTime().plusDays(3));
            }
            
            InventoryReservation savedReservation = inventoryReservationRepository.save(reservation);
            System.out.println("[InventoryService] 주문 " + orderId + " - 메뉴 아이템 " + menuItemId + " 재고 " + quantity + "개 예약 완료 (예약 ID: " + savedReservation.getId() + ")");
            
            // 이번주 예약 수량에 즉시 반영 확인
            System.out.println("[InventoryService] 예약 저장 확인 - Order ID: " + orderId + ", Menu Item ID: " + menuItemId + ", Quantity: " + quantity);
        }
        System.out.println("[InventoryService] 주문 " + orderId + "의 모든 재고 예약이 완료되었습니다.");
    }
    
    private boolean isAlcoholCategory(String category) {
        if (category == null) return false;
        String lowerCategory = category.toLowerCase();
        return lowerCategory.contains("주류") || lowerCategory.contains("alcohol") || 
               lowerCategory.contains("wine") || lowerCategory.contains("beer") ||
               lowerCategory.contains("drink") || lowerCategory.contains("음료");
    }

    @Transactional("inventoryTransactionManager")
    public void releaseReservationsForOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        
        try {
            List<InventoryReservation> reservations = inventoryReservationRepository.findByOrderId(orderId);
            if (reservations.isEmpty()) {
                System.out.println("[InventoryService] 주문 " + orderId + "에 대한 재고 예약이 없습니다.");
                return;
            }
            
            int count = reservations.size();
            inventoryReservationRepository.deleteByOrderId(orderId);
            System.out.println("[InventoryService] 주문 " + orderId + "의 재고 예약 " + count + "개가 취소되었습니다.");
        } catch (Exception e) {
            System.err.println("[InventoryService] 재고 예약 취소 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("재고 예약 취소 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional("inventoryTransactionManager")
    public void consumeReservationsForOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        
        try {
            List<InventoryReservation> reservations = inventoryReservationRepository.findUnconsumedByOrderId(orderId);
            if (reservations.isEmpty()) {
                System.out.println("[InventoryService] 주문 " + orderId + "에 대한 미소진 재고 예약이 없습니다.");
                return;
            }

            // Mark reservations as consumed and deduct from current stock (조리 시작 시 재고 소진)
            // consumed=true로 설정하면 이번주 예약 수량에서 자동으로 제외됨 (sumWeeklyReservedByMenuItemId는 consumed=false만 포함)
            int count = 0;
            for (InventoryReservation reservation : reservations) {
                reservation.setConsumed(true);
                inventoryReservationRepository.save(reservation);
                
                // 현재 보유량에서 차감
                MenuInventory inventory = getInventory(reservation.getMenuItemId());
                int currentCapacity = inventory.getCapacityPerWindow() != null ? inventory.getCapacityPerWindow() : 0;
                int quantityToDeduct = reservation.getQuantity() != null ? reservation.getQuantity() : 0;
                int newCapacity = Math.max(0, currentCapacity - quantityToDeduct);
                inventory.setCapacityPerWindow(newCapacity);
                menuInventoryRepository.save(inventory);
                
                System.out.println("[InventoryService] 주문 " + orderId + " - 메뉴 아이템 " + reservation.getMenuItemId() + 
                    " 재고 " + quantityToDeduct + "개 차감 (현재 보유량: " + currentCapacity + " -> " + newCapacity + ")");
                System.out.println("[InventoryService] 주문 " + orderId + " - 메뉴 아이템 " + reservation.getMenuItemId() + 
                    " consumed=true로 설정되어 이번주 예약 수량에서 자동 차감됨");
                count++;
            }
            System.out.println("[InventoryService] 주문 " + orderId + "의 재고 예약 " + count + "개가 소진되었습니다. (이번주 예약 수량에서도 차감됨)");
        } catch (Exception e) {
            System.err.println("[InventoryService] 재고 소진 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("재고 소진 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(value = "inventoryTransactionManager")
    public List<InventorySnapshot> getInventorySnapshots() {
        LocalDateTime now = LocalDateTime.now();
        RestockWindow currentWindow = resolveWindow(now);

        // Get all menu items and ensure inventory exists for each
        List<MenuItem> allMenuItems = menuItemRepository.findAll();
        
        // Create inventory for menu items that don't have one yet
        for (MenuItem menuItem : allMenuItems) {
            if (!menuInventoryRepository.findByMenuItemId(menuItem.getId()).isPresent()) {
                // Auto-create inventory if it doesn't exist
                MenuInventory newInventory = new MenuInventory();
                newInventory.setMenuItemId(menuItem.getId());
                newInventory.setCapacityPerWindow(defaultCapacity);
                newInventory.setSafetyStock(0);
                newInventory.setNotes("auto-initialized");
                try {
                    menuInventoryRepository.save(newInventory);
                } catch (Exception e) {
                    // If save fails, ignore (might be created by another thread)
                    System.err.println("[InventoryService] Failed to auto-create inventory for menu item " + menuItem.getId() + ": " + e.getMessage());
                }
            }
        }

        // Now get all inventories (including newly created ones)
        // Calculate weekly reserved (this week's reservations - Sunday to Saturday)
        LocalDate today = LocalDate.now();
        // 일요일을 주의 시작으로 설정 (일요일 = 0, 월요일 = 1, ..., 토요일 = 6)
        int dayOfWeek = today.getDayOfWeek().getValue() % 7; // 일요일=0, 월요일=1, ..., 토요일=6
        LocalDate weekStart = today.minusDays(dayOfWeek); // 이번 주 일요일
        LocalDate weekEnd = weekStart.plusWeeks(1); // 다음 주 일요일 (토요일까지 포함)
        LocalDateTime weekStartDateTime = LocalDateTime.of(weekStart, LocalTime.MIN);
        LocalDateTime weekEndDateTime = LocalDateTime.of(weekEnd, LocalTime.MIN);
        
        return menuInventoryRepository.findAll().stream().map(inventory -> {
            // 현재 날짜의 예약 수량
            Integer reserved = inventoryReservationRepository
                    .sumQuantityByMenuItemIdAndWindowStart(inventory.getMenuItemId(), currentWindow.start());
            if (reserved == null) {
                reserved = 0;
            }
            
            // 이번주 예약 수량 (이번 주의 모든 예약 합산)
            Integer weeklyReserved = inventoryReservationRepository
                    .sumWeeklyReservedByMenuItemId(inventory.getMenuItemId(), weekStartDateTime, weekEndDateTime);
            if (weeklyReserved == null) {
                weeklyReserved = 0;
            }
            
            return new InventorySnapshot(
                    inventory,
                    reserved,
                    inventory.getCapacityPerWindow() - reserved,
                    currentWindow.start(),
                    currentWindow.end(),
                    weeklyReserved
            );
        }).toList();
    }

    @Transactional("inventoryTransactionManager")
    public MenuInventory restock(Long menuItemId, int newCapacity, String notes) {
        if (menuItemId == null) {
            throw new IllegalArgumentException("메뉴 아이템 ID는 필수입니다.");
        }
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("보충 용량은 0보다 커야 합니다.");
        }
        
        // Verify menu item exists
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new RuntimeException("메뉴 아이템을 찾을 수 없습니다: " + menuItemId);
        }
        
        MenuInventory inventory = getInventory(menuItemId);
        inventory.setCapacityPerWindow(newCapacity);
        inventory.setNotes(notes != null ? notes : "");
        inventory.setLastRestockedAt(LocalDateTime.now());
        return menuInventoryRepository.save(inventory);
    }

    @Transactional("inventoryTransactionManager")
    public MenuInventory setOrderedQuantity(Long menuItemId, int orderedQuantity) {
        if (menuItemId == null) {
            throw new IllegalArgumentException("메뉴 아이템 ID는 필수입니다.");
        }
        if (orderedQuantity < 0) {
            throw new IllegalArgumentException("주문 수량은 0 이상이어야 합니다.");
        }
        
        // Verify menu item exists
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new RuntimeException("메뉴 아이템을 찾을 수 없습니다: " + menuItemId);
        }
        
        MenuInventory inventory = getInventory(menuItemId);
        inventory.setOrderedQuantity(orderedQuantity);
        return menuInventoryRepository.save(inventory);
    }

    @Transactional("inventoryTransactionManager")
    public MenuInventory receiveOrderedInventory(Long menuItemId) {
        if (menuItemId == null) {
            throw new IllegalArgumentException("메뉴 아이템 ID는 필수입니다.");
        }
        
        // Verify menu item exists
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new RuntimeException("메뉴 아이템을 찾을 수 없습니다: " + menuItemId);
        }
        
        MenuInventory inventory = getInventory(menuItemId);
        int orderedQuantity = inventory.getOrderedQuantity() != null ? inventory.getOrderedQuantity() : 0;
        
        if (orderedQuantity <= 0) {
            throw new IllegalArgumentException("수령할 주문 재고가 없습니다.");
        }
        
        // 주문 재고를 현재 보유량에 추가
        int currentCapacity = inventory.getCapacityPerWindow() != null ? inventory.getCapacityPerWindow() : 0;
        inventory.setCapacityPerWindow(currentCapacity + orderedQuantity);
        
        // 주문 재고를 0으로 초기화
        inventory.setOrderedQuantity(0);
        
        return menuInventoryRepository.save(inventory);
    }

    private MenuInventory autoCreateInventory(Long menuItemId) {
        MenuInventory inventory = new MenuInventory();
        inventory.setMenuItemId(menuItemId);
        inventory.setCapacityPerWindow(defaultCapacity);
        inventory.setSafetyStock(0);
        inventory.setNotes("auto-initialized");
        try {
            return menuInventoryRepository.save(inventory);
        } catch (DataIntegrityViolationException e) {
            // Another thread created it first; fetch existing row
            return menuInventoryRepository.findByMenuItemId(menuItemId)
                    .orElseThrow(() -> e);
        }
    }

    private Map<Long, Integer> aggregateQuantities(List<OrderItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 비어있습니다.");
        }
        Map<Long, Integer> aggregated = new HashMap<>();
        for (OrderItemDto item : items) {
            if (item.getMenuItemId() == null) {
                throw new IllegalArgumentException("메뉴 아이템 ID는 필수입니다.");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("메뉴 아이템 수량은 1 이상이어야 합니다.");
            }
            aggregated.merge(item.getMenuItemId(), item.getQuantity(), Integer::sum);
        }
        return aggregated;
    }

    private void validateCapacity(Long menuItemId, MenuInventory inventory, Integer requestedQuantity, RestockWindow window) {
        validateCapacity(menuItemId, inventory, requestedQuantity, window, null);
    }
    
    private void validateCapacity(Long menuItemId, MenuInventory inventory, Integer requestedQuantity, RestockWindow window, LocalDateTime deliveryTime) {
        Integer alreadyReserved = inventoryReservationRepository
                .sumQuantityByMenuItemIdAndWindowStart(menuItemId, window.start());
        if (alreadyReserved == null) {
            alreadyReserved = 0;
        }
        int projected = alreadyReserved + requestedQuantity;
        
        // 3일 이하 예약은 현재 보유량 초과 불가, 3일 이상은 초과 가능
        boolean allowExceedCapacity = false;
        if (deliveryTime != null) {
            LocalDate deliveryDate = deliveryTime.toLocalDate();
            LocalDate today = LocalDate.now();
            long daysUntilDelivery = java.time.temporal.ChronoUnit.DAYS.between(today, deliveryDate);
            allowExceedCapacity = daysUntilDelivery >= 3;
        }
        
        int maxCapacity = allowExceedCapacity ? Integer.MAX_VALUE : inventory.getCapacityPerWindow();
        
        if (projected > maxCapacity) {
            String menuName = menuItemRepository.findById(menuItemId)
                    .map(item -> item.getName() + "(" + item.getNameEn() + ")")
                    .orElse("menu item " + menuItemId);
            throw new RuntimeException(String.format(
                    "%s 재고가 부족합니다. (요청: %d, 현재 예약: %d, 최대: %d)",
                    menuName,
                    requestedQuantity,
                    alreadyReserved,
                    maxCapacity
            ));
        }
    }

    private MenuInventory getInventory(Long menuItemId) {
        return menuInventoryRepository.findByMenuItemId(menuItemId)
                .orElseGet(() -> autoCreateInventory(menuItemId));
    }

    private List<DayOfWeek> parseRestockDays(String property) {
        if (property == null || property.isBlank()) {
            return List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        }
        return Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .toList();
    }

    private RestockWindow resolveWindow(LocalDateTime deliveryTime) {
        // 매일 기준으로 당일 윈도우 설정 (00:00 ~ 23:59:59)
        LocalDate deliveryDate = deliveryTime.toLocalDate();
        LocalDateTime start = LocalDateTime.of(deliveryDate, LocalTime.MIN); // 00:00:00
        LocalDateTime end = LocalDateTime.of(deliveryDate, LocalTime.MAX); // 23:59:59.999999999
        return new RestockWindow(start, end);
    }

    private LocalDateTime findLastRestock(LocalDateTime reference) {
        // 매일 기준이므로 당일 00:00 반환
        LocalDate date = reference.toLocalDate();
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    private LocalDateTime findNextRestock(LocalDateTime after) {
        // 매일 기준이므로 다음 날 00:00 반환
        LocalDate nextDate = after.toLocalDate().plusDays(1);
        return LocalDateTime.of(nextDate, LocalTime.MIN);
    }

    public record InventoryReservationPlan(RestockWindow window,
                                           Map<Long, Integer> quantities,
                                           LocalDateTime deliveryTime) { }

    public record RestockWindow(LocalDateTime start, LocalDateTime end) { }

    public record InventorySnapshot(MenuInventory inventory,
                                    int reserved,
                                    int remaining,
                                    LocalDateTime windowStart,
                                    LocalDateTime windowEnd,
                                    int weeklyReserved) { }
}

