package com.mrdinner.service;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.customer.Customer;
import com.mrdinner.domain.delivery.Delivery;
import com.mrdinner.domain.delivery.DeliveryStatus;
import com.mrdinner.domain.menu.Dinner;
import com.mrdinner.domain.menu.MenuItem;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.order.OrderItem;
import com.mrdinner.domain.order.OrderStatus;
import com.mrdinner.domain.payment.Payment;
import com.mrdinner.domain.payment.PaymentMethod;
import com.mrdinner.domain.payment.PaymentStatus;
import com.mrdinner.domain.staff.Courier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing orders and order-related operations
 */
public class OrderService {
    private final PricingService pricingService;
    private final DeliveryService deliveryService;
    private final InventoryService inventoryService;
    
    private List<Order> orders;
    private List<Courier> availableCouriers;

    public OrderService(PricingService pricingService, DeliveryService deliveryService, 
                       InventoryService inventoryService) {
        this.pricingService = pricingService;
        this.deliveryService = deliveryService;
        this.inventoryService = inventoryService;
        this.orders = new ArrayList<>();
        this.availableCouriers = new ArrayList<>();
    }

    /**
     * Create a new order
     */
    public Order createOrder(Customer customer, Address deliveryAddress) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (deliveryAddress == null) {
            throw new IllegalArgumentException("Delivery address cannot be null");
        }

        Order order = new Order(customer, deliveryAddress);
        orders.add(order);
        
