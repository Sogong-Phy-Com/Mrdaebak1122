package com.mrdinner.domain.staff;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for all staff members
 */
public abstract class Staff {
    protected final String staffId;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String phoneNumber;
    protected Address address;
    protected LocalDate hireDate;
    protected Money hourlyRate;
    protected StaffStatus status;
    protected String department;

    protected Staff(String firstName, String lastName, String email, String phoneNumber, 
                   Address address, Money hourlyRate, String department) {
        this.staffId = UUID.randomUUID().toString();
        this.firstName = validateAndTrim(firstName, "First name");
        this.lastName = validateAndTrim(lastName, "Last name");
        this.email = validateEmail(email);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
        this.address = Objects.requireNonNull(address, "Address cannot be null");
        this.hireDate = LocalDate.now();
        this.hourlyRate = Objects.requireNonNull(hourlyRate, "Hourly rate cannot be null");
        this.status = StaffStatus.ACTIVE;
        this.department = validateAndTrim(department, "Department");
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    private String validateEmail(String email) {
        String trimmed = validateAndTrim(email, "Email");
        if (!trimmed.contains("@") || !trimmed.contains(".")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return trimmed;
    }

    private String validatePhoneNumber(String phoneNumber) {
        String trimmed = validateAndTrim(phoneNumber, "Phone number");
        if (!trimmed.matches("\\d{10,15}")) {
            throw new IllegalArgumentException("Phone number must contain 10-15 digits");
        }
        return trimmed;
    }

    public String getStaffId() {
        return staffId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = validateAndTrim(firstName, "First name");
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = validateAndTrim(lastName, "Last name");
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = validateEmail(email);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = validatePhoneNumber(phoneNumber);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = Objects.requireNonNull(address, "Address cannot be null");
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public Money getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Money hourlyRate) {
        this.hourlyRate = Objects.requireNonNull(hourlyRate, "Hourly rate cannot be null");
    }

    public StaffStatus getStatus() {
        return status;
    }

    public void setStatus(StaffStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = validateAndTrim(department, "Department");
    }

    public boolean isActive() {
        return status == StaffStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = StaffStatus.INACTIVE;
    }

    public void activate() {
        this.status = StaffStatus.ACTIVE;
    }

    /**
     * Calculate salary for given hours worked
     */
    public Money calculateSalary(double hoursWorked) {
        if (hoursWorked < 0) {
            throw new IllegalArgumentException("Hours worked cannot be negative");
        }
        return hourlyRate.multiply(hoursWorked);
    }

    /**
     * Get the staff member's role
     */
    public abstract String getRole();

    /**
     * Get specific responsibilities for this staff type
     */
    public abstract String getResponsibilities();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Staff staff = (Staff) obj;
        return Objects.equals(staffId, staff.staffId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(staffId);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', name='%s', role='%s', status=%s}", 
            getClass().getSimpleName(), staffId, getFullName(), getRole(), status);
    }

    public enum StaffStatus {
        ACTIVE, INACTIVE, ON_LEAVE, TERMINATED
    }
}
