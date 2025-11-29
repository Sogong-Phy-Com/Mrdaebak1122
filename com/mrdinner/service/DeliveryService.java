package com.mrdinner.service;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.delivery.Delivery;
import com.mrdinner.domain.delivery.DeliveryStatus;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.staff.Courier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing delivery operations
 */
public class DeliveryService {
    private List<Delivery> deliveries;
    private List<Courier> couriers;

    public DeliveryService() {
        this.deliveries = new ArrayList<>();
        this.couriers = new ArrayList<>();
    }

    /**
     * Add a delivery to the system
     */
    public void addDelivery(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        
        deliveries.add(delivery);
        System.out.println("Added delivery: " + delivery.getDeliveryId());
    }

    /**
     * Assign a courier to a delivery
     */
    public void assignCourier(Delivery delivery, Courier courier) {
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        if (courier == null) {
            throw new IllegalArgumentException("Courier cannot be null");
        }
        
        delivery.assignCourier(courier);
        System.out.println(String.format("Assigned courier %s to delivery %s", 
            courier.getFullName(), delivery.getDeliveryId()));
    }

    /**
     * Mark delivery as picked up
     */
    public void markPickedUp(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        
        delivery.markPickedUp();
        System.out.println("Delivery picked up: " + delivery.getDeliveryId());
    }

    /**
     * Mark delivery as in transit
     */
    public void markInTransit(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        
        delivery.markInTransit();
        System.out.println("Delivery in transit: " + delivery.getDeliveryId());
    }

    /**
     * Mark delivery as completed
     */
    public void markDelivered(Delivery delivery, Money tip) {
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        
        delivery.markDelivered(tip);
        
        // Update order status
        delivery.getOrder().setStatus(com.mrdinner.domain.order.OrderStatus.DELIVERED);
        
        System.out.println(String.format("Delivery completed: %s (tip: %s)", 
            delivery.getDeliveryId(), tip != null ? tip : "None"));
    }

    /**
     * Mark delivery as failed
     */
    public void markFailed(Delivery delivery, String reason) {
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        
        delivery.markFailed(reason);
        System.out.println("Delivery failed: " + delivery.getDeliveryId() + " - " + reason);
    }

    /**
     * Cancel a delivery
     */
    public void cancelDelivery(Delivery delivery, String reason) {
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        
        delivery.cancel();
        System.out.println("Delivery cancelled: " + delivery.getDeliveryId() + " - " + reason);
    }

    /**
     * Get delivery by ID
     */
    public Optional<Delivery> getDeliveryById(String deliveryId) {
        return deliveries.stream()
            .filter(delivery -> delivery.getDeliveryId().equals(deliveryId))
            .findFirst();
    }

