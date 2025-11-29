# 웹사이트 백엔드 연동 가이드

## 🌐 웹용 API 연동 가이드

### 1. 기본 설정

#### HTML 기본 구조
```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>미스터 대박 디너 서비스</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div id="app">
        <!-- 웹 애플리케이션 내용 -->
    </div>
    <script src="app.js"></script>
</body>
</html>
```

#### JavaScript API 클라이언트 설정
```javascript
// API 기본 설정
const API_BASE_URL = 'http://localhost:8080/api';

class ApiClient {
    constructor() {
        this.baseURL = API_BASE_URL;
        this.token = localStorage.getItem('authToken');
    }
    
    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };
        
        if (this.token) {
            config.headers['Authorization'] = `Bearer ${this.token}`;
        }
        
        try {
            const response = await fetch(url, config);
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.message || 'API 요청 실패');
            }
            
            return data;
        } catch (error) {
            console.error('API 요청 오류:', error);
            throw error;
        }
    }
    
    async get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    }
    
    async post(endpoint, data) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
    
    async put(endpoint, data) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }
    
    async delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
}

const api = new ApiClient();
```

### 2. 인증 시스템 구현

#### 로그인/회원가입 페이지
```html
<!-- login.html -->
<div class="auth-container">
    <div class="auth-form">
        <h2>로그인</h2>
        <form id="loginForm">
            <input type="email" id="email" placeholder="이메일" required>
            <input type="password" id="password" placeholder="비밀번호" required>
            <button type="submit">로그인</button>
        </form>
        <p>계정이 없으신가요? <a href="register.html">회원가입</a></p>
    </div>
</div>
```

```javascript
// 로그인 기능
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
    try {
        const response = await api.post('/customers/login', { email, password });
        
        if (response.success) {
            // 로그인 성공
            localStorage.setItem('authToken', response.customer.customerId);
            localStorage.setItem('userInfo', JSON.stringify(response.customer));
            
            // 메인 페이지로 리다이렉트
            window.location.href = 'index.html';
        }
    } catch (error) {
        alert('로그인 실패: ' + error.message);
    }
});

// 회원가입 기능
document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const userData = Object.fromEntries(formData);
    
    try {
        const response = await api.post('/customers/register', userData);
        
        if (response.success) {
            alert('회원가입이 완료되었습니다!');
            window.location.href = 'login.html';
        }
    } catch (error) {
        alert('회원가입 실패: ' + error.message);
    }
});
```

### 3. 메뉴 페이지 구현

#### 메뉴 목록 표시
```html
<!-- menu.html -->
<div class="menu-container">
    <h1>미스터 대박 프리미엄 메뉴</h1>
    <div class="menu-grid" id="menuGrid">
        <!-- 메뉴 카드들이 여기에 동적으로 추가됩니다 -->
    </div>
</div>

<!-- 메뉴 카드 템플릿 -->
<template id="menuCardTemplate">
    <div class="menu-card" data-dinner-type="">
        <div class="menu-image">
            <img src="" alt="" class="dinner-image">
        </div>
        <div class="menu-content">
            <h3 class="menu-name"></h3>
            <p class="menu-description"></p>
            
            <div class="serving-styles">
                <label>서빙 스타일:</label>
                <select class="serving-style-select">
                    <option value="SIMPLE">심플</option>
                    <option value="GRAND">그랜드</option>
                    <option value="DELUXE">디럭스</option>
                </select>
            </div>
            
            <div class="menu-items">
                <h4>포함 메뉴:</h4>
                <ul class="items-list"></ul>
            </div>
            
            <div class="price-section">
                <span class="price"></span>
                <button class="order-button">주문하기</button>
            </div>
        </div>
    </div>
</template>
```

