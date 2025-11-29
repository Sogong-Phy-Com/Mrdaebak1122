package com.mrdinner.gui;

import com.mrdinner.service.*;
import com.mrdinner.domain.customer.Customer;
import com.mrdinner.domain.order.Order;
import com.mrdinner.domain.common.Address;
import com.mrdinner.domain.common.Money;
import com.mrdinner.domain.menu.*;
import com.mrdinner.domain.inventory.StockItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Main GUI for Mr. Dinner Service
 */
public class MainGUI extends JFrame {
    private OrderService orderService;
    private InventoryService inventoryService;
    private DeliveryService deliveryService;
    private PricingService pricingService;
    
    private JTabbedPane tabbedPane;
    private JTextArea logArea;
    
    public MainGUI() {
        initializeServices();
        initializeGUI();
        setupEventHandlers();
    }
    
    private void initializeServices() {
        // Initialize all services
        inventoryService = new InventoryService();
        deliveryService = new DeliveryService();
        pricingService = new PricingService();
        orderService = new OrderService(pricingService, deliveryService, inventoryService);
        
        // Add sample data
        createSampleData();
        
        logMessage("서비스가 성공적으로 초기화되었습니다!");
    }
    
    private void initializeGUI() {
        setTitle("미스터 대박 디너 서비스 - 관리 시스템");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Create main layout
        setLayout(new BorderLayout());
        
        // Create header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Create tabbed pane for different sections
        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Add tabs
        tabbedPane.addTab("대시보드", createDashboardPanel());
        tabbedPane.addTab("주문 관리", createOrdersPanel());
        tabbedPane.addTab("메뉴 관리", createMenuPanel());
        tabbedPane.addTab("고객 관리", createCustomersPanel());
        tabbedPane.addTab("재고 관리", createInventoryPanel());
        tabbedPane.addTab("배달 관리", createDeliveryPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Create log panel
        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(41, 128, 185));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("미스터 대박 디너 서비스");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("특별한 날을 위한 프리미엄 디너 배달 관리 시스템");
        subtitleLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(200, 200, 200));
        
        panel.setLayout(new BorderLayout());
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(subtitleLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create stats panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        statsPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Add stat cards
        statsPanel.add(createStatCard("총 주문", "12", "오늘 처리된 주문"));
        statsPanel.add(createStatCard("매출", "₩2,450,000", "오늘의 수익"));
        statsPanel.add(createStatCard("배달", "8", "완료된 배달"));
        statsPanel.add(createStatCard("고객", "156", "활성 고객"));
        statsPanel.add(createStatCard("메뉴 항목", "24", "사용 가능한 항목"));
        statsPanel.add(createStatCard("재고", "95%", "재고 수준"));
        
        panel.add(statsPanel, BorderLayout.NORTH);
        
        // Create quick actions panel
        JPanel actionsPanel = createQuickActionsPanel();
        panel.add(actionsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatCard(String title, String value, String description) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setForeground(new Color(100, 100, 100));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        valueLabel.setForeground(new Color(41, 128, 185));
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        descLabel.setForeground(new Color(150, 150, 150));
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(descLabel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 15));
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        // Quick action buttons
        JButton newOrderBtn = createActionButton("새 주문", "새로운 고객 주문 생성", new Color(46, 204, 113));
        JButton viewMenuBtn = createActionButton("메뉴 보기", "사용 가능한 메뉴 항목 둘러보기", new Color(155, 89, 182));
        JButton manageInventoryBtn = createActionButton("재고 관리", "재고 수준 업데이트", new Color(230, 126, 34));
        
        panel.add(newOrderBtn);
        panel.add(viewMenuBtn);
        panel.add(manageInventoryBtn);
        
        return panel;
    }
    
    private JButton createActionButton(String text, String tooltip, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(new EmptyBorder(20, 20, 20, 20));
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        
        button.addActionListener(e -> {
            if (text.contains("새 주문")) {
                tabbedPane.setSelectedIndex(1); // Switch to Orders tab
            } else if (text.contains("메뉴 보기")) {
                tabbedPane.setSelectedIndex(2); // Switch to Menu tab
            } else if (text.contains("재고 관리")) {
                tabbedPane.setSelectedIndex(4); // Switch to Inventory tab
            }
        });
        
        return button;
    }
    
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create order form
        JPanel formPanel = createOrderForm();
        panel.add(formPanel, BorderLayout.NORTH);
        
