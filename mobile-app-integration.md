# 모바일 앱 백엔드 연동 가이드

## 📱 앱용 API 연동 가이드

### 1. 기본 설정

#### Base URL 설정
```javascript
// React Native / Expo
const API_BASE_URL = 'http://localhost:8080/api';

// Flutter
const String API_BASE_URL = 'http://localhost:8080/api';

// Android (Kotlin)
private val API_BASE_URL = "http://localhost:8080/api"
```

#### HTTP 클라이언트 설정
```javascript
// React Native - Axios 설정
import axios from 'axios';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터 (토큰 추가)
api.interceptors.request.use((config) => {
  const token = AsyncStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 2. 회원가입/로그인 구현

#### 회원가입 API 호출
```javascript
// React Native 예시
const registerUser = async (userData) => {
  try {
    const response = await api.post('/customers/register', {
      name: userData.name,
      email: userData.email,
      phoneNumber: userData.phoneNumber,
      password: userData.password,
      streetAddress: userData.streetAddress,
      city: userData.city,
      state: userData.state,
      postalCode: userData.postalCode,
      country: '대한민국'
    });
    
    if (response.data.success) {
      // 회원가입 성공
      console.log('회원가입 성공:', response.data.customer);
      return response.data;
    }
  } catch (error) {
    console.error('회원가입 실패:', error.response?.data?.message);
    throw error;
  }
};
```

#### 로그인 API 호출
```javascript
const loginUser = async (email, password) => {
  try {
    const response = await api.post('/customers/login', {
      email: email,
      password: password
    });
    
    if (response.data.success) {
      // 로그인 성공 - 토큰 저장
      await AsyncStorage.setItem('authToken', response.data.customer.customerId);
      await AsyncStorage.setItem('userInfo', JSON.stringify(response.data.customer));
      return response.data.customer;
    }
  } catch (error) {
    console.error('로그인 실패:', error.response?.data?.message);
    throw error;
  }
};
```

### 3. 메뉴 조회 구현

#### 디너 메뉴 목록 조회
```javascript
const getDinnerMenus = async () => {
  try {
    const response = await api.get('/menu/dinners');
    
    if (response.data.success) {
      return response.data.dinners.map(dinner => ({
        id: dinner.dinnerId,
        name: dinner.name,
        description: dinner.description,
        basePrice: dinner.basePrice,
        dinnerType: dinner.dinnerType,
        pricesByStyle: dinner.pricesByStyle,
        menuItems: dinner.menuItems,
        isAvailable: dinner.isAvailable
      }));
    }
  } catch (error) {
    console.error('메뉴 조회 실패:', error.response?.data?.message);
    throw error;
  }
};
```

#### 서빙 스타일 조회
```javascript
const getServingStyles = async () => {
  try {
    const response = await api.get('/menu/serving-styles');
    
    if (response.data.success) {
      return response.data.servingStyles.map(style => ({
        name: style.name,
        description: style.description,
        priceMultiplier: style.priceMultiplier
      }));
    }
  } catch (error) {
    console.error('서빙 스타일 조회 실패:', error.response?.data?.message);
    throw error;
  }
};
```

#### 실시간 가격 계산
```javascript
const calculatePrice = async (dinnerType, servingStyle, quantity) => {
  try {
    const response = await api.post('/menu/calculate-price', {
      dinnerType: dinnerType,
      servingStyle: servingStyle,
      quantity: quantity
    });
    
    if (response.data.success) {
      return {
        unitPrice: response.data.priceInfo.unitPrice,
        totalPrice: response.data.priceInfo.totalPrice,
        basePrice: response.data.priceInfo.basePrice,
        stylePrice: response.data.priceInfo.stylePrice
      };
    }
  } catch (error) {
    console.error('가격 계산 실패:', error.response?.data?.message);
    throw error;
  }
};
```

### 4. 주문 기능 구현

#### 새 주문 생성
```javascript
const createOrder = async (orderData) => {
  try {
    const userInfo = JSON.parse(await AsyncStorage.getItem('userInfo'));
    
    const response = await api.post('/orders', {
      customerId: userInfo.customerId,
      dinners: [{
        dinnerType: orderData.dinnerType,
        servingStyle: orderData.servingStyle,
        quantity: orderData.quantity
      }],
      deliveryAddress: {
        streetAddress: orderData.deliveryAddress.streetAddress,
        city: orderData.deliveryAddress.city,
        state: orderData.deliveryAddress.state,
        postalCode: orderData.deliveryAddress.postalCode,
        country: '대한민국'
      },
      notes: orderData.notes || ''
    });
    
    if (response.data.success) {
      return response.data.order;
    }
  } catch (error) {
    console.error('주문 생성 실패:', error.response?.data?.message);
    throw error;
  }
};
```

#### 주문 내역 조회
```javascript
const getCustomerOrders = async () => {
  try {
    const userInfo = JSON.parse(await AsyncStorage.getItem('userInfo'));
    const response = await api.get(`/customers/${userInfo.customerId}/orders`);
    
    if (response.data.success) {
      return response.data.orders.map(order => ({
        orderId: order.orderId,
        orderTime: order.orderTime,
        dinnerType: order.dinnerType,
        servingStyle: order.servingStyle,
        price: order.price,
        deliveryTime: order.deliveryTime,
        deliveryAddress: order.deliveryAddress,
        status: order.status
      }));
    }
  } catch (error) {
    console.error('주문 내역 조회 실패:', error.response?.data?.message);
    throw error;
  }
};
```

#### 주문 수정 (추가/변경/삭제)
```javascript
const modifyOrder = async (orderId, modifications) => {
  try {
    const response = await api.put(`/orders/${orderId}/items`, {
      removeItems: modifications.removeItems || [],
      addItems: modifications.addItems || [],
      updateItems: modifications.updateItems || []
    });
    
    if (response.data.success) {
      return response.data;
    }
  } catch (error) {
    console.error('주문 수정 실패:', error.response?.data?.message);
    throw error;
  }
};
```

### 5. UI 컴포넌트 예시 (React Native)

#### 메뉴 카드 컴포넌트
```javascript
import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

