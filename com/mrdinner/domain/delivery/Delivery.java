package com.mrdinner.domain.delivery;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.staff.Courier;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a delivery
 */
public class Delivery {
    private final String deliveryId;
    private final Order order;
    private final Address pickupAddress;
    private final Address deliveryAddress;
    private final LocalDateTime requestedDeliveryTime;
    private DeliveryStatus status;
    private Courier assignedCourier;
    private LocalDateTime assignedTime;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private Money deliveryFee;
    private Money tip;
    private String deliveryNotes;
    private String failureReason;

    public Delivery(Order order, Address pickupAddress, Address deliveryAddress, 
                   LocalDateTime requestedDeliveryTime, Money deliveryFee) {
        this.deliveryId = UUID.randomUUID().toString();
        this.order = Objects.requireNonNull(order, "Order cannot be null");
        this.pickupAddress = Objects.requireNonNull(pickupAddress, "Pickup address cannot be null");
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "Delivery address cannot be null");
        this.requestedDeliveryTime = Objects.requireNonNull(requestedDeliveryTime, "Requested delivery time cannot be null");
        this.status = DeliveryStatus.PENDING;
        this.deliveryFee = Objects.requireNonNull(deliveryFee, "Delivery fee cannot be null");
        this.tip = Money.zero("USD");
        this.deliveryNotes = "";
        this.failureReason = "";
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public Order getOrder() {
        return order;
    }

    public Address getPickupAddress() {
        return pickupAddress;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public LocalDateTime getRequestedDeliveryTime() {
        return requestedDeliveryTime;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        if (!this.status.canTransitionTo(status)) {
            throw new IllegalArgumentException(
                String.format("Cannot transition from %s to %s", this.status, status));
        }
        this.status = status;
    }

    public Courier getAssignedCourier() {
        return assignedCourier;
    }

    public void assignCourier(Courier courier) {
        if (status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Can only assign courier to pending deliveries");
        }
        if (courier == null) {
            throw new IllegalArgumentException("Courier cannot be null");
        }
        if (!courier.canHandleOrder(order)) {
            throw new IllegalStateException("Courier cannot handle this order");
        }
        
        this.assignedCourier = courier;
        this.assignedTime = LocalDateTime.now();
        setStatus(DeliveryStatus.ASSIGNED);
    }

    public LocalDateTime getAssignedTime() {
        return assignedTime;
    }

    public LocalDateTime getPickupTime() {
        return pickupTime;
    }

    public void markPickedUp() {
        if (status != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException("Can only mark pickup for assigned deliveries");
        }
        if (assignedCourier == null) {
            throw new IllegalStateException("No courier assigned to delivery");
        }
        
        this.pickupTime = LocalDateTime.now();
        setStatus(DeliveryStatus.PICKED_UP);
    }

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }

    public void markInTransit() {
        if (status != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("Can only mark in transit for picked up deliveries");
        }
        setStatus(DeliveryStatus.IN_TRANSIT);
    }

    public void markDelivered(Money tip) {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("Can only mark delivered for in-transit deliveries");
        }
        
        this.deliveryTime = LocalDateTime.now();
        this.tip = tip != null ? tip : Money.zero("USD");
        setStatus(DeliveryStatus.DELIVERED);
        
        if (assignedCourier != null) {
            assignedCourier.completeDelivery(this, tip);
        }
    }

    public void markFailed(String reason) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot mark failed for final status: " + status);
        }
        
        this.failureReason = reason != null ? reason.trim() : "Unknown reason";
        setStatus(DeliveryStatus.FAILED);
    }

    public void markReturned() {
        if (status != DeliveryStatus.FAILED && status != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("Can only return failed or picked up deliveries");
        }
        setStatus(DeliveryStatus.RETURNED);
    }

    public void cancel() {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot cancel delivery in final status: " + status);
        }
        setStatus(DeliveryStatus.CANCELLED);
    }

    public Money getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Money deliveryFee) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot modify delivery fee for final status: " + status);
        }
        this.deliveryFee = Objects.requireNonNull(deliveryFee, "Delivery fee cannot be null");
    }

    public Money getTip() {
        return tip;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes != null ? deliveryNotes.trim() : "";
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isDeliveredOnTime() {
        if (deliveryTime == null || requestedDeliveryTime == null) {
            return false;
        }
        return !deliveryTime.isAfter(requestedDeliveryTime);
    }

    public long getDeliveryDurationMinutes() {
        if (pickupTime == null || deliveryTime == null) {
            return 0;
        }
        return java.time.Duration.between(pickupTime, deliveryTime).toMinutes();
    }

    public boolean hasCourier() {
        return assignedCourier != null;
    }

    public Money getTotalDeliveryCost() {
        return deliveryFee.add(tip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Delivery delivery = (Delivery) obj;
        return Objects.equals(deliveryId, delivery.deliveryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryId);
    }

    @Override
    public String toString() {
        return String.format("Delivery{id='%s', order='%s', status=%s, courier='%s'}", 
            deliveryId, order.getOrderId(), status, 
            assignedCourier != null ? assignedCourier.getFullName() : "None");
    }
}