        System.out.println("Created new order: " + order.getOrderId());
        return order;
    }

    /**
     * Add a menu item to the order
     */
    public void addMenuItemToOrder(Order order, MenuItem menuItem, int quantity) {
        validateOrderModifiable(order);
        validateMenuItemAvailable(menuItem);
        
        OrderItem orderItem = new OrderItem(menuItem, quantity);
        order.addOrderItem(orderItem);
        
        // Update order pricing
        updateOrderPricing(order);
        
        System.out.println(String.format("Added %dx %s to order %s", 
            quantity, menuItem.getName(), order.getOrderId()));
    }

    /**
     * Add a dinner to the order
     */
    public void addDinnerToOrder(Order order, Dinner dinner, int quantity) {
        validateOrderModifiable(order);
        validateDinnerAvailable(dinner);
        
        OrderItem orderItem = new OrderItem(dinner, quantity);
        order.addOrderItem(orderItem);
        
        // Update order pricing
        updateOrderPricing(order);
        
        System.out.println(String.format("Added %dx %s to order %s", 
            quantity, dinner.getName(), order.getOrderId()));
    }

    /**
     * Remove an item from the order
     */
    public void removeItemFromOrder(Order order, OrderItem orderItem) {
        validateOrderModifiable(order);
        
        order.removeOrderItem(orderItem);
        updateOrderPricing(order);
        
        System.out.println(String.format("Removed %s from order %s", 
            orderItem.getItemName(), order.getOrderId()));
    }

    /**
     * Confirm the order
     */
    public void confirmOrder(Order order) {
        validateOrderConfirmable(order);
        
        // Check inventory availability
        validateInventoryAvailability(order);
        
        // Update inventory
        consumeInventoryForOrder(order);
        
        order.confirm();
        
        System.out.println("Order confirmed: " + order.getOrderId());
    }

    /**
     * Process payment for the order
     */
    public Payment processPayment(Order order, PaymentMethod paymentMethod, String customerEmail) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be confirmed before payment");
        }
        
        Money orderTotal = order.getTotalAmount();
        Payment payment = new Payment(order, orderTotal, paymentMethod, customerEmail);
        
        // Simulate payment processing
        processPaymentTransaction(payment);
        
        if (payment.isSuccessful()) {
            order.setStatus(OrderStatus.PREPARING);
            System.out.println("Payment processed successfully for order: " + order.getOrderId());
        } else {
            System.out.println("Payment failed for order: " + order.getOrderId());
        }
        
        return payment;
    }

    /**
     * Mark order as ready for pickup/delivery
     */
    public void markOrderReady(Order order) {
        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Order must be in preparing status");
        }
        
        order.setStatus(OrderStatus.READY);
        
        // Create delivery if needed
        if (needsDelivery(order)) {
            createDeliveryForOrder(order);
        }
        
        System.out.println("Order ready: " + order.getOrderId());
    }

    /**
     * Cancel an order
     */
    public void cancelOrder(Order order, String reason) {
        validateOrderCancellable(order);
        
        // Restore inventory if order was confirmed
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING) {
            restoreInventoryForOrder(order);
        }
        
        order.cancel();
        
        System.out.println("Order cancelled: " + order.getOrderId() + " - " + reason);
    }

    /**
     * Get order by ID
     */
    public Optional<Order> getOrderById(String orderId) {
        return orders.stream()
            .filter(order -> order.getOrderId().equals(orderId))
            .findFirst();
    }

    /**
     * Get all orders for a customer
     */
    public List<Order> getOrdersByCustomer(Customer customer) {
        return orders.stream()
            .filter(order -> order.getCustomer().equals(customer))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get orders by status
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orders.stream()
            .filter(order -> order.getStatus() == status)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Estimate order preparation time
     */
    public int estimatePreparationTime(Order order) {
        return order.getTotalPreparationTimeMinutes();
    }

    /**
     * Add available courier
     */
    public void addCourier(Courier courier) {
        availableCouriers.add(courier);
    }

    /**
     * Get available couriers
     */
    public List<Courier> getAvailableCouriers() {
        return availableCouriers.stream()
            .filter(Courier::isActive)
            .filter(courier -> !courier.isOnDelivery())
            .collect(java.util.stream.Collectors.toList());
    }

    private void validateOrderModifiable(Order order) {
        if (!order.isModifiable()) {
            throw new IllegalStateException("Order cannot be modified in current status: " + order.getStatus());
        }
    }

    private void validateMenuItemAvailable(MenuItem menuItem) {
        if (!menuItem.isAvailable()) {
            throw new IllegalStateException("Menu item is not available: " + menuItem.getName());
        }
    }

    private void validateDinnerAvailable(Dinner dinner) {
        if (!dinner.isAvailable()) {
            throw new IllegalStateException("Dinner is not available: " + dinner.getName());
        }
    }

    private void validateOrderConfirmable(Order order) {
        if (!order.hasItems()) {
            throw new IllegalStateException("Order must have items before confirmation");
        }
        
        Money subtotal = pricingService.calculateSubtotal(order);
        if (!pricingService.meetsMinimumOrderAmount(subtotal)) {
            throw new IllegalStateException("Order does not meet minimum amount requirement");
        }
    }

    private void validateInventoryAvailability(Order order) {
        // This would check if all required ingredients are available
        // For now, we'll assume inventory is always available
        System.out.println("Validating inventory availability for order: " + order.getOrderId());
    }

    private void consumeInventoryForOrder(Order order) {
        // This would consume inventory items based on the order
        // For now, we'll just log the action
        System.out.println("Consuming inventory for order: " + order.getOrderId());
    }

    private void restoreInventoryForOrder(Order order) {
        // This would restore inventory items that were consumed
        System.out.println("Restoring inventory for cancelled order: " + order.getOrderId());
    }

    private void updateOrderPricing(Order order) {
        PricingService.OrderPricing pricing = pricingService.calculateOrderPricing(order);
        order.setTax(pricing.getTax());
        order.setDeliveryFee(pricing.getDeliveryFee());
    }

    private void processPaymentTransaction(Payment payment) {
        // Simulate payment processing
        // In a real system, this would integrate with payment gateways
        
        if (payment.getPaymentMethod().isInstantPayment()) {
            payment.processPayment("TXN-" + System.currentTimeMillis());
        } else {
            // Simulate processing delay
            payment.setStatus(PaymentStatus.PROCESSING);
            // Simulate success after processing
            payment.processPayment("TXN-" + System.currentTimeMillis());
        }
    }

    private boolean needsDelivery(Order order) {
        // All orders need delivery in this system
        return true;
    }

    private void createDeliveryForOrder(Order order) {
        Address pickupAddress = new Address("123 Restaurant St", "Food City", "FC", "12345", "USA");
        LocalDateTime deliveryTime = LocalDateTime.now().plusHours(1);
        Money deliveryFee = order.getDeliveryFee();
        
        Delivery delivery = new Delivery(order, pickupAddress, order.getDeliveryAddress(), 
                                       deliveryTime, deliveryFee);
        
        // Assign courier if available
        Optional<Courier> availableCourier = getAvailableCouriers().stream().findFirst();
        if (availableCourier.isPresent()) {
            delivery.assignCourier(availableCourier.get());
        }
        
        deliveryService.addDelivery(delivery);
    }

    private void validateOrderCancellable(Order order) {
        if (order.getStatus().isFinal()) {
            throw new IllegalStateException("Cannot cancel order in final status: " + order.getStatus());
        }
    }
}