        // Create orders list
        JPanel listPanel = createOrdersList();
        panel.add(listPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createOrderForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("새 주문 생성"));
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Customer info
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("고객명:"), gbc);
        gbc.gridx = 1;
        JTextField customerNameField = new JTextField(20);
        panel.add(customerNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("전화번호:"), gbc);
        gbc.gridx = 1;
        JTextField phoneField = new JTextField(20);
        panel.add(phoneField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("주소:"), gbc);
        gbc.gridx = 1;
        JTextField addressField = new JTextField(20);
        panel.add(addressField, gbc);
        
        // Menu selection
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("디너 메뉴:"), gbc);
        gbc.gridx = 1;
        String[] menuItems = {"발렌타인 디너", "프렌치 디너", "잉글리시 디너", "샴페인 축제 디너"};
        JComboBox<String> menuCombo = new JComboBox<>(menuItems);
        panel.add(menuCombo, gbc);
        
        // Serving style selection
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("서빙 스타일:"), gbc);
        gbc.gridx = 1;
        String[] servingStyles = {"심플", "그랜드", "디럭스"};
        JComboBox<String> servingStyleCombo = new JComboBox<>(servingStyles);
        panel.add(servingStyleCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("수량:"), gbc);
        gbc.gridx = 1;
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        panel.add(quantitySpinner, gbc);
        
        // Create order button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JButton createOrderBtn = new JButton("주문 생성");
        createOrderBtn.setBackground(new Color(46, 204, 113));
        createOrderBtn.setForeground(Color.WHITE);
        createOrderBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        panel.add(createOrderBtn, gbc);
        
        // Add action listener
        createOrderBtn.addActionListener(e -> createNewOrder(customerNameField.getText(), 
            phoneField.getText(), addressField.getText(), 
            (String) menuCombo.getSelectedItem(), 
            (String) servingStyleCombo.getSelectedItem(),
            (Integer) quantitySpinner.getValue()));
        
        return panel;
    }
    
    private JPanel createOrdersList() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("최근 주문"));
        panel.setBackground(Color.WHITE);
        
        String[] columns = {"주문 ID", "고객", "메뉴", "총액", "상태"};
        String[][] data = {
            {"ORD-001", "김철수", "발렌타인 디너", "₩85,000", "확인됨"},
            {"ORD-002", "이영희", "프렌치 디너", "₩95,000", "준비됨"},
            {"ORD-003", "박민수", "잉글리시 디너", "₩75,000", "배달완료"}
        };
        
        JTable ordersTable = new JTable(data, columns);
        ordersTable.setBackground(Color.WHITE);
        ordersTable.setGridColor(new Color(200, 200, 200));
        ordersTable.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("미스터 대박 프리미엄 메뉴");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Create menu items grid
        JPanel menuGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        
        menuGrid.add(createMenuItemCard("발렌타인 디너", "₩85,000", "하트 모양과 큐피드 장식된 접시에 와인과 스테이크"));
        menuGrid.add(createMenuItemCard("프렌치 디너", "₩95,000", "커피, 와인, 샐러드, 스테이크 제공"));
        menuGrid.add(createMenuItemCard("잉글리시 디너", "₩75,000", "에그 스크램블, 베이컨, 빵, 스테이크"));
        menuGrid.add(createMenuItemCard("샴페인 축제 디너", "₩195,000", "2인분, 샴페인 1병, 바게트빵 4개, 커피포트, 와인, 스테이크"));
        
        panel.add(menuGrid, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMenuItemCard(String name, String price, String description) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel priceLabel = new JLabel(price);
        priceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        priceLabel.setForeground(new Color(46, 204, 113));
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        descLabel.setForeground(new Color(100, 100, 100));
        
        card.add(nameLabel, BorderLayout.NORTH);
        card.add(priceLabel, BorderLayout.CENTER);
        card.add(descLabel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JPanel createCustomersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("고객 관리");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Customer table
        String[] columns = {"이름", "이메일", "전화번호", "주소", "주문횟수"};
        String[][] data = {
            {"김철수", "kim@email.com", "010-1234-5678", "서울시 강남구", "3"},
            {"이영희", "lee@email.com", "010-2345-6789", "서울시 서초구", "2"},
            {"박민수", "park@email.com", "010-3456-7890", "서울시 송파구", "1"}
        };
        
        JTable customersTable = new JTable(data, columns);
        customersTable.setBackground(Color.WHITE);
        customersTable.setGridColor(new Color(200, 200, 200));
        
        JScrollPane scrollPane = new JScrollPane(customersTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("재고 관리");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Inventory table
        String[] columns = {"재료", "현재 재고", "최소 수준", "상태"};
        String[][] data = {
            {"소고기 스테이크", "25", "10", "양호"},
            {"프리미엄 와인", "15", "5", "양호"},
            {"샴페인", "8", "3", "부족"},
            {"신선한 채소", "12", "5", "양호"}
        };
        
        JTable inventoryTable = new JTable(data, columns);
        inventoryTable.setBackground(Color.WHITE);
        inventoryTable.setGridColor(new Color(200, 200, 200));
        
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDeliveryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("배달 관리");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Delivery table
        String[] columns = {"주문 ID", "고객", "주소", "상태", "배달원"};
        String[][] data = {
            {"ORD-001", "김철수", "서울시 강남구", "배달중", "김배달"},
            {"ORD-002", "이영희", "서울시 서초구", "준비완료", "이배달"},
            {"ORD-003", "박민수", "서울시 송파구", "배달완료", "김배달"}
        };
        
        JTable deliveryTable = new JTable(data, columns);
        deliveryTable.setBackground(Color.WHITE);
        deliveryTable.setGridColor(new Color(200, 200, 200));
        
        JScrollPane scrollPane = new JScrollPane(deliveryTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("시스템 로그"));
        panel.setPreferredSize(new Dimension(0, 150));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(240, 240, 240));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Add any additional event handlers here
    }
    
    private void createSampleData() {
        // Create sample customers, staff, and menu items
        // This would normally populate the services with initial data
    }
    
    private void createNewOrder(String customerName, String phone, String address, 
                               String menuItem, String servingStyle, int quantity) {
        try {
            // Create customer
            Address customerAddress = new Address(address, "서울", "서울시", "12345", "대한민국");
            Customer customer = new Customer(customerName, "customer@email.com", phone, customerAddress, "password123");
            
            // Create order
            Order order = orderService.createOrder(customer, customerAddress);
            
            // Add menu item to order
            // This is simplified - in real implementation, you'd get the actual menu item
            logMessage("새 주문 생성: " + customerName + " - " + quantity + "개 " + menuItem + " (" + servingStyle + " 스타일)");
            
            // Clear form
            // You would clear the form fields here
            
        } catch (Exception e) {
            logMessage("주문 생성 오류: " + e.getMessage());
        }
    }
    
    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public static void main(String[] args) {
        // Use default look and feel
        
        SwingUtilities.invokeLater(() -> {
            new MainGUI().setVisible(true);
        });
    }
}
