package com.mrdinner.domain.menu;

import com.mrdinner.domain.common.Money;

/**
 * 샴페인 축제 디너 - 항상 2명의 식사이고, 샴페인 병 1개, 바게트빵 4개, 커피 포트 1개, 와인, 스테이크 제공
 * 그랜드 또는 디럭스 스타일로만 주문 가능
 */
public class ChampagneFeastDinner extends Dinner {
    private static final Money CHAMPAGNE_PRICE = Money.ofKRW(80000); // 샴페인 1병
    private static final Money BAGUETTE_PRICE = Money.ofKRW(20000); // 바게트빵 4개
    private static final Money COFFEE_POT_PRICE = Money.ofKRW(15000); // 커피 포트 1개
    private static final Money WINE_PRICE = Money.ofKRW(35000); // 와인
    private static final Money STEAK_PRICE = Money.ofKRW(45000); // 스테이크
    private static final Money FEAST_SETUP_FEE = Money.ofKRW(25000); // 축제 설정비

    public ChampagneFeastDinner(String name, String description, Money basePrice, ServingStyle servingStyle) {
        super(name, description, basePrice, servingStyle);
        validateServingStyle(servingStyle);
        setupChampagneFeastMenu();
    }

    private void validateServingStyle(ServingStyle servingStyle) {
        if (!servingStyle.isAllowedForChampagneFeast()) {
            throw new IllegalArgumentException("샴페인 축제 디너는 그랜드 또는 디럭스 스타일로만 주문 가능합니다");
        }
    }

    private void setupChampagneFeastMenu() {
        // 샴페인 축제 디너 기본 메뉴 구성 (2인분)
        MenuItem champagne = new MenuItem("프리미엄 샴페인", 
            "축제를 위한 고급 샴페인 1병", 
            CHAMPAGNE_PRICE, ItemType.BEVERAGE);
        champagne.setPreparationTimeMinutes(5);
        
        MenuItem baguette = new MenuItem("바게트빵", 
            "신선한 바게트빵 4개", 
            BAGUETTE_PRICE, ItemType.SIDE_DISH);
        baguette.setPreparationTimeMinutes(3);
        
        MenuItem coffeePot = new MenuItem("커피 포트", 
            "따뜻한 커피 포트 1개", 
            COFFEE_POT_PRICE, ItemType.BEVERAGE);
        coffeePot.setPreparationTimeMinutes(8);
        
        MenuItem wine = new MenuItem("프리미엄 와인", 
            "축제를 위한 특별한 와인", 
            WINE_PRICE, ItemType.BEVERAGE);
        wine.setPreparationTimeMinutes(5);
        
        MenuItem steak = new MenuItem("축제용 스테이크", 
            "2인분 프리미엄 스테이크", 
            STEAK_PRICE, ItemType.MAIN_COURSE);
        steak.setPreparationTimeMinutes(30);
        
        addMenuItem(champagne);
        addMenuItem(baguette);
        addMenuItem(coffeePot);
        addMenuItem(wine);
        addMenuItem(steak);
    }

    @Override
    public void setServingStyle(ServingStyle servingStyle) {
        validateServingStyle(servingStyle);
        super.setServingStyle(servingStyle);
    }

    @Override
    protected Money applyDiscounts(Money price) {
        // 샴페인 축제 디너는 기본 가격 그대로 (할인 없음)
        return price;
    }

    @Override
    public Money calculateTotalPrice() {
        Money total = super.calculateTotalPrice();
        return total.add(FEAST_SETUP_FEE);
    }

    @Override
    public String getDinnerType() {
        return "샴페인 축제 디너";
    }

    public boolean includesChampagne() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("샴페인"));
    }

    public boolean isLuxuryDinner() {
        return true;
    }

    public String getCuisineStyle() {
        return "샴페인 축제 디너 (2인분)";
    }

    public String getSpecialInstructions() {
        return "항상 2명의 식사이고, 샴페인 병 1개, 바게트빵 4개, 커피 포트 1개, 와인, 스테이크가 제공됩니다";
    }

    public Money getChampagneValue() {
        return menuItems.stream()
            .filter(item -> item.getName().contains("샴페인"))
            .findFirst()
            .map(MenuItem::getPrice)
            .orElse(Money.zeroKRW());
    }

    public int getServingCount() {
        return 2; // 항상 2인분
    }
}

