package com.mrdinner.app;

import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.customer.Customer;
import com.mrdinner.domain.delivery.Delivery;
import com.mrdinner.domain.menu.*;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.payment.Payment;
import com.mrdinner.domain.payment.PaymentMethod;
import com.mrdinner.domain.staff.Cook;
import com.mrdinner.domain.staff.Courier;
import com.mrdinner.domain.inventory.StockItem;
import com.mrdinner.service.*;

import java.time.LocalDate;

/**
 * Main application class for Mr. Dinner service
 * Demonstrates the complete system functionality
 */
public class Main {
    private static OrderService orderService;
    private static InventoryService inventoryService;
    private static DeliveryService deliveryService;
    private static PricingService pricingService;

    public static void main(String[] args) {
        System.out.println("=== Mr. Dinner Service - Starting Application ===");
        
        // Initialize services
        initializeServices();
        
        // Create sample data
        createSampleData();
        
        // Demonstrate system functionality
        demonstrateSystem();
        
        System.out.println("\n=== Mr. Dinner Service - Application Completed ===");
    }

    private static void initializeServices() {
        System.out.println("\n--- Initializing Services ---");
        
        pricingService = new PricingService();
        inventoryService = new InventoryService();
        deliveryService = new DeliveryService();
        orderService = new OrderService(pricingService, deliveryService, inventoryService);
        
        System.out.println("Services initialized successfully");
    }

    private static void createSampleData() {
        System.out.println("\n--- Creating Sample Data ---");
        
        // Create sample staff
        createSampleStaff();
        
        // Create sample menu items
        createSampleMenuItems();
        
        System.out.println("Sample data created successfully");
    }

    private static void createSampleStaff() {
        System.out.println("Creating sample staff...");
        
        // Create cooks
        Address cookAddress = new Address("456 Kitchen St", "Chef City", "CC", "67890", "USA");
        Money cookRate = Money.of(20.00, "USD");
        
        Cook chefJohn = new Cook("John", "Chef", "john.chef@mrdinner.com", "5551234567", 
                               cookAddress, cookRate);
        chefJohn.addSpecialty("French Cuisine");
        chefJohn.addSpecialty("Valentine Special");
        chefJohn.setExperienceYears(5);
        
        // Create couriers
        Address courierAddress = new Address("789 Delivery Ave", "Driver Town", "DT", "54321", "USA");
        Money courierRate = Money.of(15.00, "USD");
        
        Courier mikeDriver = new Courier("Mike", "Driver", "mike.driver@mrdinner.com", "5559876543",
                                       courierAddress, courierRate, "Car", "DL123456");
        mikeDriver.addDeliveryArea("Downtown");
        mikeDriver.addDeliveryArea("Uptown");
        
        Courier sarahBike = new Courier("Sarah", "Cyclist", "sarah.cyclist@mrdinner.com", "5554567890",
                                      courierAddress, courierRate, "Bicycle", "DL789012");
        sarahBike.addDeliveryArea("Downtown");
        
        // Add staff to services
        orderService.addCourier(mikeDriver);
        orderService.addCourier(sarahBike);
        deliveryService.addCourier(mikeDriver);
        deliveryService.addCourier(sarahBike);
        
        System.out.println("Sample staff created: 1 cook, 2 couriers");
    }

    private static void createSampleMenuItems() {
        System.out.println("Creating sample menu items...");
        
        // Create individual menu items
        MenuItem appetizer = new MenuItem("Caesar Salad", "Fresh romaine lettuce with caesar dressing", 
                                        Money.of(8.99, "USD"), ItemType.APPETIZER);
        appetizer.setPreparationTimeMinutes(10);
        appetizer.setCalories(250);
        
        MenuItem mainCourse = new MenuItem("Grilled Salmon", "Fresh Atlantic salmon with herbs", 
                                         Money.of(24.99, "USD"), ItemType.MAIN_COURSE);
        mainCourse.setPreparationTimeMinutes(20);
        mainCourse.setCalories(350);
        
        MenuItem dessert = new MenuItem("Chocolate Cake", "Rich chocolate cake with vanilla ice cream", 
                                      Money.of(7.99, "USD"), ItemType.DESSERT);
        dessert.setPreparationTimeMinutes(5);
        dessert.setCalories(400);
        
        System.out.println("Sample menu items created: appetizer, main course, dessert");
    }

    private static void demonstrateSystem() {
        System.out.println("\n--- Demonstrating System Functionality ---");
        
        // Create a customer
        Customer customer = createSampleCustomer();
        
        // Create an order
        Order order = createSampleOrder(customer);
        
        // Add items to order
        addItemsToOrder(order);
        
        // Process the order
        processOrder(order);
        
        // Demonstrate delivery
        demonstrateDelivery(order);
        
        // Show inventory status
        showInventoryStatus();
        
        // Show delivery statistics
        showDeliveryStatistics();
    }