const MenuCard = ({ dinner, onSelect }) => {
  const [selectedStyle, setSelectedStyle] = useState('SIMPLE');
  const [price, setPrice] = useState(dinner.basePrice);
  
  useEffect(() => {
    calculatePrice();
  }, [selectedStyle]);
  
  const calculatePrice = async () => {
    try {
      const priceInfo = await calculatePrice(dinner.dinnerType, selectedStyle, 1);
      setPrice(priceInfo.totalPrice);
    } catch (error) {
      console.error('가격 계산 오류:', error);
    }
  };
  
  return (
    <TouchableOpacity style={styles.card} onPress={() => onSelect(dinner, selectedStyle)}>
      <Text style={styles.title}>{dinner.name}</Text>
      <Text style={styles.description}>{dinner.description}</Text>
      
      <View style={styles.styleSelector}>
        {Object.keys(dinner.pricesByStyle).map(style => (
          <TouchableOpacity
            key={style}
            style={[
              styles.styleButton,
              selectedStyle === style && styles.selectedStyle
            ]}
            onPress={() => setSelectedStyle(style)}
          >
            <Text style={styles.styleText}>{style}</Text>
          </TouchableOpacity>
        ))}
      </View>
      
      <Text style={styles.price}>{price}</Text>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    margin: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  description: {
    fontSize: 14,
    color: '#666',
    marginBottom: 12,
  },
  styleSelector: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  styleButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    marginRight: 8,
    borderRadius: 16,
    backgroundColor: '#f0f0f0',
  },
  selectedStyle: {
    backgroundColor: '#2980B9',
  },
  styleText: {
    fontSize: 12,
    color: '#333',
  },
  price: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#27AE60',
  },
});
```

#### 주문 화면 컴포넌트
```javascript
const OrderScreen = ({ route, navigation }) => {
  const { dinner, servingStyle } = route.params;
  const [quantity, setQuantity] = useState(1);
  const [totalPrice, setTotalPrice] = useState('');
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    calculateTotalPrice();
  }, [quantity]);
  
  const calculateTotalPrice = async () => {
    try {
      const priceInfo = await calculatePrice(dinner.dinnerType, servingStyle, quantity);
      setTotalPrice(priceInfo.totalPrice);
    } catch (error) {
      console.error('가격 계산 오류:', error);
    }
  };
  
  const handleOrder = async () => {
    setLoading(true);
    try {
      const order = await createOrder({
        dinnerType: dinner.dinnerType,
        servingStyle: servingStyle,
        quantity: quantity,
        deliveryAddress: {
          streetAddress: '기본 주소', // 사용자 주소
          city: '서울시',
          state: '강남구',
          postalCode: '06292'
        }
      });
      
      navigation.navigate('OrderSuccess', { order });
    } catch (error) {
      Alert.alert('주문 실패', error.message);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <View style={styles.container}>
      <Text style={styles.title}>{dinner.name}</Text>
      <Text style={styles.servingStyle}>서빙 스타일: {servingStyle}</Text>
      
      <View style={styles.quantitySelector}>
        <TouchableOpacity onPress={() => setQuantity(Math.max(1, quantity - 1))}>
          <Text style={styles.quantityButton}>-</Text>
        </TouchableOpacity>
        <Text style={styles.quantity}>{quantity}</Text>
        <TouchableOpacity onPress={() => setQuantity(quantity + 1)}>
          <Text style={styles.quantityButton}>+</Text>
        </TouchableOpacity>
      </View>
      
      <Text style={styles.totalPrice}>총 금액: {totalPrice}</Text>
      
      <TouchableOpacity 
        style={[styles.orderButton, loading && styles.disabledButton]}
        onPress={handleOrder}
        disabled={loading}
      >
        <Text style={styles.orderButtonText}>
          {loading ? '주문 처리 중...' : '주문하기'}
        </Text>
      </TouchableOpacity>
    </View>
  );
};
```

### 6. 에러 처리 및 사용자 경험

#### 전역 에러 처리
```javascript
// API 호출 래퍼 함수
const apiCall = async (apiFunction, ...args) => {
  try {
    return await apiFunction(...args);
  } catch (error) {
    if (error.response?.status === 401) {
      // 인증 오류 - 로그아웃 처리
      await AsyncStorage.removeItem('authToken');
      await AsyncStorage.removeItem('userInfo');
      navigation.navigate('Login');
    } else if (error.response?.status === 400) {
      // 잘못된 요청
      Alert.alert('오류', error.response.data.message);
    } else {
      // 기타 오류
      Alert.alert('오류', '네트워크 오류가 발생했습니다. 다시 시도해주세요.');
    }
    throw error;
  }
};
```

#### 로딩 상태 관리
```javascript
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

const fetchData = async () => {
  setLoading(true);
  setError(null);
  
  try {
    const data = await apiCall(getDinnerMenus);
    setMenus(data);
  } catch (err) {
    setError(err.message);
  } finally {
    setLoading(false);
  }
};
```

이제 모바일 앱에서 이 API들을 호출하여 백엔드와 완전히 연동된 미스터 대박 디너 서비스를 구현할 수 있습니다!
