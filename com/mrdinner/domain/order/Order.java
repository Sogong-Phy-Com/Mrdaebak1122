package com.mrdinner.domain.order;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.customer.Customer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a customer order
 */
public class Order {
    private final String orderId;
    private final Customer customer;
    private final LocalDateTime orderTime;
    private OrderStatus status;
    private List<OrderItem> orderItems;
    private Money subtotal;
    private Money tax;
    private Money deliveryFee;
    private Money totalAmount;
    private Address deliveryAddress;
    private LocalDateTime estimatedDeliveryTime;
    private String notes;

    public Order(Customer customer, Address deliveryAddress) {
        this.orderId = UUID.randomUUID().toString();
        this.customer = Objects.requireNonNull(customer, "Customer cannot be null");
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "Delivery address cannot be null");
        this.orderTime = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
        this.orderItems = new ArrayList<>();
        this.subtotal = Money.zeroKRW();
        this.tax = Money.zeroKRW();
        this.deliveryFee = Money.zeroKRW();
        this.totalAmount = Money.zeroKRW();
        this.notes = "";
    }

    public String getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        if (!this.status.canTransitionTo(status)) {
            throw new IllegalArgumentException(
                String.format("Cannot transition from %s to %s", this.status, status));
        }
        this.status = status;
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

    public void addOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot modify order in final status: " + status);
        }
        this.orderItems.add(orderItem);
        recalculateTotals();
    }

    public void removeOrderItem(OrderItem orderItem) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot modify order in final status: " + status);
        }
        this.orderItems.remove(orderItem);
        recalculateTotals();
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getTax() {
        return tax;
    }

    public void setTax(Money tax) {
        this.tax = Objects.requireNonNull(tax, "Tax cannot be null");
        recalculateTotals();
    }

    public Money getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Money deliveryFee) {
        this.deliveryFee = Objects.requireNonNull(deliveryFee, "Delivery fee cannot be null");
        recalculateTotals();
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot modify order in final status: " + status);
        }
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "Delivery address cannot be null");
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
    }

    public int getTotalPreparationTimeMinutes() {
        return orderItems.stream()
            .mapToInt(OrderItem::getPreparationTimeMinutes)
            .sum();
    }

    public boolean hasItems() {
        return !orderItems.isEmpty();
    }

    public boolean isModifiable() {
        return !status.isFinal();
    }

    private void recalculateTotals() {
        this.subtotal = orderItems.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(Money.zeroKRW(), Money::add);
        
        this.totalAmount = subtotal.add(tax).add(deliveryFee);
    }

    // 주문 후 음식 항목 추가/변경/삭제 기능
    public void addMenuItemToOrder(com.mrdinner.domain.menu.MenuItem menuItem, int quantity) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot modify order in final status: " + status);
        }
        
        // 이미 같은 메뉴 아이템이 있는지 확인
        OrderItem existingItem = orderItems.stream()
            .filter(item -> item.getMenuItem().equals(menuItem))
            .findFirst()
            .orElse(null);
            
        if (existingItem != null) {
            // 기존 아이템의 수량 변경
            existingItem.updateQuantity(existingItem.getQuantity() + quantity);
        } else {
            // 새로운 주문 아이템 추가
            OrderItem newOrderItem = new OrderItem(menuItem, quantity);
            addOrderItem(newOrderItem);
        }
        
        recalculateTotals();
    }

    public void removeMenuItemFromOrder(com.mrdinner.domain.menu.MenuItem menuItem) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot modify order in final status: " + status);
        }
        
        OrderItem itemToRemove = orderItems.stream()
            .filter(item -> item.getMenuItem().equals(menuItem))
            .findFirst()
            .orElse(null);
            
        if (itemToRemove != null) {
            removeOrderItem(itemToRemove);
        }
    }

    public void updateMenuItemQuantity(com.mrdinner.domain.menu.MenuItem menuItem, int newQuantity) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot modify order in final status: " + status);
        }
        
        OrderItem existingItem = orderItems.stream()
            .filter(item -> item.getMenuItem().equals(menuItem))
            .findFirst()
            .orElse(null);
            
        if (existingItem != null) {
            if (newQuantity <= 0) {
                removeOrderItem(existingItem);
            } else {
                existingItem.updateQuantity(newQuantity);
                recalculateTotals();
            }
        }
    }

    // 주문 확정 시 고객의 주문 횟수 증가
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        if (!hasItems()) {
            throw new IllegalStateException("Cannot confirm order without items");
        }
        setStatus(OrderStatus.CONFIRMED);
        
        // 고객의 주문 횟수 증가 (단골 고객 할인용)
        customer.incrementOrderCount();
    }

    public void cancel() {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot cancel order in final status: " + status);
        }
        setStatus(OrderStatus.CANCELLED);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return String.format("Order{id='%s', customer='%s', status=%s, total=%s, items=%d}", 
            orderId, customer.getName(), status, totalAmount, orderItems.size());
    }
}
