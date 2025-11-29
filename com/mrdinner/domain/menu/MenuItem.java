package com.mrdinner.domain.menu;

import com.mrdinner.domain.common.Money;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a menu item
 */
public class MenuItem {
    private final String itemId;
    private String name;
    private String description;
    private Money price;
    private ItemType itemType;
    private boolean isAvailable;
    private int preparationTimeMinutes;
    private String[] allergens;
    private int calories;

    public MenuItem(String name, String description, Money price, ItemType itemType) {
        this.itemId = UUID.randomUUID().toString();
        this.name = validateAndTrim(name, "Name");
        this.description = validateAndTrim(description, "Description");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.itemType = Objects.requireNonNull(itemType, "Item type cannot be null");
        this.isAvailable = true;
        this.preparationTimeMinutes = 15; // default
        this.allergens = new String[0];
        this.calories = 0;
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    public String getItemId() {
        return itemId;
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

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price) {
        this.price = Objects.requireNonNull(price, "Price cannot be null");
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = Objects.requireNonNull(itemType, "Item type cannot be null");
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public int getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }

    public void setPreparationTimeMinutes(int preparationTimeMinutes) {
        if (preparationTimeMinutes < 0) {
            throw new IllegalArgumentException("Preparation time cannot be negative");
        }
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    public String[] getAllergens() {
        return allergens.clone();
    }

    public void setAllergens(String[] allergens) {
        this.allergens = allergens != null ? allergens.clone() : new String[0];
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        if (calories < 0) {
            throw new IllegalArgumentException("Calories cannot be negative");
        }
        this.calories = calories;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MenuItem menuItem = (MenuItem) obj;
        return Objects.equals(itemId, menuItem.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    @Override
    public String toString() {
        return String.format("MenuItem{id='%s', name='%s', price=%s, type=%s, available=%s}", 
            itemId, name, price, itemType, isAvailable);
    }
}

