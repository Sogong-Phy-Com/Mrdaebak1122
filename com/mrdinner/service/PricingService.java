package com.mrdinner.service;

import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.menu.Dinner;
import com.mrdinner.domain.menu.MenuItem;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.order.OrderItem;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service for handling pricing calculations and business rules
 */
public class PricingService {
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.08"); // 8% tax
    private static final Money MINIMUM_ORDER_AMOUNT = Money.ofKRW(15000);
    private static final Money FREE_DELIVERY_THRESHOLD = Money.ofKRW(50000);
    private static final Money STANDARD_DELIVERY_FEE = Money.ofKRW(5000);

    /**
     * Calculate the total price for an order including tax and delivery fees
     */
    public OrderPricing calculateOrderPricing(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        Money subtotal = calculateSubtotal(order);
        Money tax = calculateTax(subtotal);
        Money deliveryFee = calculateDeliveryFee(subtotal);
        Money total = subtotal.add(tax).add(deliveryFee);

        return new OrderPricing(subtotal, tax, deliveryFee, total);
    }

    /**
     * Calculate subtotal for all items in the order
     */
    public Money calculateSubtotal(Order order) {
        return order.getOrderItems().stream()
            .map(OrderItem::getTotalPrice)
            .reduce(Money.zeroKRW(), Money::add);
    }

    /**
     * Calculate tax for the given amount
     */
    public Money calculateTax(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        return amount.multiply(DEFAULT_TAX_RATE);
    }

    /**
     * Calculate delivery fee based on order amount and business rules
     */
    public Money calculateDeliveryFee(Money orderAmount) {
        if (orderAmount == null) {
            throw new IllegalArgumentException("Order amount cannot be null");
        }

        // Free delivery for orders above threshold
        if (orderAmount.getAmount().compareTo(FREE_DELIVERY_THRESHOLD.getAmount()) >= 0) {
            return Money.zeroKRW();
        }

        return STANDARD_DELIVERY_FEE;
    }

    /**
     * Calculate special pricing for dinner packages
     */
    public Money calculateDinnerPrice(Dinner dinner, int quantity) {
        if (dinner == null) {
            throw new IllegalArgumentException("Dinner cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Money basePrice = dinner.calculateTotalPrice();
        
        // Apply volume discount for multiple dinners
        BigDecimal discountMultiplier = calculateVolumeDiscount(quantity);
        
        return basePrice.multiply(discountMultiplier).multiply(quantity);
    }

    /**
     * Apply volume discount based on quantity
     */
    private BigDecimal calculateVolumeDiscount(int quantity) {
        if (quantity >= 10) {
            return new BigDecimal("0.85"); // 15% discount for 10+ items
        } else if (quantity >= 5) {
            return new BigDecimal("0.90"); // 10% discount for 5+ items
        } else if (quantity >= 3) {
            return new BigDecimal("0.95"); // 5% discount for 3+ items
        }
        return BigDecimal.ONE; // No discount
    }

    /**
     * Validate if order meets minimum order amount
     */
    public boolean meetsMinimumOrderAmount(Money orderAmount) {
        if (orderAmount == null) {
            return false;
        }
        return orderAmount.getAmount().compareTo(MINIMUM_ORDER_AMOUNT.getAmount()) >= 0;
    }

    /**
     * Calculate tip amount based on order total and tip percentage
     */
    public Money calculateTip(Money orderTotal, BigDecimal tipPercentage) {
        if (orderTotal == null) {
            throw new IllegalArgumentException("Order total cannot be null");
        }
        if (tipPercentage == null || tipPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tip percentage must be non-negative");
        }
        
        return orderTotal.multiply(tipPercentage);
    }

    /**
     * Get standard tip percentages
     */
    public List<BigDecimal> getStandardTipPercentages() {
        return List.of(
            new BigDecimal("0.15"), // 15%
            new BigDecimal("0.18"), // 18%
            new BigDecimal("0.20")  // 20%
        );
    }

    /**
     * Apply promotional discount
     */
    public Money applyPromotionalDiscount(Money amount, BigDecimal discountPercentage, String promoCode) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount percentage must be non-negative");
        }
        if (discountPercentage.compareTo(new BigDecimal("1.0")) > 0) {
            throw new IllegalArgumentException("Discount percentage cannot exceed 100%");
        }

        Money discount = amount.multiply(discountPercentage);
        Money discountedAmount = amount.subtract(discount);
        
        // Ensure discounted amount is not negative
        if (discountedAmount.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return Money.zero(amount.getCurrency());
        }
        
        return discountedAmount;
    }

    /**
     * Data class for order pricing breakdown
     */
    public static class OrderPricing {
        private final Money subtotal;
        private final Money tax;
        private final Money deliveryFee;
        private final Money total;

        public OrderPricing(Money subtotal, Money tax, Money deliveryFee, Money total) {
            this.subtotal = subtotal;
            this.tax = tax;
            this.deliveryFee = deliveryFee;
            this.total = total;
        }

        public Money getSubtotal() {
            return subtotal;
        }

        public Money getTax() {
            return tax;
        }

        public Money getDeliveryFee() {
            return deliveryFee;
        }

        public Money getTotal() {
            return total;
        }

        public boolean hasDeliveryFee() {
            return deliveryFee.getAmount().compareTo(BigDecimal.ZERO) > 0;
        }

        @Override
        public String toString() {
            return String.format("OrderPricing{subtotal=%s, tax=%s, deliveryFee=%s, total=%s}", 
                subtotal, tax, deliveryFee, total);
        }
    }
}
