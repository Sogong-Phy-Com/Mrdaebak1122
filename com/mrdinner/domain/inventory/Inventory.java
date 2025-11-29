package com.mrdinner.domain.inventory;

import com.mrdinner.domain.common.Money;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity representing the restaurant's inventory management system
 */
public class Inventory {
    private final String inventoryId;
    private Map<String, StockItem> stockItems;
    private List<InventoryTransaction> transactions;
    private LocalDate lastInventoryCheck;
    private Money totalInventoryValue;

    public Inventory() {
        this.inventoryId = UUID.randomUUID().toString();
        this.stockItems = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.lastInventoryCheck = LocalDate.now();
        this.totalInventoryValue = Money.zero("USD");
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public Map<String, StockItem> getStockItems() {
        return Collections.unmodifiableMap(stockItems);
    }

    public List<StockItem> getAllStockItems() {
        return new ArrayList<>(stockItems.values());
    }

    public StockItem getStockItem(String stockItemId) {
        return stockItems.get(stockItemId);
    }

    public void addStockItem(StockItem stockItem) {
        if (stockItem == null) {
            throw new IllegalArgumentException("Stock item cannot be null");
        }
        
        stockItems.put(stockItem.getStockItemId(), stockItem);
        
        // Record transaction
        InventoryTransaction transaction = new InventoryTransaction(
            TransactionType.ADD_ITEM,
            stockItem,
            stockItem.getCurrentQuantity(),
            "Added new stock item to inventory"
        );
        transactions.add(transaction);
        
        recalculateTotalValue();
    }

    public void removeStockItem(String stockItemId) {
        StockItem stockItem = stockItems.remove(stockItemId);
        if (stockItem != null) {
            // Record transaction
            InventoryTransaction transaction = new InventoryTransaction(
                TransactionType.REMOVE_ITEM,
                stockItem,
                stockItem.getCurrentQuantity(),
                "Removed stock item from inventory"
            );
            transactions.add(transaction);
            
            recalculateTotalValue();
        }
    }

    public void updateStockQuantity(String stockItemId, int newQuantity) {
        StockItem stockItem = stockItems.get(stockItemId);
        if (stockItem == null) {
            throw new IllegalArgumentException("Stock item not found: " + stockItemId);
        }
        
        int oldQuantity = stockItem.getCurrentQuantity();
        int difference = newQuantity - oldQuantity;
        
        if (difference > 0) {
            stockItem.addStock(difference);
            recordTransaction(TransactionType.RESTOCK, stockItem, difference, "Stock restocked");
        } else if (difference < 0) {
            stockItem.removeStock(-difference);
            recordTransaction(TransactionType.CONSUMPTION, stockItem, -difference, "Stock consumed");
        }
        
        recalculateTotalValue();
    }

    public void consumeStock(String stockItemId, int quantity, String reason) {
        StockItem stockItem = stockItems.get(stockItemId);
        if (stockItem == null) {
            throw new IllegalArgumentException("Stock item not found: " + stockItemId);
        }
        
        stockItem.removeStock(quantity);
        recordTransaction(TransactionType.CONSUMPTION, stockItem, quantity, reason);
        recalculateTotalValue();
    }

    public void restockItem(String stockItemId, int quantity, String reason) {
        StockItem stockItem = stockItems.get(stockItemId);
        if (stockItem == null) {
            throw new IllegalArgumentException("Stock item not found: " + stockItemId);
        }
        
        stockItem.addStock(quantity);
        recordTransaction(TransactionType.RESTOCK, stockItem, quantity, reason);
        recalculateTotalValue();
    }

    private void recordTransaction(TransactionType type, StockItem stockItem, int quantity, String reason) {
        InventoryTransaction transaction = new InventoryTransaction(
            type, stockItem, quantity, reason
        );
        transactions.add(transaction);
    }

    public List<InventoryTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public List<InventoryTransaction> getTransactionsByType(TransactionType type) {
        return transactions.stream()
            .filter(t -> t.getTransactionType() == type)
            .collect(Collectors.toList());
    }

    public List<StockItem> getLowStockItems() {
        return stockItems.values().stream()
            .filter(StockItem::isLowStock)
            .collect(Collectors.toList());
    }

    public List<StockItem> getOutOfStockItems() {
        return stockItems.values().stream()
            .filter(StockItem::isOutOfStock)
            .collect(Collectors.toList());
    }

    public List<StockItem> getExpiredItems() {
        return stockItems.values().stream()
            .filter(StockItem::isExpired)
            .collect(Collectors.toList());
    }

    public List<StockItem> getExpiringSoonItems(int daysThreshold) {
        return stockItems.values().stream()
            .filter(item -> item.isExpiringSoon(daysThreshold))
            .collect(Collectors.toList());
    }

    public List<StockItem> getItemsByCategory(String category) {
        return stockItems.values().stream()
            .filter(item -> item.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }

    public List<StockItem> getItemsBySupplier(String supplier) {
        return stockItems.values().stream()
            .filter(item -> item.getSupplier().equalsIgnoreCase(supplier))
            .collect(Collectors.toList());
    }

    public Money getTotalInventoryValue() {
        return totalInventoryValue;
    }

    private void recalculateTotalValue() {
        this.totalInventoryValue = stockItems.values().stream()
            .map(StockItem::getTotalValue)
            .reduce(Money.zero("USD"), Money::add);
    }

    public LocalDate getLastInventoryCheck() {
        return lastInventoryCheck;
    }

    public void performInventoryCheck() {
        this.lastInventoryCheck = LocalDate.now();
        recordTransaction(TransactionType.INVENTORY_CHECK, null, 0, "Regular inventory check performed");
    }

    public Map<String, Integer> getStockSummaryByCategory() {
        return stockItems.values().stream()
            .collect(Collectors.groupingBy(
                StockItem::getCategory,
                Collectors.summingInt(StockItem::getCurrentQuantity)
            ));
    }

    public boolean hasSufficientStock(String stockItemId, int requiredQuantity) {
        StockItem stockItem = stockItems.get(stockItemId);
        return stockItem != null && stockItem.getCurrentQuantity() >= requiredQuantity;
    }

    public List<String> getStockItemIdsNeedingRestock() {
        return stockItems.values().stream()
            .filter(StockItem::needsRestocking)
            .map(StockItem::getStockItemId)
            .collect(Collectors.toList());
    }

    public int getTotalItemsCount() {
        return stockItems.size();
    }

    public int getTotalStockQuantity() {
        return stockItems.values().stream()
            .mapToInt(StockItem::getCurrentQuantity)
            .sum();
    }

    @Override
    public String toString() {
        return String.format("Inventory{id='%s', items=%d, totalValue=%s, lastCheck=%s}", 
            inventoryId, stockItems.size(), totalInventoryValue, lastInventoryCheck);
    }

    public enum TransactionType {
        ADD_ITEM("Add new item to inventory"),
        REMOVE_ITEM("Remove item from inventory"),
        RESTOCK("Restock existing item"),
        CONSUMPTION("Consume stock for production"),
        INVENTORY_CHECK("Regular inventory check"),
        ADJUSTMENT("Manual stock adjustment");

        private final String description;

        TransactionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class InventoryTransaction {
        private final String transactionId;
        private final TransactionType transactionType;
        private final StockItem stockItem;
        private final int quantity;
        private final String reason;
        private final LocalDateTime timestamp;

        public InventoryTransaction(TransactionType transactionType, StockItem stockItem, 
                                  int quantity, String reason) {
            this.transactionId = UUID.randomUUID().toString();
            this.transactionType = transactionType;
            this.stockItem = stockItem;
            this.quantity = quantity;
            this.reason = reason;
            this.timestamp = LocalDateTime.now();
        }

        public String getTransactionId() {
            return transactionId;
        }

        public TransactionType getTransactionType() {
            return transactionType;
        }

        public StockItem getStockItem() {
            return stockItem;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getReason() {
            return reason;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("Transaction{id='%s', type=%s, item='%s', quantity=%d, reason='%s'}", 
                transactionId, transactionType, 
                stockItem != null ? stockItem.getItemName() : "N/A", 
                quantity, reason);
        }
    }
}
