-- 미스터 대박 디너 서비스 데이터베이스 스키마
-- Database Schema for Mr. DaeBak Dinner Service

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS mrdaeBak_dinner_service;
USE mrdaeBak_dinner_service;

-- 1. 고객 테이블 (Customers)
CREATE TABLE customers (
    customer_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_date TIMESTAMP NULL,
    order_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 주소 테이블 (Addresses)
CREATE TABLE addresses (
    address_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) DEFAULT 'South Korea',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- 3. 메뉴 아이템 테이블 (Menu Items)
CREATE TABLE menu_items (
    menu_item_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'KRW',
    item_type ENUM('APPETIZER', 'MAIN_COURSE', 'SIDE_DISH', 'DESSERT', 'BEVERAGE', 'BREAD') NOT NULL,
    preparation_time_minutes INT DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. 서빙 스타일 테이블 (Serving Styles)
CREATE TABLE serving_styles (
    serving_style_id INT PRIMARY KEY AUTO_INCREMENT,
    name ENUM('SIMPLE', 'GRAND', 'DELUXE') NOT NULL,
    description TEXT NOT NULL,
    price_multiplier DECIMAL(3,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. 디너 테이블 (Dinners)
CREATE TABLE dinners (
    dinner_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    base_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'KRW',
    dinner_type ENUM('VALENTINE', 'FRENCH', 'ENGLISH', 'CHAMPAGNE_FEAST') NOT NULL,
    serving_style_id INT NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (serving_style_id) REFERENCES serving_styles(serving_style_id)
);

-- 6. 디너 메뉴 아이템 매핑 테이블 (Dinner Menu Items)
CREATE TABLE dinner_menu_items (
    dinner_id VARCHAR(36) NOT NULL,
    menu_item_id VARCHAR(36) NOT NULL,
    quantity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dinner_id, menu_item_id),
    FOREIGN KEY (dinner_id) REFERENCES dinners(dinner_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(menu_item_id) ON DELETE CASCADE
);

-- 7. 주문 테이블 (Orders)
CREATE TABLE orders (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_DELIVERY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'KRW',
    delivery_address_id VARCHAR(36) NOT NULL,
    estimated_delivery_time TIMESTAMP NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (delivery_address_id) REFERENCES addresses(address_id)
);

-- 8. 주문 아이템 테이블 (Order Items)
CREATE TABLE order_items (
    order_item_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    dinner_id VARCHAR(36) NULL,
    menu_item_id VARCHAR(36) NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    special_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (dinner_id) REFERENCES dinners(dinner_id) ON DELETE SET NULL,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(menu_item_id) ON DELETE SET NULL,
    CONSTRAINT chk_order_item_type CHECK (
        (dinner_id IS NOT NULL AND menu_item_id IS NULL) OR 
        (dinner_id IS NULL AND menu_item_id IS NOT NULL)
    )
);

-- 9. 결제 테이블 (Payments)
CREATE TABLE payments (
    payment_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    payment_method ENUM('CREDIT_CARD', 'DEBIT_CARD', 'CASH', 'BANK_TRANSFER') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'KRW',
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    transaction_id VARCHAR(255) NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- 10. 배달 테이블 (Deliveries)
CREATE TABLE deliveries (
    delivery_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    courier_id VARCHAR(36) NULL,
    status ENUM('PENDING', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED') DEFAULT 'PENDING',
    pickup_time TIMESTAMP NULL,
    delivery_time TIMESTAMP NULL,
    delivery_address_id VARCHAR(36) NOT NULL,
    delivery_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (delivery_address_id) REFERENCES addresses(address_id)
);

-- 11. 직원 테이블 (Staff)
CREATE TABLE staff (
    staff_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    staff_type ENUM('COOK', 'COURIER', 'MANAGER') NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'ON_BREAK') DEFAULT 'ACTIVE',
    work_start_time TIME DEFAULT '15:30:00',
    work_end_time TIME DEFAULT '22:00:00',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 12. 재고 테이블 (Inventory)
CREATE TABLE inventory (
    inventory_id VARCHAR(36) PRIMARY KEY,
    item_name VARCHAR(200) NOT NULL,
    item_type ENUM('INGREDIENT', 'UTENSIL', 'PACKAGING', 'BEVERAGE') NOT NULL,
    current_stock INT NOT NULL DEFAULT 0,
    minimum_stock INT NOT NULL DEFAULT 10,
    unit VARCHAR(50) NOT NULL,
    cost_per_unit DECIMAL(8,2) NOT NULL,
    supplier VARCHAR(200),
    last_restocked TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 13. 재고 거래 테이블 (Inventory Transactions)
CREATE TABLE inventory_transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    inventory_id VARCHAR(36) NOT NULL,
    transaction_type ENUM('IN', 'OUT', 'ADJUSTMENT') NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(255),
    reference_id VARCHAR(36) NULL, -- 주문 ID나 다른 참조
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NULL,
    FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES staff(staff_id) ON DELETE SET NULL
);

-- 인덱스 생성
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_phone ON customers(phone_number);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_addresses_customer ON addresses(customer_id);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_time ON orders(order_time);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_deliveries_order ON deliveries(order_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_staff_type ON staff(staff_type);
CREATE INDEX idx_staff_status ON staff(status);
CREATE INDEX idx_inventory_item_type ON inventory(item_type);
CREATE INDEX idx_inventory_transactions_inventory ON inventory_transactions(inventory_id);
CREATE INDEX idx_inventory_transactions_date ON inventory_transactions(transaction_date);

-- 초기 데이터 삽입

-- 서빙 스타일 데이터
INSERT INTO serving_styles (name, description, price_multiplier) VALUES
('SIMPLE', '플라스틱 접시와 플라스틱 컵, 종이 냅킨이 플라스틱 쟁반에 제공', 0.00),
('GRAND', '도자기 접시와 도자기 컵, 흰색 면 냅킨이 나무 쟁반에 제공', 0.15),
('DELUXE', '꽃들이 있는 작은 꽃병, 도자기 접시와 도자기 컵, 린넨 냅킨이 나무 쟁반에 제공', 0.30);

-- 기본 메뉴 아이템 데이터
INSERT INTO menu_items (menu_item_id, name, description, price, item_type, preparation_time_minutes) VALUES
(UUID(), '발렌타인 와인', '로맨틱한 분위기의 특별한 와인', 25000.00, 'BEVERAGE', 5),
(UUID(), '발렌타인 스테이크', '특별한 날을 위한 프리미엄 스테이크', 35000.00, 'MAIN_COURSE', 25),
(UUID(), '하트 & 큐피드 장식', '작은 하트 모양과 큐피드가 장식된 특별한 접시와 냅킨', 10000.00, 'APPETIZER', 10),
(UUID(), '프렌치 커피', '프랑스식 프리미엄 커피 한잔', 8000.00, 'BEVERAGE', 5),
(UUID(), '프렌치 와인', '프랑스산 프리미엄 와인 한잔', 25000.00, 'BEVERAGE', 3),
(UUID(), '프렌치 샐러드', '프랑스식 신선한 샐러드', 15000.00, 'APPETIZER', 10),
(UUID(), '프렌치 스테이크', '프랑스식 조리법의 프리미엄 스테이크', 35000.00, 'MAIN_COURSE', 25),
(UUID(), '에그 스크램블', '영국식 부드러운 에그 스크램블', 12000.00, 'MAIN_COURSE', 8),
(UUID(), '영국식 베이컨', '바삭한 영국식 베이컨', 15000.00, 'SIDE_DISH', 6),
(UUID(), '영국식 빵', '신선한 영국식 토스트 빵', 8000.00, 'BREAD', 3),
(UUID(), '잉글리시 스테이크', '영국식 조리법의 프리미엄 스테이크', 35000.00, 'MAIN_COURSE', 25),
(UUID(), '프리미엄 샴페인', '축제를 위한 고급 샴페인 1병', 80000.00, 'BEVERAGE', 5),
(UUID(), '바게트빵', '신선한 바게트빵 4개', 20000.00, 'BREAD', 3),
(UUID(), '커피 포트', '따뜻한 커피 포트 1개', 15000.00, 'BEVERAGE', 8),
(UUID(), '축제용 스테이크', '2인분 프리미엄 스테이크', 45000.00, 'MAIN_COURSE', 30);

-- 기본 직원 데이터
INSERT INTO staff (staff_id, name, email, phone_number, staff_type) VALUES
(UUID(), '김요리', 'cook1@mrdaeBak.com', '010-1111-1111', 'COOK'),
(UUID(), '이요리', 'cook2@mrdaeBak.com', '010-1111-1112', 'COOK'),
(UUID(), '박요리', 'cook3@mrdaeBak.com', '010-1111-1113', 'COOK'),
(UUID(), '최요리', 'cook4@mrdaeBak.com', '010-1111-1114', 'COOK'),
(UUID(), '정요리', 'cook5@mrdaeBak.com', '010-1111-1115', 'COOK'),
(UUID(), '김배달', 'courier1@mrdaeBak.com', '010-2222-2221', 'COURIER'),
(UUID(), '이배달', 'courier2@mrdaeBak.com', '010-2222-2222', 'COURIER'),
(UUID(), '박배달', 'courier3@mrdaeBak.com', '010-2222-2223', 'COURIER'),
(UUID(), '최배달', 'courier4@mrdaeBak.com', '010-2222-2224', 'COURIER'),
(UUID(), '정배달', 'courier5@mrdaeBak.com', '010-2222-2225', 'COURIER');

-- 기본 재고 데이터
INSERT INTO inventory (inventory_id, item_name, item_type, current_stock, minimum_stock, unit, cost_per_unit, supplier) VALUES
(UUID(), '소고기 스테이크', 'INGREDIENT', 50, 10, 'kg', 25000.00, '프리미엄 육류공급업체'),
(UUID(), '프리미엄 와인', 'BEVERAGE', 100, 20, '병', 15000.00, '와인 전문업체'),
(UUID(), '샴페인', 'BEVERAGE', 30, 5, '병', 50000.00, '와인 전문업체'),
(UUID(), '신선한 채소', 'INGREDIENT', 200, 50, 'kg', 5000.00, '농산물 직거래'),
(UUID(), '도자기 접시', 'UTENSIL', 500, 100, '개', 3000.00, '식기 전문업체'),
(UUID(), '플라스틱 접시', 'UTENSIL', 1000, 200, '개', 500.00, '식기 전문업체'),
(UUID(), '린넨 냅킨', 'UTENSIL', 300, 50, '개', 2000.00, '텍스타일 업체'),
(UUID(), '종이 냅킨', 'UTENSIL', 2000, 500, '개', 100.00, '종이 제품업체'),
(UUID(), '나무 쟁반', 'UTENSIL', 200, 50, '개', 8000.00, '목재 가구업체'),
(UUID(), '플라스틱 쟁반', 'UTENSIL', 500, 100, '개', 2000.00, '플라스틱 제품업체');
