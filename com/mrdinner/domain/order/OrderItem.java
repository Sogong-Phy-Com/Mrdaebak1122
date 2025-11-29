package com.mrdinner.domain.order;

import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.menu.Dinner;
import com.mrdinner.domain.menu.MenuItem;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an item in an order
 */
public class OrderItem {
    private final String orderItemId;
    private final MenuItem menuItem;
    private final Dinner dinner;
    private int quantity;
    private Money unitPrice;
    private Money totalPrice;
    private String specialInstructions;

    public OrderItem(MenuItem menuItem, int quantity) {
        this.orderItemId = UUID.randomUUID().toString();
        this.menuItem = Objects.requireNonNull(menuItem, "Menu item cannot be null");
        this.dinner = null;
        this.quantity = validateQuantity(quantity);
        this.unitPrice = menuItem.getPrice();
        this.totalPrice = unitPrice.multiply(quantity);
        this.specialInstructions = "";
    }

    public OrderItem(Dinner dinner, int quantity) {
        this.orderItemId = UUID.randomUUID().toString();
        this.menuItem = null;
        this.dinner = Objects.requireNonNull(dinner, "Dinner cannot be null");
        this.quantity = validateQuantity(quantity);
        this.unitPrice = dinner.calculateTotalPrice();
        this.totalPrice = unitPrice.multiply(quantity);
        this.specialInstructions = "";
    }

    private int validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        return quantity;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public Dinner getDinner() {
        return dinner;
    }

    public boolean isMenuItem() {
        return menuItem != null;
    }

    public boolean isDinner() {
        return dinner != null;
    }

    public String getItemName() {
        return isMenuItem() ? menuItem.getName() : dinner.getName();
    }

    public String getItemDescription() {
        return isMenuItem() ? menuItem.getDescription() : dinner.getDescription();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = validateQuantity(quantity);
        this.totalPrice = unitPrice.multiply(this.quantity);
    }

    public void updateQuantity(int newQuantity) {
        this.quantity = validateQuantity(newQuantity);
        this.totalPrice = unitPrice.multiply(this.quantity);
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Money unitPrice) {
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        this.totalPrice = unitPrice.multiply(quantity);
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions != null ? specialInstructions.trim() : "";
    }

    public int getPreparationTimeMinutes() {
        return isMenuItem() ? 
            menuItem.getPreparationTimeMinutes() * quantity : 
            dinner.getTotalPreparationTimeMinutes() * quantity;
    }

    public boolean isAvailable() {
        return isMenuItem() ? menuItem.isAvailable() : dinner.isAvailable();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderItem orderItem = (OrderItem) obj;
        return Objects.equals(orderItemId, orderItem.orderItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderItemId);
    }

    @Override
    public String toString() {
        return String.format("OrderItem{id='%s', item='%s', quantity=%d, totalPrice=%s}", 
            orderItemId, getItemName(), quantity, totalPrice);
    }
}
