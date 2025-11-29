package com.mrdinner.domain.order;

/**
 * Enumeration representing the status of an order
 */
public enum OrderStatus {
    PENDING("Order received and pending confirmation"),
    CONFIRMED("Order confirmed and being prepared"),
    PREPARING("Order is being prepared by kitchen staff"),
    READY("Order is ready for pickup/delivery"),
    OUT_FOR_DELIVERY("Order is being delivered to customer"),
    DELIVERED("Order has been successfully delivered"),
    CANCELLED("Order was cancelled"),
    REFUNDED("Order was refunded");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                return newStatus == PREPARING || newStatus == CANCELLED;
            case PREPARING:
                return newStatus == READY || newStatus == CANCELLED;
            case READY:
                return newStatus == OUT_FOR_DELIVERY || newStatus == CANCELLED;
            case OUT_FOR_DELIVERY:
                return newStatus == DELIVERED || newStatus == CANCELLED;
            case DELIVERED:
                return newStatus == REFUNDED;
            case CANCELLED:
                return false; // Cannot transition from cancelled
            case REFUNDED:
                return false; // Cannot transition from refunded
            default:
                return false;
        }
    }

    public boolean isActive() {
        return this != CANCELLED && this != REFUNDED && this != DELIVERED;
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }

    @Override
    public String toString() {
        return name().replace("_", " ").toLowerCase();
    }
}

