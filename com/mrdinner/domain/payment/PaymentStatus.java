package com.mrdinner.domain.payment;

/**
 * Enumeration representing the status of a payment
 */
public enum PaymentStatus {
    PENDING("Payment is pending processing"),
    PROCESSING("Payment is being processed"),
    COMPLETED("Payment has been successfully completed"),
    FAILED("Payment processing failed"),
    REFUNDED("Payment has been refunded"),
    PARTIALLY_REFUNDED("Payment has been partially refunded"),
    CANCELLED("Payment was cancelled"),
    EXPIRED("Payment authorization has expired");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(PaymentStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == PROCESSING || newStatus == COMPLETED || 
                       newStatus == FAILED || newStatus == CANCELLED || newStatus == EXPIRED;
            case PROCESSING:
                return newStatus == COMPLETED || newStatus == FAILED || newStatus == CANCELLED;
            case COMPLETED:
                return newStatus == REFUNDED || newStatus == PARTIALLY_REFUNDED;
            case FAILED:
                return newStatus == PROCESSING; // Retry payment
            case REFUNDED:
            case PARTIALLY_REFUNDED:
                return false; // Cannot transition from refunded states
            case CANCELLED:
                return newStatus == PROCESSING; // Retry payment
            case EXPIRED:
                return newStatus == PROCESSING; // Retry payment
            default:
                return false;
        }
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == REFUNDED || this == PARTIALLY_REFUNDED || 
               this == CANCELLED || this == EXPIRED;
    }

    public boolean isPending() {
        return this == PENDING || this == PROCESSING;
    }

    public boolean canBeRefunded() {
        return this == COMPLETED;
    }

    @Override
    public String toString() {
        return name().replace("_", " ").toLowerCase();
    }
}
