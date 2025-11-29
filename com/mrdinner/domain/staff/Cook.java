package com.mrdinner.domain.staff;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.menu.Dinner;
import com.mrdinner.domain.menu.MenuItem;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.order.OrderItem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete staff class representing a cook
 */
public class Cook extends Staff {
    private List<String> specialties;
    private List<String> certifications;
    private int experienceYears;
    private LocalDateTime lastShiftStart;
    private LocalDateTime lastShiftEnd;

    public Cook(String firstName, String lastName, String email, String phoneNumber, 
               Address address, Money hourlyRate) {
        super(firstName, lastName, email, phoneNumber, address, hourlyRate, "Kitchen");
        this.specialties = new ArrayList<>();
        this.certifications = new ArrayList<>();
        this.experienceYears = 0;
    }

    public List<String> getSpecialties() {
        return new ArrayList<>(specialties);
    }

    public void addSpecialty(String specialty) {
        if (specialty != null && !specialty.trim().isEmpty()) {
            this.specialties.add(specialty.trim());
        }
    }

    public void removeSpecialty(String specialty) {
        this.specialties.remove(specialty);
    }

    public List<String> getCertifications() {
        return new ArrayList<>(certifications);
    }

    public void addCertification(String certification) {
        if (certification != null && !certification.trim().isEmpty()) {
            this.certifications.add(certification.trim());
        }
    }

    public void removeCertification(String certification) {
        this.certifications.remove(certification);
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        if (experienceYears < 0) {
            throw new IllegalArgumentException("Experience years cannot be negative");
        }
        this.experienceYears = experienceYears;
    }

    public LocalDateTime getLastShiftStart() {
        return lastShiftStart;
    }

    public void startShift() {
        if (lastShiftStart != null && lastShiftEnd == null) {
            throw new IllegalStateException("Cannot start shift while another shift is active");
        }
        this.lastShiftStart = LocalDateTime.now();
        this.lastShiftEnd = null;
    }

    public void endShift() {
        if (lastShiftStart == null) {
            throw new IllegalStateException("No active shift to end");
        }
        this.lastShiftEnd = LocalDateTime.now();
    }

    public boolean isOnShift() {
        return lastShiftStart != null && lastShiftEnd == null;
    }

    public double getCurrentShiftHours() {
        if (!isOnShift()) {
            return 0.0;
        }
        return java.time.Duration.between(lastShiftStart, LocalDateTime.now()).toMinutes() / 60.0;
    }

    @Override
    public String getRole() {
        return "Cook";
    }

    @Override
    public String getResponsibilities() {
        return "Prepare food items, cook meals, maintain kitchen hygiene, follow recipes";
    }

    /**
     * Check if this cook can prepare a specific menu item
     */
    public boolean canPrepareMenuItem(MenuItem menuItem) {
        if (!isActive() || !isOnShift()) {
            return false;
        }
        
        // Check if cook has specialty in the item type
        String itemType = menuItem.getItemType().toString();
        return specialties.stream()
            .anyMatch(specialty -> specialty.toLowerCase().contains(itemType.toLowerCase()));
    }

    /**
     * Check if this cook can prepare a specific dinner
     */
    public boolean canPrepareDinner(Dinner dinner) {
        if (!isActive() || !isOnShift()) {
            return false;
        }
        
        // Check if cook has specialty in the dinner type
        String dinnerType = dinner.getDinnerType();
        return specialties.stream()
            .anyMatch(specialty -> specialty.toLowerCase().contains(dinnerType.toLowerCase()));
    }

    /**
     * Estimate preparation time for an order item
     */
    public int estimatePreparationTime(OrderItem orderItem) {
        int baseTime = orderItem.getPreparationTimeMinutes();
        
        // Adjust based on experience
        double experienceMultiplier = Math.max(0.5, 1.0 - (experienceYears * 0.05));
        
        // Check if cook has specialty
        boolean hasSpecialty = orderItem.isMenuItem() ? 
            canPrepareMenuItem(orderItem.getMenuItem()) : 
            canPrepareDinner(orderItem.getDinner());
        
        if (hasSpecialty) {
            experienceMultiplier *= 0.8; // 20% faster with specialty
        }
        
        return (int) Math.ceil(baseTime * experienceMultiplier);
    }

    /**
     * Prepare an order item
     */
    public void prepareOrderItem(OrderItem orderItem) {
        if (!canPrepareMenuItem(orderItem.isMenuItem() ? orderItem.getMenuItem() : null) &&
            !canPrepareDinner(orderItem.isDinner() ? orderItem.getDinner() : null)) {
            throw new IllegalStateException("Cook cannot prepare this order item");
        }
        
        // Simulate preparation process
        System.out.println(String.format("Cook %s is preparing: %s", getFullName(), orderItem.getItemName()));
    }

    @Override
    public String toString() {
        return String.format("Cook{id='%s', name='%s', specialties=%s, experience=%d years, onShift=%s}", 
            getStaffId(), getFullName(), specialties, experienceYears, isOnShift());
    }
}
