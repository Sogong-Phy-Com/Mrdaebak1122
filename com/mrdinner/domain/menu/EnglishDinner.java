package com.mrdinner.domain.menu;

import com.mrdinner.domain.common.Money;

/**
 * 잉글리시 디너 - 에그 스크램블, 베이컨, 빵, 스테이크가 제공
 */
public class EnglishDinner extends Dinner {
    private static final Money SCRAMBLED_EGGS_PRICE = Money.ofKRW(12000); // 에그 스크램블
    private static final Money BACON_PRICE = Money.ofKRW(15000); // 베이컨
    private static final Money BREAD_PRICE = Money.ofKRW(8000); // 빵
    private static final Money STEAK_PRICE = Money.ofKRW(35000); // 스테이크
    private static final Money ENGLISH_SETUP_FEE = Money.ofKRW(10000); // 잉글리시 설정비

    public EnglishDinner(String name, String description, Money basePrice, ServingStyle servingStyle) {
        super(name, description, basePrice, servingStyle);
        setupEnglishMenu();
    }

    private void setupEnglishMenu() {
        // 잉글리시 디너 기본 메뉴 구성
        MenuItem scrambledEggs = new MenuItem("에그 스크램블", 
            "영국식 부드러운 에그 스크램블", 
            SCRAMBLED_EGGS_PRICE, ItemType.MAIN_COURSE);
        scrambledEggs.setPreparationTimeMinutes(8);
        
        MenuItem bacon = new MenuItem("영국식 베이컨", 
            "바삭한 영국식 베이컨", 
            BACON_PRICE, ItemType.SIDE_DISH);
        bacon.setPreparationTimeMinutes(6);
        
        MenuItem bread = new MenuItem("영국식 빵", 
            "신선한 영국식 토스트 빵", 
            BREAD_PRICE, ItemType.BREAD);
        bread.setPreparationTimeMinutes(3);
        
        MenuItem steak = new MenuItem("잉글리시 스테이크", 
            "영국식 조리법의 프리미엄 스테이크", 
            STEAK_PRICE, ItemType.MAIN_COURSE);
        steak.setPreparationTimeMinutes(25);
        
        addMenuItem(scrambledEggs);
        addMenuItem(bacon);
        addMenuItem(bread);
        addMenuItem(steak);
    }

    @Override
    protected Money applyDiscounts(Money price) {
        // 잉글리시 디너는 기본 가격 그대로 (할인 없음)
        return price;
    }

    @Override
    public Money calculateTotalPrice() {
        Money total = super.calculateTotalPrice();
        return total.add(ENGLISH_SETUP_FEE);
    }

    @Override
    public String getDinnerType() {
        return "잉글리시 디너";
    }

    public boolean includesScrambledEggs() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("에그 스크램블"));
    }

    public boolean includesBacon() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("베이컨"));
    }

    public boolean includesBread() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("빵"));
    }

    public boolean includesSteak() {
        return menuItems.stream()
            .anyMatch(item -> item.getName().contains("스테이크"));
    }

    public String getCuisineStyle() {
        return "잉글리시 디너";
    }

    public String getSpecialInstructions() {
        return "에그 스크램블, 베이컨, 빵, 스테이크가 제공되는 영국식 디너";
    }
}

