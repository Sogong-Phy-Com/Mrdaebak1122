package com.mrdinner.domain.delivery;

/**
 * Enumeration representing the status of a delivery
 */
public enum DeliveryStatus {
    PENDING("Delivery request pending assignment"),
    ASSIGNED("Delivery assigned to courier"),
    PICKED_UP("Order picked up from restaurant"),
    IN_TRANSIT("Order being delivered to customer"),
    DELIVERED("Order successfully delivered"),
    FAILED("Delivery failed - unable to complete"),
    RETURNED("Order returned to restaurant"),
    CANCELLED("Delivery was cancelled");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(DeliveryStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == ASSIGNED || newStatus == CANCELLED;
            case ASSIGNED:
                return newStatus == PICKED_UP || newStatus == CANCELLED;
            case PICKED_UP:
                return newStatus == IN_TRANSIT || newStatus == RETURNED || newStatus == CANCELLED;
            case IN_TRANSIT:
                return newStatus == DELIVERED || newStatus == FAILED || newStatus == CANCELLED;
            case DELIVERED:
                return false; // Cannot transition from delivered
            case FAILED:
                return newStatus == RETURNED || newStatus == CANCELLED;
            case RETURNED:
                return newStatus == PICKED_UP || newStatus == CANCELLED;
            case CANCELLED:
                return false; // Cannot transition from cancelled
            default:
                return false;
        }
    }

    public boolean isActive() {
        return this != DELIVERED && this != FAILED && this != CANCELLED;
    }

    public boolean isFinal() {
        return this == DELIVERED || this == FAILED || this == CANCELLED;
    }

    public boolean requiresCourier() {
        return this == ASSIGNED || this == PICKED_UP || this == IN_TRANSIT;
    }

    @Override
    public String toString() {
        return name().replace("_", " ").toLowerCase();
    }
}
