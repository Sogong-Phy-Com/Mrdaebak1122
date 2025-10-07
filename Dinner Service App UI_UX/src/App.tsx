import { useState, useEffect } from 'react';
import { Auth } from './components/Auth';
import { Dashboard } from './components/Dashboard';
import { MenuSelection } from './components/MenuSelection';
import { OrderScreen } from './components/OrderScreen';
import { OrderHistory } from './components/OrderHistory';
import { Profile } from './components/Profile';
import { BottomNavigation } from './components/BottomNavigation';
import { Toaster } from './components/ui/sonner';
import { toast } from 'sonner@2.0.3';

export default function App() {
  const [user, setUser] = useState<any>(null);
  const [currentScreen, setCurrentScreen] = useState('dashboard');
  const [orderItems, setOrderItems] = useState<any[]>([]);
  const [orderHistory, setOrderHistory] = useState<any[]>([]);

  useEffect(() => {
    // 로컬 스토리지에서 사용자 정보 불러오기
    const savedUser = localStorage.getItem('user');
    const savedOrders = localStorage.getItem('orderHistory');
    
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      parsedUser.lastLogin = new Date().toISOString();
      setUser(parsedUser);
      localStorage.setItem('user', JSON.stringify(parsedUser));
    }
    
    if (savedOrders) {
      setOrderHistory(JSON.parse(savedOrders));
    }
  }, []);

  const handleLogin = (userData: any) => {
    const savedOrders = localStorage.getItem('orderHistory');
    const orders = savedOrders ? JSON.parse(savedOrders) : [];
    
    const userWithOrderCount = {
      ...userData,
      orderCount: orders.length
    };
    
    setUser(userWithOrderCount);
    localStorage.setItem('user', JSON.stringify(userWithOrderCount));
    setCurrentScreen('dashboard');
    toast.success('로그인되었습니다!');
  };

  const handleLogout = () => {
    setUser(null);
    setOrderItems([]);
    localStorage.removeItem('user');
    setCurrentScreen('dashboard');
    toast.info('로그아웃되었습니다');
  };

  const handleAddToOrder = (item: any) => {
    setOrderItems([...orderItems, item]);
    toast.success('메뉴가 추가되었습니다');
  };

  const handleUpdateOrder = (items: any[]) => {
    setOrderItems(items);
  };

  const handlePlaceOrder = (specialRequest: string) => {
    const calculateDiscount = () => {
      if (user.orderCount >= 20) return 0.15;
      if (user.orderCount >= 10) return 0.10;
      if (user.orderCount >= 5) return 0.05;
      return 0;
    };

    const subtotal = orderItems.reduce((sum, item) => sum + item.totalPrice, 0);
    const discount = subtotal * calculateDiscount();
    const total = subtotal - discount;

    const newOrder = {
      items: orderItems,
      subtotal: subtotal,
      discount: discount,
      total: total,
      orderedAt: new Date().toISOString(),
      deliveryAddress: user.address,
      city: user.city,
      specialRequest: specialRequest,
      status: Math.random() > 0.7 ? 'delivered' : Math.random() > 0.5 ? 'delivering' : 'preparing'
    };

    const updatedHistory = [newOrder, ...orderHistory];
    setOrderHistory(updatedHistory);
    localStorage.setItem('orderHistory', JSON.stringify(updatedHistory));

    const updatedUser = {
      ...user,
      orderCount: user.orderCount + 1
    };
    setUser(updatedUser);
    localStorage.setItem('user', JSON.stringify(updatedUser));

    setOrderItems([]);
    toast.success('주문이 완료되었습니다!');
  };

  const handleNavigate = (screen: string) => {
    setCurrentScreen(screen);
  };

  if (!user) {
    return (
      <>
        <Auth onLogin={handleLogin} />
        <Toaster />
      </>
    );
  }

  return (
    <div className="max-w-md mx-auto bg-background min-h-screen">
      {currentScreen === 'dashboard' && (
        <Dashboard user={user} onNavigate={handleNavigate} />
      )}
      
      {currentScreen === 'menu' && (
        <MenuSelection 
          onAddToOrder={handleAddToOrder} 
          onNavigate={handleNavigate}
        />
      )}
      
      {currentScreen === 'order' && (
        <OrderScreen
          orderItems={orderItems}
          onUpdateOrder={handleUpdateOrder}
          onPlaceOrder={handlePlaceOrder}
          onNavigate={handleNavigate}
          user={user}
        />
      )}
      
      {currentScreen === 'history' && (
        <OrderHistory orders={orderHistory} />
      )}
      
      {currentScreen === 'profile' && (
        <Profile 
          user={user} 
          orderCount={orderHistory.length}
          onLogout={handleLogout}
        />
      )}

      <BottomNavigation 
        currentScreen={currentScreen}
        onNavigate={handleNavigate}
      />
      
      <Toaster />
    </div>
  );
}
