package com.mrdinner.domain.menu;

import com.mrdinner.domain.common.Money;

/**
 * Enumeration representing different serving styles for dinner items
 * Based on Mr. DaeBak dinner service requirements
 */
public enum ServingStyle {
    SIMPLE("플라스틱 접시와 플라스틱 컵, 종이 냅킨이 플라스틱 쟁반에 제공", 0.0),
    GRAND("도자기 접시와 도자기 컵, 흰색 면 냅킨이 나무 쟁반에 제공", 0.15),
    DELUXE("꽃들이 있는 작은 꽃병, 도자기 접시와 도자기 컵, 린넨 냅킨이 나무 쟁반에 제공", 0.30);

    private final String description;
    private final double priceMultiplier; // 서빙 스타일별 가격 배수

    ServingStyle(String description, double priceMultiplier) {
        this.description = description;
        this.priceMultiplier = priceMultiplier;
    }

    public String getDescription() {
        return description;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    /**
     * 서빙 스타일에 따른 추가 가격을 계산
     */
    public Money calculateStylePrice(Money basePrice) {
        if (priceMultiplier > 0) {
            return basePrice.multiply(priceMultiplier);
        }
        return Money.zero("KRW");
    }

    /**
     * 샴페인 축제 디너는 그랜드 또는 디럭스 스타일만 가능
     */
    public boolean isAllowedForChampagneFeast() {
        return this == GRAND || this == DELUXE;
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}

