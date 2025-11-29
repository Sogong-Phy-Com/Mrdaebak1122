package com.mrdinner.service;

import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.inventory.Inventory;
import com.mrdinner.domain.inventory.StockItem;
import com.mrdinner.domain.menu.MenuItem;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.order.OrderItem;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing inventory operations
 */
public class InventoryService {
    private final Inventory inventory;

    public InventoryService() {
        this.inventory = new Inventory();
        initializeDefaultInventory();
    }

    /**
     * Add a new stock item to inventory
     */
    public StockItem addStockItem(String itemName, String description, String category, 
                                String unit, int initialQuantity, int minimumQuantity, 
                                int maximumQuantity, Money unitCost, LocalDate expirationDate, 
                                String supplier, String location) {
        StockItem stockItem = new StockItem(itemName, description, category, unit,
            initialQuantity, minimumQuantity, maximumQuantity, unitCost, 
            expirationDate, supplier, location);
        
        inventory.addStockItem(stockItem);
        
        System.out.println("Added stock item: " + stockItem.getItemName());
        return stockItem;
    }

    /**
     * Remove stock item from inventory
     */
    public void removeStockItem(String stockItemId) {
        StockItem stockItem = inventory.getStockItem(stockItemId);
        if (stockItem != null) {
            inventory.removeStockItem(stockItemId);
            System.out.println("Removed stock item: " + stockItem.getItemName());
        }
    }

    /**
     * Update stock quantity
     */
    public void updateStockQuantity(String stockItemId, int newQuantity) {
        inventory.updateStockQuantity(stockItemId, newQuantity);
        
        StockItem stockItem = inventory.getStockItem(stockItemId);
        System.out.println(String.format("Updated stock for %s: %d units", 
            stockItem.getItemName(), newQuantity));
    }

    /**
     * Consume stock for an order
     */
    public void consumeStockForOrder(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            consumeStockForOrderItem(orderItem);
        }
        
