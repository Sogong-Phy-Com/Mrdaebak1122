package com.mrdinner.service;

import com.mrdinner.domain.menu.*;
import com.mrdinner.domain.common.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for managing menu operations
 * 메뉴 관련 비즈니스 로직을 처리하는 서비스
 */
public class MenuService {
    private List<Dinner> dinners;
    private List<MenuItem> menuItems;
    
    public MenuService() {
        this.dinners = new ArrayList<>();
        this.menuItems = new ArrayList<>();
        initializeMenuData();
    }
    
    /**
     * 모든 디너 메뉴 조회
     */
    public List<Map<String, Object>> getAllDinners() {
        List<Map<String, Object>> dinnerList = new ArrayList<>();
        
        for (Dinner dinner : dinners) {
            Map<String, Object> dinnerInfo = new HashMap<>();
            dinnerInfo.put("dinnerId", dinner.getDinnerId());
            dinnerInfo.put("name", dinner.getName());
            dinnerInfo.put("description", dinner.getDescription());
            dinnerInfo.put("dinnerType", dinner.getDinnerType());
            dinnerInfo.put("basePrice", dinner.getBasePrice().toString());
            dinnerInfo.put("servingStyle", dinner.getServingStyle().toString());
            dinnerInfo.put("isAvailable", dinner.isAvailable());
            dinnerInfo.put("preparationTime", dinner.getTotalPreparationTimeMinutes());
            
            // 서빙 스타일별 가격 계산
            Map<String, String> pricesByStyle = new HashMap<>();
            for (ServingStyle style : ServingStyle.values()) {
                Dinner tempDinner = createDinnerByType(dinner.getDinnerType(), style);
                if (tempDinner != null) {
                    pricesByStyle.put(style.toString(), tempDinner.calculateTotalPrice().toString());
                }
            }
            dinnerInfo.put("pricesByStyle", pricesByStyle);
            
            // 메뉴 아이템 목록
            List<Map<String, Object>> items = new ArrayList<>();
            for (MenuItem item : dinner.getMenuItems()) {
                Map<String, Object> itemInfo = new HashMap<>();
                itemInfo.put("name", item.getName());
                itemInfo.put("description", item.getDescription());
                itemInfo.put("price", item.getPrice().toString());
                itemInfo.put("itemType", item.getItemType().toString());
                items.add(itemInfo);
            }
            dinnerInfo.put("menuItems", items);
            
            dinnerList.add(dinnerInfo);
        }
        
        return dinnerList;
    }
    
