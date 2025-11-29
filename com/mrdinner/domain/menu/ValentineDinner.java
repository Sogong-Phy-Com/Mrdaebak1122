package com.mrdinner.domain.menu;

import com.mrdinner.domain.common.Money;

/**
 * 발렌타인 디너 - 작은 하트 모양과 큐피드가 장식된 접시에 냅킨과 함께 와인과 스테이크가 제공
 */
public class ValentineDinner extends Dinner {
    private static final Money ROMANTIC_SETUP_FEE = Money.ofKRW(15000); // 로맨틱 설정비
    private static final Money WINE_PRICE = Money.ofKRW(25000); // 와인 가격
    private static final Money STEAK_PRICE = Money.ofKRW(35000); // 스테이크 가격
    private static final Money DECORATION_PRICE = Money.ofKRW(10000); // 하트/큐피드 장식비

    public ValentineDinner(String name, String description, Money basePrice, ServingStyle servingStyle) {
        super(name, description, basePrice, servingStyle);
        setupValentineMenu();
    }

    private void setupValentineMenu() {
        // 발렌타인 디너 기본 메뉴 구성
        MenuItem wine = new MenuItem("발렌타인 와인", 
            "로맨틱한 분위기의 특별한 와인", 
            WINE_PRICE, ItemType.BEVERAGE);
        wine.setPreparationTimeMinutes(5);
        
        MenuItem steak = new MenuItem("발렌타인 스테이크", 
            "특별한 날을 위한 프리미엄 스테이크", 
            STEAK_PRICE, ItemType.MAIN_COURSE);
        steak.setPreparationTimeMinutes(25);
        
        MenuItem decoration = new MenuItem("하트 & 큐피드 장식", 
            "작은 하트 모양과 큐피드가 장식된 특별한 접시와 냅킨", 
            DECORATION_PRICE, ItemType.APPETIZER);
        decoration.setPreparationTimeMinutes(10);
        
        addMenuItem(wine);
        addMenuItem(steak);
        addMenuItem(decoration);
    }

    @Override
    protected Money applyDiscounts(Money price) {
        // 발렌타인 디너는 기본 가격 그대로 (할인 없음)
        return price;
    }

    @Override
    public Money calculateTotalPrice() {
        Money total = super.calculateTotalPrice();
        return total.add(ROMANTIC_SETUP_FEE);
    }

    @Override
    public String getDinnerType() {
        return "발렌타인 디너";
    }

    public boolean isRomanticSetting() {
        return true;
    }

    public String getSpecialInstructions() {
        return "작은 하트 모양과 큐피드가 장식된 접시에 냅킨과 함께 와인과 스테이크가 제공됩니다";
    }

    public boolean includesWine() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("와인"));
    }

    public boolean includesSteak() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("스테이크"));
    }
}