        System.out.println("Consumed stock for order: " + order.getOrderId());
    }

    /**
     * Restore stock for a cancelled order
     */
    public void restoreStockForOrder(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            restoreStockForOrderItem(orderItem);
        }
        
        System.out.println("Restored stock for cancelled order: " + order.getOrderId());
    }

    /**
     * Get stock item by ID
     */
    public Optional<StockItem> getStockItem(String stockItemId) {
        StockItem stockItem = inventory.getStockItem(stockItemId);
        return stockItem != null ? Optional.of(stockItem) : Optional.empty();
    }

    /**
     * Get all stock items
     */
    public List<StockItem> getAllStockItems() {
        return inventory.getAllStockItems();
    }

    /**
     * Get low stock items
     */
    public List<StockItem> getLowStockItems() {
        return inventory.getLowStockItems();
    }

    /**
     * Get out of stock items
     */
    public List<StockItem> getOutOfStockItems() {
        return inventory.getOutOfStockItems();
    }

    /**
     * Get expired items
     */
    public List<StockItem> getExpiredItems() {
        return inventory.getExpiredItems();
    }

    /**
     * Get items expiring soon
     */
    public List<StockItem> getExpiringSoonItems(int daysThreshold) {
        return inventory.getExpiringSoonItems(daysThreshold);
    }

    /**
     * Get items by category
     */
    public List<StockItem> getItemsByCategory(String category) {
        return inventory.getItemsByCategory(category);
    }

    /**
     * Check if sufficient stock is available for an order
     */
    public boolean hasSufficientStock(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            if (!hasSufficientStockForOrderItem(orderItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get total inventory value
     */
    public Money getTotalInventoryValue() {
        return inventory.getTotalInventoryValue();
    }

    /**
     * Perform inventory check
     */
    public void performInventoryCheck() {
        inventory.performInventoryCheck();
        System.out.println("Inventory check completed");
    }

    /**
     * Get stock summary by category
     */
    public java.util.Map<String, Integer> getStockSummaryByCategory() {
        return inventory.getStockSummaryByCategory();
    }

    /**
     * Get items needing restock
     */
    public List<String> getItemsNeedingRestock() {
        return inventory.getStockItemIdsNeedingRestock();
    }

    /**
     * Restock an item
     */
    public void restockItem(String stockItemId, int quantity, String reason) {
        inventory.restockItem(stockItemId, quantity, reason);
        
        StockItem stockItem = inventory.getStockItem(stockItemId);
        System.out.println(String.format("Restocked %s: +%d units (%s)", 
            stockItem.getItemName(), quantity, reason));
    }

    /**
     * Get inventory statistics
     */
    public InventoryStats getInventoryStats() {
        return new InventoryStats(
            inventory.getTotalItemsCount(),
            inventory.getTotalStockQuantity(),
            inventory.getLowStockItems().size(),
            inventory.getOutOfStockItems().size(),
            inventory.getExpiredItems().size(),
            inventory.getTotalInventoryValue()
        );
    }

    private void consumeStockForOrderItem(OrderItem orderItem) {
        if (orderItem.isMenuItem()) {
            consumeStockForMenuItem(orderItem.getMenuItem(), orderItem.getQuantity());
        } else if (orderItem.isDinner()) {
            consumeStockForDinner(orderItem.getDinner(), orderItem.getQuantity());
        }
    }

    private void restoreStockForOrderItem(OrderItem orderItem) {
        if (orderItem.isMenuItem()) {
            restoreStockForMenuItem(orderItem.getMenuItem(), orderItem.getQuantity());
        } else if (orderItem.isDinner()) {
            restoreStockForDinner(orderItem.getDinner(), orderItem.getQuantity());
        }
    }

    private void consumeStockForMenuItem(MenuItem menuItem, int quantity) {
        // This would consume specific ingredients based on the menu item
        // For now, we'll simulate by consuming generic stock
        System.out.println(String.format("Consuming stock for %dx %s", quantity, menuItem.getName()));
    }

    private void restoreStockForMenuItem(MenuItem menuItem, int quantity) {
        // This would restore specific ingredients
        System.out.println(String.format("Restoring stock for %dx %s", quantity, menuItem.getName()));
    }

    private void consumeStockForDinner(com.mrdinner.domain.menu.Dinner dinner, int quantity) {
        // This would consume ingredients for all items in the dinner
        System.out.println(String.format("Consuming stock for %dx %s", quantity, dinner.getName()));
    }

    private void restoreStockForDinner(com.mrdinner.domain.menu.Dinner dinner, int quantity) {
        // This would restore ingredients for all items in the dinner
        System.out.println(String.format("Restoring stock for %dx %s", quantity, dinner.getName()));
    }

    private boolean hasSufficientStockForOrderItem(OrderItem orderItem) {
        // This would check if sufficient ingredients are available
        // For now, we'll assume stock is always sufficient
        return true;
    }

    private void initializeDefaultInventory() {
        // Add some default stock items
        addStockItem("Chicken Breast", "Fresh chicken breast", "Meat", "kg", 50, 10, 100, 
            Money.of(8.99, "USD"), LocalDate.now().plusDays(3), "Local Farm", "Cold Storage A");
        
        addStockItem("Tomatoes", "Fresh tomatoes", "Vegetables", "kg", 30, 5, 60, 
            Money.of(3.49, "USD"), LocalDate.now().plusDays(7), "Garden Fresh", "Produce Section");
        
        addStockItem("Olive Oil", "Extra virgin olive oil", "Pantry", "bottle", 20, 5, 40, 
            Money.of(12.99, "USD"), LocalDate.now().plusMonths(12), "Mediterranean Imports", "Pantry Shelf");
        
        addStockItem("Champagne", "Premium champagne", "Beverages", "bottle", 15, 3, 30, 
            Money.of(45.00, "USD"), LocalDate.now().plusYears(3), "Wine Distributors", "Wine Cellar");
    }

    /**
     * Data class for inventory statistics
     */
    public static class InventoryStats {
        private final int totalItems;
        private final int totalQuantity;
        private final int lowStockItems;
        private final int outOfStockItems;
        private final int expiredItems;
        private final Money totalValue;

        public InventoryStats(int totalItems, int totalQuantity, int lowStockItems, 
                            int outOfStockItems, int expiredItems, Money totalValue) {
            this.totalItems = totalItems;
            this.totalQuantity = totalQuantity;
            this.lowStockItems = lowStockItems;
            this.outOfStockItems = outOfStockItems;
            this.expiredItems = expiredItems;
            this.totalValue = totalValue;
        }

        public int getTotalItems() { return totalItems; }
        public int getTotalQuantity() { return totalQuantity; }
        public int getLowStockItems() { return lowStockItems; }
        public int getOutOfStockItems() { return outOfStockItems; }
        public int getExpiredItems() { return expiredItems; }
        public Money getTotalValue() { return totalValue; }

        @Override
        public String toString() {
            return String.format("InventoryStats{items=%d, quantity=%d, lowStock=%d, outOfStock=%d, expired=%d, value=%s}", 
                totalItems, totalQuantity, lowStockItems, outOfStockItems, expiredItems, totalValue);
        }
    }
}