```javascript
// 메뉴 로드 및 표시
class MenuManager {
    constructor() {
        this.menus = [];
        this.init();
    }
    
    async init() {
        await this.loadMenus();
        this.renderMenus();
        this.bindEvents();
    }
    
    async loadMenus() {
        try {
            const response = await api.get('/menu/dinners');
            if (response.success) {
                this.menus = response.dinners;
            }
        } catch (error) {
            console.error('메뉴 로드 실패:', error);
        }
    }
    
    renderMenus() {
        const menuGrid = document.getElementById('menuGrid');
        const template = document.getElementById('menuCardTemplate');
        
        menuGrid.innerHTML = '';
        
        this.menus.forEach(menu => {
            const card = template.content.cloneNode(true);
            const cardElement = card.querySelector('.menu-card');
            
            // 메뉴 정보 설정
            cardElement.dataset.dinnerType = menu.dinnerType;
            cardElement.querySelector('.dinner-image').src = this.getMenuImage(menu.dinnerType);
            cardElement.querySelector('.dinner-image').alt = menu.name;
            cardElement.querySelector('.menu-name').textContent = menu.name;
            cardElement.querySelector('.menu-description').textContent = menu.description;
            
            // 서빙 스타일별 가격 설정
            const priceElement = cardElement.querySelector('.price');
            const styleSelect = cardElement.querySelector('.serving-style-select');
            
            this.updatePrice(priceElement, menu, 'SIMPLE');
            
            styleSelect.addEventListener('change', (e) => {
                this.updatePrice(priceElement, menu, e.target.value);
            });
            
            // 포함 메뉴 아이템 표시
            const itemsList = cardElement.querySelector('.items-list');
            menu.menuItems.forEach(item => {
                const li = document.createElement('li');
                li.textContent = item.name;
                itemsList.appendChild(li);
            });
            
            menuGrid.appendChild(card);
        });
    }
    
    updatePrice(priceElement, menu, servingStyle) {
        const price = menu.pricesByStyle[servingStyle] || menu.basePrice;
        priceElement.textContent = price;
    }
    
    getMenuImage(dinnerType) {
        const imageMap = {
            '발렌타인 디너': 'images/valentine-dinner.jpg',
            '프렌치 디너': 'images/french-dinner.jpg',
            '잉글리시 디너': 'images/english-dinner.jpg',
            '샴페인 축제 디너': 'images/champagne-dinner.jpg'
        };
        return imageMap[dinnerType] || 'images/default-dinner.jpg';
    }
    
    bindEvents() {
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('order-button')) {
                const menuCard = e.target.closest('.menu-card');
                const dinnerType = menuCard.dataset.dinnerType;
                const servingStyle = menuCard.querySelector('.serving-style-select').value;
                
                this.handleOrder(dinnerType, servingStyle);
            }
        });
    }
    
    handleOrder(dinnerType, servingStyle) {
        // 주문 페이지로 이동
        window.location.href = `order.html?dinner=${encodeURIComponent(dinnerType)}&style=${servingStyle}`;
    }
}

// 페이지 로드 시 메뉴 매니저 초기화
document.addEventListener('DOMContentLoaded', () => {
    new MenuManager();
});
```

### 4. 주문 페이지 구현

#### 주문 폼
```html
<!-- order.html -->
<div class="order-container">
    <h1>주문하기</h1>
    
    <div class="order-form">
        <div class="order-summary">
            <h2>주문 요약</h2>
            <div id="orderSummary">
                <!-- 주문 정보가 여기에 표시됩니다 -->
            </div>
        </div>
        
        <div class="customer-info">
            <h2>배달 정보</h2>
            <form id="orderForm">
                <input type="text" id="customerName" placeholder="고객명" required>
                <input type="tel" id="phoneNumber" placeholder="전화번호" required>
                <input type="text" id="streetAddress" placeholder="상세주소" required>
                <input type="text" id="city" placeholder="도시" value="서울시" required>
                <input type="text" id="state" placeholder="구/군" required>
                <input type="text" id="postalCode" placeholder="우편번호" required>
                
                <div class="quantity-selector">
                    <label>수량:</label>
                    <button type="button" id="decreaseQty">-</button>
                    <span id="quantity">1</span>
                    <button type="button" id="increaseQty">+</button>
                </div>
                
                <div class="special-requests">
                    <label>특별 요청사항:</label>
                    <textarea id="notes" placeholder="특별 요청사항을 입력해주세요"></textarea>
                </div>
                
                <div class="total-price">
                    <h3>총 금액: <span id="totalPrice">₩0</span></h3>
                </div>
                
                <button type="submit" id="submitOrder">주문하기</button>
            </form>
        </div>
    </div>
</div>
```