    private static Customer createSampleCustomer() {
        System.out.println("\nCreating sample customer...");
        
        Address customerAddress = new Address("123 Main St", "Downtown", "CC", "12345", "USA");
        Customer customer = new Customer("Alice Johnson", "alice.johnson@email.com", 
                                       "5551234567", customerAddress, "password123");
        
        System.out.println("Customer created: " + customer.getName());
        return customer;
    }

    private static Order createSampleOrder(Customer customer) {
        System.out.println("\nCreating sample order...");
        
        Address deliveryAddress = new Address("456 Oak Ave", "Delivery City", "DC", "67890", "USA");
        Order order = orderService.createOrder(customer, deliveryAddress);
        
        System.out.println("Order created: " + order.getOrderId());
        return order;
    }

    private static void addItemsToOrder(Order order) {
        System.out.println("\nAdding items to order...");
        
        // Create and add a Valentine dinner
        ValentineDinner valentineDinner = new ValentineDinner(
            "Romantic Valentine Dinner", 
            "Special romantic dinner for two with candlelight", 
            Money.ofKRW(85000),
            ServingStyle.GRAND
        );
        
        orderService.addDinnerToOrder(order, valentineDinner, 1);
        
        // Create and add a French dinner
        FrenchDinner frenchDinner = new FrenchDinner(
            "Classic French Dinner",
            "Traditional French cuisine with wine pairing",
            Money.ofKRW(95000),
            ServingStyle.DELUXE
        );
        
        orderService.addDinnerToOrder(order, frenchDinner, 1);
        
        // Add individual menu items
        MenuItem wine = new MenuItem("House Wine", "Premium red wine", 
                                   Money.ofKRW(19000), ItemType.BEVERAGE);
        wine.setPreparationTimeMinutes(2);
        
        MenuItem bread = new MenuItem("Artisan Bread", "Fresh baked artisan bread", 
                                    Money.ofKRW(5000), ItemType.BREAD);
        bread.setPreparationTimeMinutes(3);
        
        orderService.addMenuItemToOrder(order, wine, 2);
        orderService.addMenuItemToOrder(order, bread, 1);
        
        System.out.println("Items added to order:");
        System.out.println("- 1x Valentine Dinner");
        System.out.println("- 1x French Dinner");
        System.out.println("- 2x House Wine");
        System.out.println("- 1x Artisan Bread");
        
        System.out.println("Order total: " + order.getTotalAmount());
    }

    private static void processOrder(Order order) {
        System.out.println("\nProcessing order...");
        
        // Confirm the order
        orderService.confirmOrder(order);
        System.out.println("Order confirmed: " + order.getStatus());
        
        // Process payment
        Payment payment = orderService.processPayment(order, PaymentMethod.CREDIT_CARD, 
                                                    order.getCustomer().getEmail());
        System.out.println("Payment processed: " + payment.getStatus() + " - " + payment.getAmount());
        
        // Mark order as ready
        orderService.markOrderReady(order);
        System.out.println("Order ready for delivery: " + order.getStatus());
    }

    private static void demonstrateDelivery(Order order) {
        System.out.println("\nDemonstrating delivery process...");
        
        // Get the delivery for this order
        java.util.List<Delivery> deliveries = deliveryService.getDeliveriesByStatus(com.mrdinner.domain.delivery.DeliveryStatus.PENDING);
        if (!deliveries.isEmpty()) {
            Delivery delivery = deliveries.get(0);
            
            // Auto-assign courier
            deliveryService.autoAssignCouriers();
            
            // Simulate delivery process
            if (delivery.getAssignedCourier() != null) {
                deliveryService.markPickedUp(delivery);
                deliveryService.markInTransit(delivery);
                deliveryService.markDelivered(delivery, Money.of(8.00, "USD"));
                
                System.out.println("Delivery completed successfully!");
                System.out.println("Courier: " + delivery.getAssignedCourier().getFullName());
                System.out.println("Tip received: $8.00");
            }
        }
    }

    private static void showInventoryStatus() {
        System.out.println("\n--- Inventory Status ---");
        
        InventoryService.InventoryStats stats = inventoryService.getInventoryStats();
        System.out.println("Inventory Statistics: " + stats);
        
        java.util.List<StockItem> lowStockItems = inventoryService.getLowStockItems();
        if (!lowStockItems.isEmpty()) {
            System.out.println("Low stock items:");
            lowStockItems.forEach(item -> 
                System.out.println("- " + item.getItemName() + ": " + item.getCurrentQuantity() + " " + item.getUnit()));
        } else {
            System.out.println("All items have sufficient stock");
        }
    }

    private static void showDeliveryStatistics() {
        System.out.println("\n--- Delivery Statistics ---");
        
        DeliveryService.DeliveryStats stats = deliveryService.getDeliveryStats();
        System.out.println("Delivery Statistics: " + stats);
    }
}