    /**
     * Get deliveries by status
     */
    public List<Delivery> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveries.stream()
            .filter(delivery -> delivery.getStatus() == status)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get deliveries for a specific courier
     */
    public List<Delivery> getDeliveriesByCourier(Courier courier) {
        return deliveries.stream()
            .filter(delivery -> courier.equals(delivery.getAssignedCourier()))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get pending deliveries that need courier assignment
     */
    public List<Delivery> getPendingDeliveries() {
        return deliveries.stream()
            .filter(delivery -> delivery.getStatus() == DeliveryStatus.PENDING)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get active deliveries
     */
    public List<Delivery> getActiveDeliveries() {
        return deliveries.stream()
            .filter(delivery -> delivery.getStatus().isActive())
            .toList();
    }

    /**
     * Add a courier to the system
     */
    public void addCourier(Courier courier) {
        if (courier == null) {
            throw new IllegalArgumentException("Courier cannot be null");
        }
        
        couriers.add(courier);
        System.out.println("Added courier: " + courier.getFullName());
    }

    /**
     * Get all couriers
     */
    public List<Courier> getAllCouriers() {
        return new ArrayList<>(couriers);
    }

    /**
     * Get available couriers
     */
    public List<Courier> getAvailableCouriers() {
        return couriers.stream()
            .filter(Courier::isActive)
            .filter(courier -> !courier.isOnDelivery())
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find the best courier for a delivery
     */
    public Optional<Courier> findBestCourier(Delivery delivery) {
        Address deliveryAddress = delivery.getDeliveryAddress();
        
        return getAvailableCouriers().stream()
            .filter(courier -> courier.canDeliverTo(deliveryAddress))
            .min((c1, c2) -> {
                // Simple comparison based on estimated delivery time
                int time1 = c1.estimateDeliveryTime(deliveryAddress);
                int time2 = c2.estimateDeliveryTime(deliveryAddress);
                return Integer.compare(time1, time2);
            });
    }

    /**
     * Auto-assign couriers to pending deliveries
     */
    public void autoAssignCouriers() {
        List<Delivery> pendingDeliveries = getPendingDeliveries();
        
        for (Delivery delivery : pendingDeliveries) {
            Optional<Courier> bestCourier = findBestCourier(delivery);
            if (bestCourier.isPresent()) {
                assignCourier(delivery, bestCourier.get());
            } else {
                System.out.println("No available courier found for delivery: " + delivery.getDeliveryId());
            }
        }
    }

    /**
     * Calculate delivery fee based on distance and order value
     */
    public Money calculateDeliveryFee(Address pickupAddress, Address deliveryAddress, Money orderValue) {
        // Simple delivery fee calculation
        Money baseFee = Money.of(4.99, "USD");
        
        // Free delivery for orders above $50
        if (orderValue.getAmount().doubleValue() >= 50.00) {
            return Money.zero("USD");
        }
        
        // Add distance-based fee (simplified)
        double distance = calculateDistance(pickupAddress, deliveryAddress);
        if (distance > 5.0) { // More than 5 miles
            return baseFee.add(Money.of(2.00, "USD"));
        }
        
        return baseFee;
    }

    /**
     * Estimate delivery time
     */
    public int estimateDeliveryTime(Address pickupAddress, Address deliveryAddress) {
        // Simplified estimation - in real system would use actual routing
        double distance = calculateDistance(pickupAddress, deliveryAddress);
        return (int) (distance * 3); // 3 minutes per mile
    }

    /**
     * Get delivery statistics
     */
    public DeliveryStats getDeliveryStats() {
        int totalDeliveries = deliveries.size();
        long completedDeliveries = deliveries.stream()
            .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
            .count();
        long failedDeliveries = deliveries.stream()
            .filter(d -> d.getStatus() == DeliveryStatus.FAILED)
            .count();
        long pendingDeliveries = deliveries.stream()
            .filter(d -> d.getStatus() == DeliveryStatus.PENDING)
            .count();
        
        return new DeliveryStats(totalDeliveries, (int) completedDeliveries, 
                               (int) failedDeliveries, (int) pendingDeliveries);
    }

    /**
     * Process delivery workflow
     */
    public void processDeliveryWorkflow(Delivery delivery) {
        switch (delivery.getStatus()) {
            case PENDING:
                autoAssignCouriers();
                break;
            case ASSIGNED:
                // Ready for pickup
                break;
            case PICKED_UP:
                markInTransit(delivery);
                break;
            case IN_TRANSIT:
                // Simulate delivery completion
                markDelivered(delivery, Money.of(5.00, "USD")); // Default tip
                break;
            default:
                // No action needed for final states
                break;
        }
    }

    private double calculateDistance(Address address1, Address address2) {
        // Simplified distance calculation - in real system would use geocoding
        // For demo purposes, return a random distance between 1-10 miles
        return Math.random() * 9 + 1;
    }

    /**
     * Data class for delivery statistics
     */
    public static class DeliveryStats {
        private final int totalDeliveries;
        private final int completedDeliveries;
        private final int failedDeliveries;
        private final int pendingDeliveries;

        public DeliveryStats(int totalDeliveries, int completedDeliveries, 
                           int failedDeliveries, int pendingDeliveries) {
            this.totalDeliveries = totalDeliveries;
            this.completedDeliveries = completedDeliveries;
            this.failedDeliveries = failedDeliveries;
            this.pendingDeliveries = pendingDeliveries;
        }

        public int getTotalDeliveries() { return totalDeliveries; }
        public int getCompletedDeliveries() { return completedDeliveries; }
        public int getFailedDeliveries() { return failedDeliveries; }
        public int getPendingDeliveries() { return pendingDeliveries; }

        public double getSuccessRate() {
            if (totalDeliveries == 0) return 0.0;
            return (double) completedDeliveries / totalDeliveries * 100;
        }

        @Override
        public String toString() {
            return String.format("DeliveryStats{total=%d, completed=%d, failed=%d, pending=%d, successRate=%.1f%%}", 
                totalDeliveries, completedDeliveries, failedDeliveries, pendingDeliveries, getSuccessRate());
        }
    }
}
