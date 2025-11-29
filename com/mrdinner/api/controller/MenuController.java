package com.mrdinner.api.controller;

import com.mrdinner.domain.menu.*;
import com.mrdinner.domain.common.Money;
import com.mrdinner.service.MenuService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST API Controller for Menu operations
 * 앱/웹에서 메뉴 관련 기능을 처리하는 API
 */
@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {
    
    private final MenuService menuService;
    
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }
    
    /**
     * 모든 디너 메뉴 조회 API
     * GET /api/menu/dinners
     */
    @GetMapping("/dinners")
    public ResponseEntity<Map<String, Object>> getAllDinners() {
        try {
            List<Map<String, Object>> dinners = menuService.getAllDinners();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "dinners", dinners,
                "totalCount", dinners.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "메뉴 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 특정 디너 상세 정보 조회 API
     * GET /api/menu/dinners/{dinnerType}
     */
    @GetMapping("/dinners/{dinnerType}")
    public ResponseEntity<Map<String, Object>> getDinnerDetails(@PathVariable String dinnerType) {
        try {
            Map<String, Object> dinnerDetails = menuService.getDinnerDetails(dinnerType);
            
            if (dinnerDetails != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "dinner", dinnerDetails
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "디너 정보 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 서빙 스타일 목록 조회 API
     * GET /api/menu/serving-styles
     */
    @GetMapping("/serving-styles")
    public ResponseEntity<Map<String, Object>> getServingStyles() {
        try {
            List<Map<String, Object>> servingStyles = menuService.getServingStyles();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "servingStyles", servingStyles
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "서빙 스타일 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 가격 계산 API (디너 + 서빙 스타일)
     * POST /api/menu/calculate-price
     */
    @PostMapping("/calculate-price")
    public ResponseEntity<Map<String, Object>> calculatePrice(@RequestBody PriceCalculationRequest request) {
        try {
            Map<String, Object> priceInfo = menuService.calculatePrice(
                request.getDinnerType(),
                request.getServingStyle(),
                request.getQuantity()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "priceInfo", priceInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "가격 계산 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 샴페인 축제 디너 서빙 스타일 제약 확인 API
     * GET /api/menu/champagne-feast/constraints
     */
    @GetMapping("/champagne-feast/constraints")
    public ResponseEntity<Map<String, Object>> getChampagneFeastConstraints() {
        try {
            Map<String, Object> constraints = menuService.getChampagneFeastConstraints();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "constraints", constraints
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "제약사항 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 사용 가능한 메뉴 아이템 조회 API
     * GET /api/menu/items
     */
    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> getAvailableItems() {
        try {
            List<Map<String, Object>> items = menuService.getAvailableItems();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "items", items,
                "totalCount", items.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "메뉴 아이템 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    // Request DTO
    public static class PriceCalculationRequest {
        private String dinnerType;
        private String servingStyle;
        private int quantity = 1;
        
        // Getters and Setters
        public String getDinnerType() { return dinnerType; }
        public void setDinnerType(String dinnerType) { this.dinnerType = dinnerType; }
        
        public String getServingStyle() { return servingStyle; }
        public void setServingStyle(String servingStyle) { this.servingStyle = servingStyle; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