    /**
     * 특정 디너 상세 정보 조회
     */
    public Map<String, Object> getDinnerDetails(String dinnerType) {
        Dinner dinner = dinners.stream()
            .filter(d -> d.getDinnerType().equals(dinnerType))
            .findFirst()
            .orElse(null);
            
        if (dinner == null) {
            return null;
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("dinnerId", dinner.getDinnerId());
        details.put("name", dinner.getName());
        details.put("description", dinner.getDescription());
        details.put("dinnerType", dinner.getDinnerType());
        details.put("basePrice", dinner.getBasePrice().toString());
        details.put("servingStyle", dinner.getServingStyle().toString());
        details.put("isAvailable", dinner.isAvailable());
        details.put("preparationTime", dinner.getTotalPreparationTimeMinutes());
        
        // 서빙 스타일별 가격 정보
        Map<String, Map<String, Object>> stylePrices = new HashMap<>();
        for (ServingStyle style : ServingStyle.values()) {
            Dinner styleDinner = createDinnerByType(dinnerType, style);
            if (styleDinner != null) {
                Map<String, Object> styleInfo = new HashMap<>();
                styleInfo.put("price", styleDinner.calculateTotalPrice().toString());
                styleInfo.put("description", style.getDescription());
                styleInfo.put("priceMultiplier", style.getPriceMultiplier());
                styleInfo.put("isAllowed", true);
                
                // 샴페인 축제 디너 제약사항
                if (dinnerType.equals("샴페인 축제 디너")) {
                    styleInfo.put("isAllowed", style.isAllowedForChampagneFeast());
                }
                
                stylePrices.put(style.toString(), styleInfo);
            }
        }
        details.put("stylePrices", stylePrices);
        
        // 구성 메뉴 아이템
        List<Map<String, Object>> items = new ArrayList<>();
        for (MenuItem item : dinner.getMenuItems()) {
            Map<String, Object> itemInfo = new HashMap<>();
            itemInfo.put("name", item.getName());
            itemInfo.put("description", item.getDescription());
            itemInfo.put("price", item.getPrice().toString());
            itemInfo.put("itemType", item.getItemType().toString());
            itemInfo.put("preparationTime", item.getPreparationTimeMinutes());
            items.add(itemInfo);
        }
        details.put("menuItems", items);
        
        return details;
    }
    
    /**
     * 서빙 스타일 목록 조회
     */
    public List<Map<String, Object>> getServingStyles() {
        List<Map<String, Object>> styles = new ArrayList<>();
        
        for (ServingStyle style : ServingStyle.values()) {
            Map<String, Object> styleInfo = new HashMap<>();
            styleInfo.put("name", style.toString());
            styleInfo.put("description", style.getDescription());
            styleInfo.put("priceMultiplier", style.getPriceMultiplier());
            styles.add(styleInfo);
        }
        
        return styles;
    }
    
    /**
     * 가격 계산 (디너 + 서빙 스타일 + 수량)
     */
    public Map<String, Object> calculatePrice(String dinnerType, String servingStyle, int quantity) {
        try {
            ServingStyle style = ServingStyle.valueOf(servingStyle.toUpperCase());
            Dinner dinner = createDinnerByType(dinnerType, style);
            
            if (dinner == null) {
                throw new IllegalArgumentException("지원하지 않는 디너 타입입니다: " + dinnerType);
            }
            
            // 샴페인 축제 디너 제약사항 체크
            if (dinnerType.equals("샴페인 축제 디너") && !style.isAllowedForChampagneFeast()) {
                throw new IllegalArgumentException("샴페인 축제 디너는 그랜드 또는 디럭스 스타일만 선택 가능합니다");
            }
            
            Money unitPrice = dinner.calculateTotalPrice();
            Money totalPrice = unitPrice.multiply(quantity);
            
            Map<String, Object> priceInfo = new HashMap<>();
            priceInfo.put("dinnerType", dinnerType);
            priceInfo.put("servingStyle", servingStyle);
            priceInfo.put("quantity", quantity);
            priceInfo.put("basePrice", dinner.getBasePrice().toString());
            priceInfo.put("stylePrice", style.calculateStylePrice(dinner.getBasePrice()).toString());
            priceInfo.put("unitPrice", unitPrice.toString());
            priceInfo.put("totalPrice", totalPrice.toString());
            priceInfo.put("currency", "KRW");
            
            return priceInfo;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("가격 계산 실패: " + e.getMessage());
        }
    }
    
    /**
     * 샴페인 축제 디너 제약사항 조회
     */
    public Map<String, Object> getChampagneFeastConstraints() {
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("allowedStyles", List.of("GRAND", "DELUXE"));
        constraints.put("message", "샴페인 축제 디너는 그랜드 또는 디럭스 스타일로만 주문 가능합니다");
        
        return constraints;
    }
    
    /**
     * 사용 가능한 메뉴 아이템 조회
     */
    public List<Map<String, Object>> getAvailableItems() {
        List<Map<String, Object>> items = new ArrayList<>();
        
        for (MenuItem item : menuItems) {
            if (item.isAvailable()) {
                Map<String, Object> itemInfo = new HashMap<>();
                itemInfo.put("menuItemId", item.getItemId());
                itemInfo.put("name", item.getName());
                itemInfo.put("description", item.getDescription());
                itemInfo.put("price", item.getPrice().toString());
                itemInfo.put("itemType", item.getItemType().toString());
                itemInfo.put("preparationTime", item.getPreparationTimeMinutes());
                itemInfo.put("isAvailable", item.isAvailable());
                items.add(itemInfo);
            }
        }
        
        return items;
    }
    
    /**
     * 디너 타입과 서빙 스타일로 디너 객체 생성
     */
    private Dinner createDinnerByType(String dinnerType, ServingStyle servingStyle) {
        Money basePrice;
        
        switch (dinnerType) {
            case "발렌타인 디너":
                basePrice = Money.ofKRW(85000);
                return new ValentineDinner("발렌타인 디너", "로맨틱한 발렌타인 디너", basePrice, servingStyle);
                
            case "프렌치 디너":
                basePrice = Money.ofKRW(95000);
                return new FrenchDinner("프렌치 디너", "프랑스식 디너", basePrice, servingStyle);
                
            case "잉글리시 디너":
                basePrice = Money.ofKRW(75000);
                return new EnglishDinner("잉글리시 디너", "영국식 디너", basePrice, servingStyle);
                
            case "샴페인 축제 디너":
                basePrice = Money.ofKRW(195000);
                return new ChampagneFeastDinner("샴페인 축제 디너", "럭셔리 샴페인 디너", basePrice, servingStyle);
                
            default:
                return null;
        }
    }
    
    /**
     * 메뉴 데이터 초기화
     */
    private void initializeMenuData() {
        try {
            // 발렌타인 디너 (기본: 그랜드 스타일)
            Dinner valentineDinner = new ValentineDinner(
                "발렌타인 디너", 
                "하트 모양과 큐피드가 장식된 접시에 냅킨과 함께 와인과 스테이크가 제공", 
                Money.ofKRW(85000), 
                ServingStyle.GRAND
            );
            dinners.add(valentineDinner);
            
            // 프렌치 디너 (기본: 그랜드 스타일)
            Dinner frenchDinner = new FrenchDinner(
                "프렌치 디너", 
                "커피 한잔, 와인 한잔, 샐러드, 스테이크 제공", 
                Money.ofKRW(95000), 
                ServingStyle.GRAND
            );
            dinners.add(frenchDinner);
            
            // 잉글리시 디너 (기본: 심플 스타일)
            Dinner englishDinner = new EnglishDinner(
                "잉글리시 디너", 
                "에그 스크램블, 베이컨, 빵, 스테이크가 제공", 
                Money.ofKRW(75000), 
                ServingStyle.SIMPLE
            );
            dinners.add(englishDinner);
            
            // 샴페인 축제 디너 (기본: 그랜드 스타일)
            Dinner champagneDinner = new ChampagneFeastDinner(
                "샴페인 축제 디너", 
                "2인분, 샴페인 1병, 바게트빵 4개, 커피포트, 와인, 스테이크", 
                Money.ofKRW(195000), 
                ServingStyle.GRAND
            );
            dinners.add(champagneDinner);
            
            System.out.println("메뉴 데이터 초기화 완료: " + dinners.size() + "개 디너");
            
        } catch (Exception e) {
            System.out.println("메뉴 데이터 초기화 실패: " + e.getMessage());
        }
    }
}