```javascript
// 주문 페이지 관리
class OrderManager {
    constructor() {
        this.dinnerType = new URLSearchParams(window.location.search).get('dinner');
        this.servingStyle = new URLSearchParams(window.location.search).get('style');
        this.quantity = 1;
        this.unitPrice = 0;
        
        this.init();
    }
    
    async init() {
        await this.loadOrderDetails();
        this.bindEvents();
        this.updateOrderSummary();
        this.calculateTotalPrice();
    }
    
    async loadOrderDetails() {
        try {
            const response = await api.get(`/menu/dinners/${encodeURIComponent(this.dinnerType)}`);
            if (response.success) {
                this.orderDetails = response.dinner;
                this.updatePrice();
            }
        } catch (error) {
            console.error('주문 정보 로드 실패:', error);
        }
    }
    
    updatePrice() {
        const stylePrices = this.orderDetails.stylePrices[this.servingStyle];
        this.unitPrice = stylePrices.price;
        this.calculateTotalPrice();
    }
    
    calculateTotalPrice() {
        const totalPrice = this.unitPrice.replace('₩', '').replace(',', '') * this.quantity;
        document.getElementById('totalPrice').textContent = `₩${totalPrice.toLocaleString()}`;
    }
    
    updateOrderSummary() {
        const summaryElement = document.getElementById('orderSummary');
        summaryElement.innerHTML = `
            <div class="summary-item">
                <h3>${this.orderDetails.name}</h3>
                <p>서빙 스타일: ${this.servingStyle}</p>
                <p>수량: ${this.quantity}개</p>
                <p>단가: ${this.unitPrice}</p>
            </div>
        `;
    }
    
    bindEvents() {
        // 수량 조절
        document.getElementById('decreaseQty').addEventListener('click', () => {
            if (this.quantity > 1) {
                this.quantity--;
                document.getElementById('quantity').textContent = this.quantity;
                this.updateOrderSummary();
                this.calculateTotalPrice();
            }
        });
        
        document.getElementById('increaseQty').addEventListener('click', () => {
            this.quantity++;
            document.getElementById('quantity').textContent = this.quantity;
            this.updateOrderSummary();
            this.calculateTotalPrice();
        });
        
        // 주문 제출
        document.getElementById('orderForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.submitOrder();
        });
    }
    
    async submitOrder() {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
        
        const orderData = {
            customerId: userInfo.customerId || 'guest',
            dinners: [{
                dinnerType: this.dinnerType,
                servingStyle: this.servingStyle,
                quantity: this.quantity
            }],
            deliveryAddress: {
                streetAddress: document.getElementById('streetAddress').value,
                city: document.getElementById('city').value,
                state: document.getElementById('state').value,
                postalCode: document.getElementById('postalCode').value,
                country: '대한민국'
            },
            notes: document.getElementById('notes').value
        };
        
        try {
            const response = await api.post('/orders', orderData);
            
            if (response.success) {
                // 주문 성공 페이지로 이동
                window.location.href = `order-success.html?orderId=${response.order.orderId}`;
            }
        } catch (error) {
            alert('주문 실패: ' + error.message);
        }
    }
}

// 페이지 로드 시 주문 매니저 초기화
document.addEventListener('DOMContentLoaded', () => {
    new OrderManager();
});
```

### 5. 주문 내역 페이지 구현

```html
<!-- order-history.html -->
<div class="order-history-container">
    <h1>주문 내역</h1>
    <div class="order-list" id="orderList">
        <!-- 주문 내역이 여기에 표시됩니다 -->
    </div>
</div>

<template id="orderItemTemplate">
    <div class="order-item">
        <div class="order-header">
            <span class="order-id"></span>
            <span class="order-date"></span>
            <span class="order-status"></span>
        </div>
        <div class="order-details">
            <div class="order-info">
                <h3 class="dinner-name"></h3>
                <p class="serving-style"></p>
                <p class="delivery-address"></p>
                <p class="delivery-time"></p>
            </div>
            <div class="order-price">
                <span class="price"></span>
            </div>
        </div>
        <div class="order-actions">
            <button class="reorder-button">재주문</button>
            <button class="cancel-button" style="display: none;">주문 취소</button>
        </div>
    </div>
</template>
```

