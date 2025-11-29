package com.mrdinner.domain.payment;

import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.order.Order;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a payment
 */
public class Payment {
    private final String paymentId;
    private final Order order;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime paymentTime;
    private LocalDateTime processedTime;
    private String transactionId;
    private String cardLastFourDigits;
    private String cardBrand;
    private Money refundAmount;
    private LocalDateTime refundTime;
    private String refundReason;
    private String failureReason;
    private String customerEmail;

    public Payment(Order order, Money amount, PaymentMethod paymentMethod, String customerEmail) {
        this.paymentId = UUID.randomUUID().toString();
        this.order = Objects.requireNonNull(order, "Order cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
        this.customerEmail = validateAndTrim(customerEmail, "Customer email");
        this.status = PaymentStatus.PENDING;
        this.paymentTime = LocalDateTime.now();
        this.refundAmount = Money.zero(amount.getCurrency());
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Order getOrder() {
        return order;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        if (!this.status.canTransitionTo(status)) {
            throw new IllegalArgumentException(
                String.format("Cannot transition from %s to %s", this.status, status));
        }
        this.status = status;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public LocalDateTime getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(LocalDateTime processedTime) {
        this.processedTime = processedTime;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId != null ? transactionId.trim() : "";
    }

    public String getCardLastFourDigits() {
        return cardLastFourDigits;
    }

    public void setCardLastFourDigits(String cardLastFourDigits) {
        if (cardLastFourDigits != null && !cardLastFourDigits.matches("\\d{4}")) {
            throw new IllegalArgumentException("Card last four digits must be exactly 4 digits");
        }
        this.cardLastFourDigits = cardLastFourDigits;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand != null ? cardBrand.trim() : "";
    }

    public Money getRefundAmount() {
        return refundAmount;
    }

    public LocalDateTime getRefundTime() {
        return refundTime;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void processPayment(String transactionId) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Can only process pending or processing payments");
        }
        
        this.transactionId = transactionId;
        this.processedTime = LocalDateTime.now();
        setStatus(PaymentStatus.COMPLETED);
    }

    public void failPayment(String failureReason) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot fail payment in final status: " + status);
        }
        
        this.failureReason = failureReason != null ? failureReason.trim() : "Unknown failure";
        setStatus(PaymentStatus.FAILED);
    }

    public void refundPayment(Money refundAmount, String refundReason) {
        if (!status.canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded in status: " + status);
        }
        
        if (refundAmount == null || refundAmount.getAmount().doubleValue() <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        
        if (refundAmount.getAmount().compareTo(amount.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed original payment amount");
        }
        
        this.refundAmount = refundAmount;
        this.refundReason = refundReason != null ? refundReason.trim() : "";
        this.refundTime = LocalDateTime.now();
        
        if (refundAmount.equals(amount)) {
            setStatus(PaymentStatus.REFUNDED);
        } else {
            setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = validateAndTrim(customerEmail, "Customer email");
    }

    public boolean isSuccessful() {
        return status.isSuccessful();
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED || status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    public Money getNetAmount() {
        return amount.subtract(refundAmount);
    }

    public boolean isFullRefund() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean isPartialRefund() {
        return status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    public String getMaskedCardNumber() {
        if (cardLastFourDigits == null || cardLastFourDigits.isEmpty()) {
            return "****";
        }
        return "**** **** **** " + cardLastFourDigits;
    }

    public String getPaymentSummary() {
        return String.format("%s - %s (%s)", 
            amount, paymentMethod.getDisplayName(), status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Payment payment = (Payment) obj;
        return Objects.equals(paymentId, payment.paymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }

    @Override
    public String toString() {
        return String.format("Payment{id='%s', amount=%s, method=%s, status=%s}", 
            paymentId, amount, paymentMethod, status);
    }
}
