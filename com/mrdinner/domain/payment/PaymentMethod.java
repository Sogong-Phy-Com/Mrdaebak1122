package com.mrdinner.domain.payment;

/**
 * Enumeration representing different payment methods
 */
public enum PaymentMethod {
    CREDIT_CARD("Credit Card", "Visa, MasterCard, American Express"),
    DEBIT_CARD("Debit Card", "Bank debit card"),
    CASH("Cash", "Cash payment upon delivery"),
    PAYPAL("PayPal", "PayPal digital wallet"),
    APPLE_PAY("Apple Pay", "Apple mobile payment"),
    GOOGLE_PAY("Google Pay", "Google mobile payment"),
    BANK_TRANSFER("Bank Transfer", "Direct bank transfer"),
    GIFT_CARD("Gift Card", "Restaurant gift card");

    private final String displayName;
    private final String description;

    PaymentMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDigital() {
        return this != CASH;
    }

    public boolean requiresOnlineProcessing() {
        return this == CREDIT_CARD || this == DEBIT_CARD || this == PAYPAL || 
               this == APPLE_PAY || this == GOOGLE_PAY || this == BANK_TRANSFER;
    }

    public boolean isInstantPayment() {
        return this == CASH || this == APPLE_PAY || this == GOOGLE_PAY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