```javascript
// 주문 내역 관리
class OrderHistoryManager {
    constructor() {
        this.orders = [];
        this.init();
    }
    
    async init() {
        await this.loadOrders();
        this.renderOrders();
        this.bindEvents();
    }
    
    async loadOrders() {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const response = await api.get(`/customers/${userInfo.customerId}/orders`);
            
            if (response.success) {
                this.orders = response.orders;
            }
        } catch (error) {
            console.error('주문 내역 로드 실패:', error);
        }
    }
    
    renderOrders() {
        const orderList = document.getElementById('orderList');
        const template = document.getElementById('orderItemTemplate');
        
        orderList.innerHTML = '';
        
        this.orders.forEach(order => {
            const orderElement = template.content.cloneNode(true);
            
            // 주문 정보 설정
            orderElement.querySelector('.order-id').textContent = `주문번호: ${order.orderId}`;
            orderElement.querySelector('.order-date').textContent = new Date(order.orderTime).toLocaleDateString();
            orderElement.querySelector('.order-status').textContent = this.getStatusText(order.status);
            orderElement.querySelector('.dinner-name').textContent = order.dinnerType;
            orderElement.querySelector('.serving-style').textContent = `서빙 스타일: ${order.servingStyle}`;
            orderElement.querySelector('.delivery-address').textContent = order.deliveryAddress;
            orderElement.querySelector('.delivery-time').textContent = `배달 예정: ${new Date(order.deliveryTime).toLocaleString()}`;
            orderElement.querySelector('.price').textContent = order.price;
            
            // 주문 상태에 따른 버튼 표시
            if (order.status === 'PENDING' || order.status === 'CONFIRMED') {
                orderElement.querySelector('.cancel-button').style.display = 'inline-block';
            }
            
            orderList.appendChild(orderElement);
        });
    }
    
    getStatusText(status) {
        const statusMap = {
            'PENDING': '주문 대기',
            'CONFIRMED': '주문 확인',
            'PREPARING': '조리 중',
            'READY_FOR_DELIVERY': '배달 준비',
            'OUT_FOR_DELIVERY': '배달 중',
            'DELIVERED': '배달 완료',
            'CANCELLED': '주문 취소'
        };
        return statusMap[status] || status;
    }
    
    bindEvents() {
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('reorder-button')) {
                const orderItem = e.target.closest('.order-item');
                const orderId = orderItem.querySelector('.order-id').textContent.split(': ')[1];
                this.reorder(orderId);
            }
            
            if (e.target.classList.contains('cancel-button')) {
                const orderItem = e.target.closest('.order-item');
                const orderId = orderItem.querySelector('.order-id').textContent.split(': ')[1];
                this.cancelOrder(orderId);
            }
        });
    }
    
    async reorder(orderId) {
        // 재주문 로직 구현
        const order = this.orders.find(o => o.orderId === orderId);
        if (order) {
            window.location.href = `order.html?dinner=${encodeURIComponent(order.dinnerType)}&style=${order.servingStyle}`;
        }
    }
    
    async cancelOrder(orderId) {
        if (confirm('정말로 주문을 취소하시겠습니까?')) {
            try {
                const response = await api.delete(`/orders/${orderId}`);
                
                if (response.success) {
                    alert('주문이 취소되었습니다.');
                    this.init(); // 목록 새로고침
                }
            } catch (error) {
                alert('주문 취소 실패: ' + error.message);
            }
        }
    }
}

// 페이지 로드 시 주문 내역 매니저 초기화
document.addEventListener('DOMContentLoaded', () => {
    new OrderHistoryManager();
});
```

### 6. 관리자 페이지 구현

```html
<!-- admin.html -->
<div class="admin-container">
    <h1>관리자 대시보드</h1>
    
    <div class="dashboard-stats" id="dashboardStats">
        <!-- 대시보드 통계가 여기에 표시됩니다 -->
    </div>
    
    <div class="admin-tabs">
        <button class="tab-button active" data-tab="orders">주문 관리</button>
        <button class="tab-button" data-tab="customers">고객 관리</button>
        <button class="tab-button" data-tab="inventory">재고 관리</button>
        <button class="tab-button" data-tab="deliveries">배달 관리</button>
    </div>
    
    <div class="tab-content">
        <div id="ordersTab" class="tab-panel active">
            <div class="order-list" id="adminOrderList">
                <!-- 관리자용 주문 목록 -->
            </div>
        </div>
        
        <div id="customersTab" class="tab-panel">
            <div class="customer-list" id="adminCustomerList">
                <!-- 고객 목록 -->
            </div>
        </div>
        
        <div id="inventoryTab" class="tab-panel">
            <div class="inventory-list" id="adminInventoryList">
                <!-- 재고 목록 -->
            </div>
        </div>
        
        <div id="deliveriesTab" class="tab-panel">
            <div class="delivery-list" id="adminDeliveryList">
                <!-- 배달 목록 -->
            </div>
        </div>
    </div>
</div>
```

