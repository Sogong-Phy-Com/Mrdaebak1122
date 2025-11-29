package com.mrdinner.domain.menu;

import com.mrdinner.domain.common.Money;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for dinner packages
 */
public abstract class Dinner {
    protected final String dinnerId;
    protected String name;
    protected String description;
    protected Money basePrice;
    protected ServingStyle servingStyle;
    protected List<MenuItem> menuItems;
    protected boolean isAvailable;
    protected int totalPreparationTimeMinutes;

    protected Dinner(String name, String description, Money basePrice, ServingStyle servingStyle) {
        this.dinnerId = UUID.randomUUID().toString();
        this.name = validateAndTrim(name, "Name");
        this.description = validateAndTrim(description, "Description");
        this.basePrice = Objects.requireNonNull(basePrice, "Base price cannot be null");
        this.servingStyle = Objects.requireNonNull(servingStyle, "Serving style cannot be null");
        this.menuItems = new ArrayList<>();
        this.isAvailable = true;
        this.totalPreparationTimeMinutes = 0;
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    public String getDinnerId() {
        return dinnerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateAndTrim(name, "Name");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = validateAndTrim(description, "Description");
    }

    public Money getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Money basePrice) {
        this.basePrice = Objects.requireNonNull(basePrice, "Base price cannot be null");
    }

    public ServingStyle getServingStyle() {
        return servingStyle;
    }

    public void setServingStyle(ServingStyle servingStyle) {
        this.servingStyle = Objects.requireNonNull(servingStyle, "Serving style cannot be null");
    }

    public List<MenuItem> getMenuItems() {
        return Collections.unmodifiableList(menuItems);
    }

    public void addMenuItem(MenuItem menuItem) {
        if (menuItem == null) {
            throw new IllegalArgumentException("Menu item cannot be null");
        }
        this.menuItems.add(menuItem);
        updatePreparationTime();
    }

    public void removeMenuItem(MenuItem menuItem) {
        this.menuItems.remove(menuItem);
        updatePreparationTime();
    }

    public boolean isAvailable() {
        return isAvailable && menuItems.stream().allMatch(MenuItem::isAvailable);
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public int getTotalPreparationTimeMinutes() {
        return totalPreparationTimeMinutes;
    }

    private void updatePreparationTime() {
        this.totalPreparationTimeMinutes = menuItems.stream()
            .mapToInt(MenuItem::getPreparationTimeMinutes)
            .sum();
    }

    /**
     * Calculate the total price for this dinner
     * Subclasses can override this method to add special pricing logic
     */
    public Money calculateTotalPrice() {
        Money total = basePrice;
        for (MenuItem item : menuItems) {
            total = total.add(item.getPrice());
        }
        
        // 서빙 스타일에 따른 추가 가격 적용
        Money stylePrice = servingStyle.calculateStylePrice(total);
        total = total.add(stylePrice);
        
        return applyDiscounts(total);
    }

    /**
     * Apply any discounts specific to this dinner type
     * Default implementation returns the price without discounts
     */
    protected Money applyDiscounts(Money price) {
        return price;
    }

    /**
     * Get the dinner type name
     */
    public abstract String getDinnerType();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Dinner dinner = (Dinner) obj;
        return Objects.equals(dinnerId, dinner.dinnerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dinnerId);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', name='%s', price=%s, style=%s, available=%s}", 
            getDinnerType(), dinnerId, name, basePrice, servingStyle, isAvailable());
    }
}

