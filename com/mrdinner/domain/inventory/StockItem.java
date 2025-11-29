package com.mrdinner.domain.inventory;

import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.menu.MenuItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a stock item in inventory
 */
public class StockItem {
    private final String stockItemId;
    private String itemName;
    private String description;
    private String category;
    private String unit;
    private int currentQuantity;
    private int minimumQuantity;
    private int maximumQuantity;
    private Money unitCost;
    private LocalDate expirationDate;
    private String supplier;
    private String location;
    private LocalDateTime lastUpdated;
    private StockStatus status;

    public StockItem(String itemName, String description, String category, String unit,
                    int initialQuantity, int minimumQuantity, int maximumQuantity,
                    Money unitCost, LocalDate expirationDate, String supplier, String location) {
        this.stockItemId = UUID.randomUUID().toString();
        this.itemName = validateAndTrim(itemName, "Item name");
        this.description = validateAndTrim(description, "Description");
        this.category = validateAndTrim(category, "Category");
        this.unit = validateAndTrim(unit, "Unit");
        this.currentQuantity = validateQuantity(initialQuantity, "Initial quantity");
        this.minimumQuantity = validateQuantity(minimumQuantity, "Minimum quantity");
        this.maximumQuantity = validateQuantity(maximumQuantity, "Maximum quantity");
        this.unitCost = Objects.requireNonNull(unitCost, "Unit cost cannot be null");
        this.expirationDate = expirationDate;
        this.supplier = validateAndTrim(supplier, "Supplier");
        this.location = validateAndTrim(location, "Location");
        this.lastUpdated = LocalDateTime.now();
        this.status = StockStatus.AVAILABLE;
        
        validateQuantityLimits();
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    private int validateQuantity(int quantity, String fieldName) {
        if (quantity < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        return quantity;
    }

    private void validateQuantityLimits() {
        if (minimumQuantity > maximumQuantity) {
            throw new IllegalArgumentException("Minimum quantity cannot be greater than maximum quantity");
        }
        if (currentQuantity > maximumQuantity) {
            throw new IllegalArgumentException("Current quantity cannot exceed maximum quantity");
        }
    }

    public String getStockItemId() {
        return stockItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = validateAndTrim(itemName, "Item name");
        updateLastModified();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = validateAndTrim(description, "Description");
        updateLastModified();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = validateAndTrim(category, "Category");
        updateLastModified();
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = validateAndTrim(unit, "Unit");
        updateLastModified();
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }

    public void setMinimumQuantity(int minimumQuantity) {
        this.minimumQuantity = validateQuantity(minimumQuantity, "Minimum quantity");
        validateQuantityLimits();
        updateLastModified();
    }

    public int getMaximumQuantity() {
        return maximumQuantity;
    }

    public void setMaximumQuantity(int maximumQuantity) {
        this.maximumQuantity = validateQuantity(maximumQuantity, "Maximum quantity");
        validateQuantityLimits();
        updateLastModified();
    }

    public Money getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Money unitCost) {
        this.unitCost = Objects.requireNonNull(unitCost, "Unit cost cannot be null");
        updateLastModified();
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
        updateLastModified();
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = validateAndTrim(supplier, "Supplier");
        updateLastModified();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = validateAndTrim(location, "Location");
        updateLastModified();
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        updateLastModified();
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }
        
        int newQuantity = currentQuantity + quantity;
        if (newQuantity > maximumQuantity) {
            throw new IllegalArgumentException("Adding stock would exceed maximum quantity");
        }
        
        this.currentQuantity = newQuantity;
        updateStatus();
        updateLastModified();
    }

    public void removeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }
        if (quantity > currentQuantity) {
            throw new IllegalArgumentException("Cannot remove more stock than available");
        }
        
        this.currentQuantity -= quantity;
        updateStatus();
        updateLastModified();
    }

    public boolean isLowStock() {
        return currentQuantity <= minimumQuantity;
    }

    public boolean isOutOfStock() {
        return currentQuantity == 0;
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int daysThreshold) {
        if (expirationDate == null) {
            return false;
        }
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return expirationDate.isBefore(thresholdDate);
    }

    public int getDaysUntilExpiration() {
        if (expirationDate == null) {
            return Integer.MAX_VALUE;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    public Money getTotalValue() {
        return unitCost.multiply(currentQuantity);
    }

    public boolean needsRestocking() {
        return isLowStock() || isOutOfStock();
    }

    private void updateStatus() {
        if (isOutOfStock()) {
            status = StockStatus.OUT_OF_STOCK;
        } else if (isLowStock()) {
            status = StockStatus.LOW_STOCK;
        } else if (isExpired()) {
            status = StockStatus.EXPIRED;
        } else {
            status = StockStatus.AVAILABLE;
        }
    }

    private void updateLastModified() {
        this.lastUpdated = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StockItem stockItem = (StockItem) obj;
        return Objects.equals(stockItemId, stockItem.stockItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockItemId);
    }

    @Override
    public String toString() {
        return String.format("StockItem{id='%s', name='%s', quantity=%d/%d, status=%s}", 
            stockItemId, itemName, currentQuantity, maximumQuantity, status);
    }

    public enum StockStatus {
        AVAILABLE("Available for use"),
        LOW_STOCK("Running low, needs restocking"),
        OUT_OF_STOCK("No stock available"),
        EXPIRED("Item has expired"),
        DAMAGED("Item is damaged and unusable");

        private final String description;

        StockStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