```javascript
// 관리자 대시보드 관리
class AdminDashboard {
    constructor() {
        this.init();
    }
    
    async init() {
        await this.loadDashboardStats();
        this.bindTabEvents();
        this.loadOrders();
    }
    
    async loadDashboardStats() {
        try {
            const response = await api.get('/admin/dashboard');
            if (response.success) {
                this.renderDashboardStats(response.stats);
            }
        } catch (error) {
            console.error('대시보드 통계 로드 실패:', error);
        }
    }
    
    renderDashboardStats(stats) {
        const statsElement = document.getElementById('dashboardStats');
        statsElement.innerHTML = `
            <div class="stat-card">
                <h3>오늘의 주문</h3>
                <span class="stat-number">${stats.todayOrders}</span>
            </div>
            <div class="stat-card">
                <h3>오늘의 매출</h3>
                <span class="stat-number">${stats.todayRevenue}</span>
            </div>
            <div class="stat-card">
                <h3>활성 고객</h3>
                <span class="stat-number">${stats.customerStats.activeCustomers}</span>
            </div>
            <div class="stat-card">
                <h3>배달 중</h3>
                <span class="stat-number">${stats.activeDeliveries}</span>
            </div>
        `;
    }
    
    bindTabEvents() {
        document.querySelectorAll('.tab-button').forEach(button => {
            button.addEventListener('click', (e) => {
                const tabName = e.target.dataset.tab;
                this.switchTab(tabName);
            });
        });
    }
    
    switchTab(tabName) {
        // 탭 버튼 활성화
        document.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        
        // 탭 패널 표시
        document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.remove('active'));
        document.getElementById(`${tabName}Tab`).classList.add('active');
        
        // 해당 탭 데이터 로드
        switch(tabName) {
            case 'orders':
                this.loadOrders();
                break;
            case 'customers':
                this.loadCustomers();
                break;
            case 'inventory':
                this.loadInventory();
                break;
            case 'deliveries':
                this.loadDeliveries();
                break;
        }
    }
    
    async loadOrders() {
        try {
            const response = await api.get('/admin/orders');
            if (response.success) {
                this.renderOrders(response.orders);
            }
        } catch (error) {
            console.error('주문 목록 로드 실패:', error);
        }
    }
    
    renderOrders(orders) {
        const orderList = document.getElementById('adminOrderList');
        orderList.innerHTML = `
            <table class="admin-table">
                <thead>
                    <tr>
                        <th>주문 ID</th>
                        <th>고객명</th>
                        <th>메뉴</th>
                        <th>총액</th>
                        <th>상태</th>
                        <th>주문시간</th>
                        <th>액션</th>
                    </tr>
                </thead>
                <tbody>
                    ${orders.map(order => `
                        <tr>
                            <td>${order.orderId}</td>
                            <td>${order.customerName}</td>
                            <td>${order.menuItems}</td>
                            <td>${order.totalAmount}</td>
                            <td>
                                <select class="status-select" data-order-id="${order.orderId}">
                                    <option value="PENDING" ${order.status === 'PENDING' ? 'selected' : ''}>대기</option>
                                    <option value="CONFIRMED" ${order.status === 'CONFIRMED' ? 'selected' : ''}>확인</option>
                                    <option value="PREPARING" ${order.status === 'PREPARING' ? 'selected' : ''}>조리중</option>
                                    <option value="READY_FOR_DELIVERY" ${order.status === 'READY_FOR_DELIVERY' ? 'selected' : ''}>배달준비</option>
                                    <option value="OUT_FOR_DELIVERY" ${order.status === 'OUT_FOR_DELIVERY' ? 'selected' : ''}>배달중</option>
                                    <option value="DELIVERED" ${order.status === 'DELIVERED' ? 'selected' : ''}>배달완료</option>
                                </select>
                            </td>
                            <td>${new Date(order.orderTime).toLocaleString()}</td>
                            <td>
                                <button class="btn btn-primary" onclick="viewOrderDetails('${order.orderId}')">상세보기</button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
        // 상태 변경 이벤트 바인딩
        orderList.querySelectorAll('.status-select').forEach(select => {
            select.addEventListener('change', (e) => {
                this.updateOrderStatus(e.target.dataset.orderId, e.target.value);
            });
        });
    }
    
    async updateOrderStatus(orderId, newStatus) {
        try {
            const response = await api.put(`/orders/${orderId}/status`, { status: newStatus });
            if (response.success) {
                alert('주문 상태가 업데이트되었습니다.');
            }
        } catch (error) {
            alert('주문 상태 업데이트 실패: ' + error.message);
        }
    }
}

// 페이지 로드 시 관리자 대시보드 초기화
document.addEventListener('DOMContentLoaded', () => {
    new AdminDashboard();
});
```

이제 웹사이트에서 완전한 미스터 대박 디너 서비스를 구현할 수 있습니다! 모든 기능이 백엔드 API와 연동되어 실시간으로 데이터를 주고받습니다.
