package com.mrdinner.domain.staff;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.delivery.Delivery;
import com.mrdinner.domain.order.Order;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete staff class representing a delivery courier
 */
public class Courier extends Staff {
    private String vehicleType;
    private String licenseNumber;
    private List<String> deliveryAreas;
    private int maxDeliveryRadius; // in miles
    private LocalDateTime lastDeliveryStart;
    private LocalDateTime lastDeliveryEnd;
    private int deliveriesCompleted;
    private Money totalTipsEarned;

    public Courier(String firstName, String lastName, String email, String phoneNumber, 
                  Address address, Money hourlyRate, String vehicleType, String licenseNumber) {
        super(firstName, lastName, email, phoneNumber, address, hourlyRate, "Delivery");
        this.vehicleType = validateAndTrim(vehicleType, "Vehicle type");
        this.licenseNumber = validateAndTrim(licenseNumber, "License number");
        this.deliveryAreas = new ArrayList<>();
        this.maxDeliveryRadius = 10; // default 10 miles
        this.deliveriesCompleted = 0;
        this.totalTipsEarned = Money.zero("USD");
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = validateAndTrim(vehicleType, "Vehicle type");
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = validateAndTrim(licenseNumber, "License number");
    }

    public List<String> getDeliveryAreas() {
        return new ArrayList<>(deliveryAreas);
    }

    public void addDeliveryArea(String area) {
        if (area != null && !area.trim().isEmpty()) {
            this.deliveryAreas.add(area.trim());
        }
    }

    public void removeDeliveryArea(String area) {
        this.deliveryAreas.remove(area);
    }

    public int getMaxDeliveryRadius() {
        return maxDeliveryRadius;
    }

    public void setMaxDeliveryRadius(int maxDeliveryRadius) {
        if (maxDeliveryRadius <= 0) {
            throw new IllegalArgumentException("Max delivery radius must be positive");
        }
        this.maxDeliveryRadius = maxDeliveryRadius;
    }

    public LocalDateTime getLastDeliveryStart() {
        return lastDeliveryStart;
    }

    public LocalDateTime getLastDeliveryEnd() {
        return lastDeliveryEnd;
    }

    public void startDelivery(Delivery delivery) {
        if (lastDeliveryStart != null && lastDeliveryEnd == null) {
            throw new IllegalStateException("Cannot start delivery while another delivery is active");
        }
        this.lastDeliveryStart = LocalDateTime.now();
        this.lastDeliveryEnd = null;
    }

    public void completeDelivery(Delivery delivery, Money tip) {
        if (lastDeliveryStart == null) {
            throw new IllegalStateException("No active delivery to complete");
        }
        this.lastDeliveryEnd = LocalDateTime.now();
        this.deliveriesCompleted++;
        
        if (tip != null && tip.getAmount().doubleValue() > 0) {
            this.totalTipsEarned = this.totalTipsEarned.add(tip);
        }
    }

    public boolean isOnDelivery() {
        return lastDeliveryStart != null && lastDeliveryEnd == null;
    }

    public double getCurrentDeliveryHours() {
        if (!isOnDelivery()) {
            return 0.0;
        }
        return java.time.Duration.between(lastDeliveryStart, LocalDateTime.now()).toMinutes() / 60.0;
    }

    public int getDeliveriesCompleted() {
        return deliveriesCompleted;
    }

    public Money getTotalTipsEarned() {
        return totalTipsEarned;
    }

    @Override
    public String getRole() {
        return "Courier";
    }

    @Override
    public String getResponsibilities() {
        return "Deliver orders to customers, maintain vehicle, collect payments, provide customer service";
    }

    /**
     * Check if this courier can deliver to a specific address
     */
    public boolean canDeliverTo(Address deliveryAddress) {
        if (!isActive() || isOnDelivery()) {
            return false;
        }
        
        // Check if address is within delivery areas
        String city = deliveryAddress.getCity();
        return deliveryAreas.stream()
            .anyMatch(area -> area.toLowerCase().equals(city.toLowerCase()));
    }

    /**
     * Check if this courier can handle a specific order
     */
    public boolean canHandleOrder(Order order) {
        // Temporary fix: always return true for demo purposes
        return isActive() && !isOnDelivery();
    }

    /**
     * Estimate delivery time in minutes
     */
    public int estimateDeliveryTime(Address deliveryAddress) {
        // Base delivery time calculation
        int baseTime = 30; // 30 minutes base
        
        // Adjust based on distance (simplified calculation)
        // In a real system, this would use actual distance calculation
        int distanceAdjustment = maxDeliveryRadius * 2; // rough estimate
        
        // Adjust based on vehicle type
        double vehicleMultiplier;
        switch (vehicleType.toLowerCase()) {
            case "bicycle":
                vehicleMultiplier = 1.5;
                break;
            case "motorcycle":
                vehicleMultiplier = 1.0;
                break;
            case "car":
                vehicleMultiplier = 0.8;
                break;
            default:
                vehicleMultiplier = 1.0;
                break;
        }
        
        return (int) Math.ceil((baseTime + distanceAdjustment) * vehicleMultiplier);
    }

    /**
     * Calculate total earnings including tips
     */
    public Money calculateTotalEarnings(double hoursWorked) {
        Money baseSalary = calculateSalary(hoursWorked);
        return baseSalary.add(totalTipsEarned);
    }

    @Override
    public String toString() {
        return String.format("Courier{id='%s', name='%s', vehicle='%s', deliveries=%d, onDelivery=%s}", 
            getStaffId(), getFullName(), vehicleType, deliveriesCompleted, isOnDelivery());
    }
}
