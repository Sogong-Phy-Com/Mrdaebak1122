import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './Order.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface Dinner {
  id: number;
  name: string;
  name_en: string;
  base_price: number;
  description: string;
  menu_items: MenuItem[];
}

interface MenuItem {
  id: number;
  name: string;
  name_en: string;
  price: number;
  category: string;
  quantity?: number;
}

interface ServingStyle {
  name: string;
  name_ko: string;
  price_multiplier: number;
  description: string;
}

const Order: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [dinners, setDinners] = useState<Dinner[]>([]);
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  const [servingStyles, setServingStyles] = useState<ServingStyle[]>([]);
  const [selectedDinner, setSelectedDinner] = useState<number | null>(null);
  const [selectedStyle, setSelectedStyle] = useState<string>('simple');
  const [deliveryTime, setDeliveryTime] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState(user?.address || '');
  const [orderItems, setOrderItems] = useState<{ menu_item_id: number; quantity: number }[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [inventoryAvailable, setInventoryAvailable] = useState(true);

  useEffect(() => {
    fetchDinners();
    fetchMenuItems();
    fetchServingStyles();
  }, []);

  useEffect(() => {
    if (selectedDinner) {
      const dinner = dinners.find(d => d.id === selectedDinner);
      if (dinner) {
        const items = dinner.menu_items.map(item => ({
          menu_item_id: item.id,
          quantity: 1
        }));
        setOrderItems(items);
      }
    }
  }, [selectedDinner, dinners]);

  // 재고 확인 (배달 시간이 선택되고 디너가 선택되었을 때)
  useEffect(() => {
    const checkInventory = async () => {
      if (!selectedDinner || !deliveryTime || orderItems.length === 0) {
        setInventoryAvailable(true);
        return;
      }

      try {
        const menuItemIds = orderItems.map(item => item.menu_item_id).join(',');
        const response = await axios.get(`${API_URL}/inventory/check-availability`, {
          params: {
            menuItemIds: menuItemIds,
            deliveryTime: deliveryTime
          }
        });

        // 모든 메뉴 아이템이 재고가 있는지 확인
        const allAvailable = orderItems.every(item => response.data[item.menu_item_id] === true);
        setInventoryAvailable(allAvailable);
      } catch (err) {
        console.error('재고 확인 실패:', err);
        setInventoryAvailable(false);
      }
    };

    checkInventory();
  }, [selectedDinner, deliveryTime, orderItems]);

  const fetchDinners = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/menu/dinners`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      setDinners(response.data);
    } catch (err) {
      console.error('디너 목록 조회 실패:', err);
    }
  };

  const fetchMenuItems = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/menu/items`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      setMenuItems(response.data);
    } catch (err) {
      console.error('메뉴 항목 조회 실패:', err);
    }
  };

  const fetchServingStyles = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/menu/serving-styles`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      setServingStyles(response.data);
    } catch (err) {
      console.error('서빙 스타일 조회 실패:', err);
    }
  };

  const startVoiceRecognition = () => {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      alert('이 브라우저는 음성 인식을 지원하지 않습니다.');
      return;
    }

    const SpeechRecognition = (window as any).webkitSpeechRecognition || (window as any).SpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.lang = 'ko-KR';
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onstart = () => {
      setIsListening(true);
      setTranscript('');
    };

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      setTranscript(transcript);
      processVoiceOrder(transcript);
    };

    recognition.onerror = (event: any) => {
      console.error('음성 인식 오류:', event.error);
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognition.start();
  };

  const processVoiceOrder = (transcript: string) => {
    // 간단한 음성 주문 처리 로직
    const lowerTranscript = transcript.toLowerCase();
    
    if (lowerTranscript.includes('발렌타인')) {
      const valentineDinner = dinners.find(d => d.name.includes('발렌타인'));
      if (valentineDinner) setSelectedDinner(valentineDinner.id);
    } else if (lowerTranscript.includes('프렌치')) {
      const frenchDinner = dinners.find(d => d.name.includes('프렌치'));
      if (frenchDinner) setSelectedDinner(frenchDinner.id);
    } else if (lowerTranscript.includes('잉글리시')) {
      const englishDinner = dinners.find(d => d.name.includes('잉글리시'));
      if (englishDinner) setSelectedDinner(englishDinner.id);
    } else if (lowerTranscript.includes('샴페인')) {
      const champagneDinner = dinners.find(d => d.name.includes('샴페인'));
      if (champagneDinner) setSelectedDinner(champagneDinner.id);
    }
  };

  const updateItemQuantity = (menuItemId: number, delta: number) => {
    setOrderItems(prev => {
      const existing = prev.find(item => item.menu_item_id === menuItemId);
      if (existing) {
        const newQuantity = existing.quantity + delta;
        if (newQuantity <= 0) {
          return prev.filter(item => item.menu_item_id !== menuItemId);
        }
        return prev.map(item =>
          item.menu_item_id === menuItemId
            ? { ...item, quantity: newQuantity }
            : item
        );
      } else if (delta > 0) {
        return [...prev, { menu_item_id: menuItemId, quantity: 1 }];
      }
      return prev;
    });
  };

  const calculateTotal = () => {
    if (!selectedDinner) return 0;
    
    const dinner = dinners.find(d => d.id === selectedDinner);
    if (!dinner) return 0;

    const style = servingStyles.find(s => s.name === selectedStyle);
    const styleMultiplier = style?.price_multiplier || 1;

    const basePrice = dinner.base_price * styleMultiplier;
    const itemsPrice = orderItems.reduce((sum, item) => {
      const menuItem = menuItems.find(m => m.id === item.menu_item_id);
      return sum + (menuItem?.price || 0) * item.quantity;
    }, 0);

    return basePrice + itemsPrice;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!selectedDinner) {
      setError('디너를 선택해주세요.');
      return;
    }

    if (!deliveryTime) {
      setError('배달 시간을 입력해주세요.');
      return;
    }

    if (!deliveryAddress) {
      setError('배달 주소를 입력해주세요.');
      return;
    }

    if (orderItems.length === 0) {
      setError('주문 항목을 선택해주세요.');
      return;
    }

    // 중복 제출 방지
    if (loading) {
      return;
    }

    setLoading(true);
    setError(''); // 에러 초기화

    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('로그인이 필요합니다.');
        setLoading(false);
        navigate('/login');
        return;
      }

      const response = await axios.post(`${API_URL}/orders`, {
        dinner_type_id: selectedDinner,
        serving_style: selectedStyle,
        delivery_time: deliveryTime,
        delivery_address: deliveryAddress,
        items: orderItems,
        payment_method: 'card'
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      console.log('[주문 생성] 성공:', response.data);
      // 응답 형식에 따라 orderId 추출
      const orderId = response.data.order_id || response.data.id || response.data.order?.id || response.data.order_id;
      if (orderId) {
        navigate(`/delivery/${orderId}`);
      } else {
        // orderId가 없어도 주문은 성공했을 수 있으므로 주문 목록으로 이동
        console.warn('[주문 생성] orderId를 찾을 수 없지만 주문은 성공했습니다:', response.data);
        navigate('/orders');
      }
    } catch (err: any) {
      console.error('[주문 생성] 실패');
      console.error('[주문 생성] 에러:', err);
      
      if (err.response) {
        const status = err.response.status;
        const errorData = err.response.data;
        console.error('[주문 생성] HTTP 상태 코드:', status);
        console.error('[주문 생성] 응답 데이터:', errorData);
        
        if (status === 403) {
          const userStr = localStorage.getItem('user');
          const user = userStr ? JSON.parse(userStr) : null;
          setError(`[권한 없음] 주문 권한이 없습니다. (상태: 403)\n현재 역할: ${user?.role || '알 수 없음'}\n상세: ${JSON.stringify(errorData)}`);
        } else if (status === 401) {
          setError(`[인증 실패] 로그인이 필요합니다. (상태: 401)\n상세: ${JSON.stringify(errorData)}`);
        } else if (status === 400) {
          const validationErrors = errorData.errors || errorData;
          if (Array.isArray(validationErrors)) {
            setError(`[입력 오류]\n${validationErrors.map((e: any) => e.message || e).join('\n')}`);
          } else if (typeof validationErrors === 'object') {
            setError(`[입력 오류]\n${JSON.stringify(validationErrors, null, 2)}`);
          } else {
            setError(`[입력 오류] ${errorData.message || errorData}`);
          }
        } else {
          setError(`[주문 생성 실패] 서버 오류가 발생했습니다. (상태: ${status})\n상세: ${JSON.stringify(errorData)}`);
        }
      } else {
        setError('[주문 생성 실패] 네트워크 오류가 발생했습니다.\n서버에 연결할 수 없습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const selectedDinnerData = dinners.find(d => d.id === selectedDinner);
  const isChampagneDinner = selectedDinnerData?.name.includes('샴페인');

  return (
    <div className="order-page">
      <TopLogo />

      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>
        <h2>주문하기</h2>

        <div className="voice-section">
          <button
            type="button"
            onClick={startVoiceRecognition}
            className={`btn ${isListening ? 'btn-listening' : 'btn-primary'}`}
            disabled={isListening}
          >
            {isListening ? '🎤 듣는 중...' : '🎤 음성으로 주문하기'}
          </button>
          {transcript && (
            <div className="voice-transcript">
              인식된 내용: {transcript}
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="order-form">
          <div className="form-group">
            <label>디너 선택</label>
            <div className="dinner-grid">
              {dinners.map(dinner => (
                <div
                  key={dinner.id}
                  className={`dinner-card ${selectedDinner === dinner.id ? 'selected' : ''}`}
                  onClick={() => setSelectedDinner(dinner.id)}
                >
                  <h3>{dinner.name}</h3>
                  <p>{dinner.description}</p>
                  <div className="price">{dinner.base_price.toLocaleString()}원</div>
                </div>
              ))}
            </div>
          </div>

          {selectedDinner && (
            <>
              <div className="form-group">
                <label>서빙 스타일</label>
                <div className="style-grid">
                  {servingStyles.map(style => {
                    const disabled = isChampagneDinner && style.name === 'simple';
                    return (
                      <label
                        key={style.name}
                        className={`style-option ${disabled ? 'disabled' : ''} ${selectedStyle === style.name ? 'selected' : ''}`}
                      >
                        <input
                          type="radio"
                          name="style"
                          value={style.name}
                          checked={selectedStyle === style.name}
                          onChange={(e) => setSelectedStyle(e.target.value)}
                          disabled={disabled}
                        />
                        <div className="style-name">{style.name_ko}</div>
                        <div className="style-price">
                          {style.price_multiplier > 1 ? `+${((style.price_multiplier - 1) * 100).toFixed(0)}%` : '기본'}
                        </div>
                      </label>
                    );
                  })}
                </div>
              </div>

              <div className="form-group">
                <label>주문 항목</label>
                <div className="order-items-section">
                  {selectedDinnerData?.menu_items.map(item => {
                    const orderItem = orderItems.find(oi => oi.menu_item_id === item.id);
                    const quantity = orderItem?.quantity || 0;
                    return (
                      <div key={item.id} className="order-item">
                        <span>{item.name} - {item.price.toLocaleString()}원</span>
                        <div className="quantity-controls">
                          <button
                            type="button"
                            onClick={() => updateItemQuantity(item.id, -1)}
                            className="btn btn-secondary"
                          >
                            -
                          </button>
                          <span className="quantity">{quantity}</span>
                          <button
                            type="button"
                            onClick={() => updateItemQuantity(item.id, 1)}
                            className="btn btn-secondary"
                          >
                            +
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="form-group">
                <label>배달 시간</label>
                <input
                  type="datetime-local"
                  value={deliveryTime}
                  onChange={(e) => setDeliveryTime(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>배달 주소</label>
                <textarea
                  value={deliveryAddress}
                  onChange={(e) => setDeliveryAddress(e.target.value)}
                  required
                  rows={3}
                />
              </div>

              <div className="total-price">
                <h3>총 가격</h3>
                <div className="amount">{calculateTotal().toLocaleString()}원</div>
              </div>

              {error && <div className="error">{error}</div>}
              
              {!inventoryAvailable && (
                <div className="error" style={{ marginBottom: '10px' }}>
                  재고가 부족하여 주문할 수 없습니다.
                </div>
              )}

              <button 
                type="submit" 
                className="btn btn-primary submit-button" 
                disabled={loading || !inventoryAvailable}
              >
                {loading ? '주문 처리 중...' : '주문하기'}
              </button>
            </>
          )}
        </form>
      </div>
    </div>
  );
};

export default Order;
