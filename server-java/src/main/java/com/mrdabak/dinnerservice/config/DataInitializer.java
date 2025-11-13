package com.mrdabak.dinnerservice.config;

import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DinnerTypeRepository dinnerTypeRepository;
    private final MenuItemRepository menuItemRepository;
    private final DinnerMenuItemRepository dinnerMenuItemRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(DinnerTypeRepository dinnerTypeRepository, MenuItemRepository menuItemRepository,
                          DinnerMenuItemRepository dinnerMenuItemRepository, UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.menuItemRepository = menuItemRepository;
        this.dinnerMenuItemRepository = dinnerMenuItemRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Delete and recreate admin account
        userRepository.findByEmail("admin@mrdabak.com").ifPresent(user -> userRepository.delete(user));
        createEmployeeAccount("admin@mrdabak.com", "admin123", "관리자", "서울시 강남구", "010-0000-0000", "admin");
        
        // Create employee accounts (check if they exist first)
        createEmployeeAccount("employee1@mrdabak.com", "emp123", "직원1", "서울시 강남구", "010-1111-1111", "employee");
        createEmployeeAccount("employee2@mrdabak.com", "emp123", "직원2", "서울시 강남구", "010-2222-2222", "employee");

        if (dinnerTypeRepository.count() > 0) {
            return; // Data already initialized
        }

        // Insert menu items
        MenuItem wine = new MenuItem(null, "와인", "Wine", 15000, "drink");
        MenuItem champagneItem = new MenuItem(null, "샴페인", "Champagne", 50000, "drink");
        MenuItem coffee = new MenuItem(null, "커피", "Coffee", 5000, "drink");
        MenuItem steak = new MenuItem(null, "스테이크", "Steak", 35000, "food");
        MenuItem salad = new MenuItem(null, "샐러드", "Salad", 12000, "food");
        MenuItem eggs = new MenuItem(null, "에그 스크램블", "Scrambled Eggs", 8000, "food");
        MenuItem bacon = new MenuItem(null, "베이컨", "Bacon", 10000, "food");
        MenuItem bread = new MenuItem(null, "빵", "Bread", 5000, "food");
        MenuItem baguette = new MenuItem(null, "바게트빵", "Baguette", 6000, "food");
        
        wine = menuItemRepository.save(wine);
        champagneItem = menuItemRepository.save(champagneItem);
        coffee = menuItemRepository.save(coffee);
        steak = menuItemRepository.save(steak);
        salad = menuItemRepository.save(salad);
        eggs = menuItemRepository.save(eggs);
        bacon = menuItemRepository.save(bacon);
        bread = menuItemRepository.save(bread);
        baguette = menuItemRepository.save(baguette);

        // Insert dinner types
        DinnerType valentine = new DinnerType(null, "발렌타인 디너", "Valentine Dinner", 60000,
                "와인과 스테이크가 하트 모양 접시와 큐피드 장식과 함께 제공");
        DinnerType french = new DinnerType(null, "프렌치 디너", "French Dinner", 70000,
                "커피, 와인, 샐러드, 스테이크 제공");
        DinnerType english = new DinnerType(null, "잉글리시 디너", "English Dinner", 65000,
                "에그 스크램블, 베이컨, 빵, 스테이크 제공");
        DinnerType champagneDinner = new DinnerType(null, "샴페인 축제 디너", "Champagne Feast Dinner", 120000,
                "2인 식사, 샴페인 1병, 바게트빵 4개, 커피 포트 1개, 와인, 스테이크");
        
        valentine = dinnerTypeRepository.save(valentine);
        french = dinnerTypeRepository.save(french);
        english = dinnerTypeRepository.save(english);
        champagneDinner = dinnerTypeRepository.save(champagneDinner);

        // Insert dinner menu items
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, valentine.getId(), wine.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, valentine.getId(), steak.getId(), 1));

        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), coffee.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), wine.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), salad.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, french.getId(), steak.getId(), 1));

        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), eggs.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), bacon.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), bread.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, english.getId(), steak.getId(), 1));

        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), champagneItem.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), baguette.getId(), 4));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), coffee.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), wine.getId(), 1));
        dinnerMenuItemRepository.save(new DinnerMenuItem(null, champagneDinner.getId(), steak.getId(), 1));

        System.out.println("Initial data seeded successfully");
    }

    private void createEmployeeAccount(String email, String password, String name, String address, String phone, String role) {
        if (!userRepository.existsByEmail(email)) {
            User employee = new User();
            employee.setEmail(email);
            employee.setPassword(passwordEncoder.encode(password));
            employee.setName(name);
            employee.setAddress(address);
            employee.setPhone(phone);
            employee.setRole(role);
            userRepository.save(employee);
        }
    }
}

