package com.mrdinner.domain.menu;

import com.mrdinner.domain.common.Money;

/**
 * 프렌치 디너 - 커피 한잔, 와인 한잔, 샐러드, 스테이크 제공
 */
public class FrenchDinner extends Dinner {
    private static final Money COFFEE_PRICE = Money.ofKRW(8000); // 커피 한잔
    private static final Money WINE_PRICE = Money.ofKRW(25000); // 와인 한잔
    private static final Money SALAD_PRICE = Money.ofKRW(15000); // 샐러드
    private static final Money STEAK_PRICE = Money.ofKRW(35000); // 스테이크
    private static final Money FRENCH_SETUP_FEE = Money.ofKRW(12000); // 프렌치 설정비

    public FrenchDinner(String name, String description, Money basePrice, ServingStyle servingStyle) {
        super(name, description, basePrice, servingStyle);
        setupFrenchMenu();
    }

    private void setupFrenchMenu() {
        // 프렌치 디너 기본 메뉴 구성
        MenuItem coffee = new MenuItem("프렌치 커피", 
            "프랑스식 프리미엄 커피 한잔", 
            COFFEE_PRICE, ItemType.BEVERAGE);
        coffee.setPreparationTimeMinutes(5);
        
        MenuItem wine = new MenuItem("프렌치 와인", 
            "프랑스산 프리미엄 와인 한잔", 
            WINE_PRICE, ItemType.BEVERAGE);
        wine.setPreparationTimeMinutes(3);
        
        MenuItem salad = new MenuItem("프렌치 샐러드", 
            "프랑스식 신선한 샐러드", 
            SALAD_PRICE, ItemType.APPETIZER);
        salad.setPreparationTimeMinutes(10);
        
        MenuItem steak = new MenuItem("프렌치 스테이크", 
            "프랑스식 조리법의 프리미엄 스테이크", 
            STEAK_PRICE, ItemType.MAIN_COURSE);
        steak.setPreparationTimeMinutes(25);
        
        addMenuItem(coffee);
        addMenuItem(wine);
        addMenuItem(salad);
        addMenuItem(steak);
    }

    @Override
    protected Money applyDiscounts(Money price) {
        // 프렌치 디너는 기본 가격 그대로 (할인 없음)
        return price;
    }

    @Override
    public Money calculateTotalPrice() {
        Money total = super.calculateTotalPrice();
        return total.add(FRENCH_SETUP_FEE);
    }

    @Override
    public String getDinnerType() {
        return "프렌치 디너";
    }

    public boolean includesCoffee() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("커피"));
    }

    public boolean includesWine() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("와인"));
    }

    public boolean includesSalad() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("샐러드"));
    }

    public boolean includesSteak() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("스테이크"));
    }

    public String getCuisineStyle() {
        return "프렌치 디너";
    }

    public String getSpecialInstructions() {
        return "커피 한잔, 와인 한잔, 샐러드, 스테이크가 제공되는 프랑스식 디너";
    }
}

