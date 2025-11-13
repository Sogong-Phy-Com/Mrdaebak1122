import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import './Order.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5000/api';

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

  // Redirect staff to their home
  useEffect(() => {
    if (user && (user.role === 'admin' || user.role === 'employee')) {
      navigate('/');
    }
  }, [user, navigate]);
  const [dinners, setDinners] = useState<Dinner[]>([]);
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  const [servingStyles, setServingStyles] = useState<ServingStyle[]>([]);
  const [selectedDinner, setSelectedDinner] = useState<number | null>(null);
  const [selectedStyle, setSelectedStyle] = useState<string>('simple');
  const [deliveryTime, setDeliveryTime] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState(user?.address || '');
  const [orderItems, setOrderItems] = useState<{ menu_item_id: number; quantity: number }[]>([]);
  const [isListening, setIsListening] = useState(false);
  const [voiceTranscript, setVoiceTranscript] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    if (selectedDinner) {
      const dinner = dinners.find(d => d.id === selectedDinner);
      if (dinner) {
        const items = dinner.menu_items.map(item => ({
          menu_item_id: item.id,
          quantity: item.quantity || 1
        }));
        setOrderItems(items);
      }
    }
  }, [selectedDinner, dinners]);

  const fetchData = async () => {
    try {
      const [dinnersRes, itemsRes, stylesRes] = await Promise.all([
        axios.get(`${API_URL}/menu/dinners`),
        axios.get(`${API_URL}/menu/items`),
        axios.get(`${API_URL}/menu/serving-styles`)
      ]);
      setDinners(dinnersRes.data);
      setMenuItems(itemsRes.data);
      setServingStyles(stylesRes.data);
    } catch (err: any) {
      setError('데이터를 불러오는데 실패했습니다.');
    }
  };

  const startVoiceRecognition = () => {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      setError('이 브라우저는 음성 인식을 지원하지 않습니다.');
      return;
    }

    const SpeechRecognition = (window as any).webkitSpeechRecognition || (window as any).SpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.lang = 'ko-KR';
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onstart = () => {
      setIsListening(true);
      setVoiceTranscript('');
    };

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      setVoiceTranscript(transcript);
      processVoiceCommand(transcript);
    };

    recognition.onerror = (event: any) => {
      setError('음성 인식 오류: ' + event.error);
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognition.start();
  };

  const processVoiceCommand = (transcript: string) => {
    const lowerTranscript = transcript.toLowerCase();

    // Dinner selection
    if (lowerTranscript.includes('발렌타인')) {
      const dinner = dinners.find(d => d.name.includes('발렌타인'));
      if (dinner) setSelectedDinner(dinner.id);
    } else if (lowerTranscript.includes('프렌치')) {
      const dinner = dinners.find(d => d.name.includes('프렌치'));
      if (dinner) setSelectedDinner(dinner.id);
    } else if (lowerTranscript.includes('잉글리시')) {
      const dinner = dinners.find(d => d.name.includes('잉글리시'));
      if (dinner) setSelectedDinner(dinner.id);
    } else if (lowerTranscript.includes('샴페인')) {
      const dinner = dinners.find(d => d.name.includes('샴페인'));
      if (dinner) setSelectedDinner(dinner.id);
    }

    // Style selection
    if (lowerTranscript.includes('심플')) {
      setSelectedStyle('simple');
    } else if (lowerTranscript.includes('그랜드')) {
      setSelectedStyle('grand');
    } else if (lowerTranscript.includes('디럭스')) {
      setSelectedStyle('deluxe');
    }

    // Item modifications
    if (lowerTranscript.includes('추가')) {
      // Simple: add wine if not present
      const wine = menuItems.find(m => m.name.includes('와인'));
      if (wine && !orderItems.find(oi => oi.menu_item_id === wine.id)) {
        setOrderItems([...orderItems, { menu_item_id: wine.id, quantity: 1 }]);
      }
    }
  };

  const updateItemQuantity = (itemId: number, delta: number) => {
    setOrderItems(items => {
      const existing = items.find(i => i.menu_item_id === itemId);
      if (existing) {
        const newQuantity = existing.quantity + delta;
        if (newQuantity <= 0) {
          return items.filter(i => i.menu_item_id !== itemId);
        }
        return items.map(i =>
          i.menu_item_id === itemId ? { ...i, quantity: newQuantity } : i
        );
      } else if (delta > 0) {
        return [...items, { menu_item_id: itemId, quantity: delta }];
      }
      return items;
    });
  };

  const calculateTotal = () => {
    if (!selectedDinner) return 0;
    const dinner = dinners.find(d => d.id === selectedDinner);
    if (!dinner) return 0;

    const style = servingStyles.find(s => s.name === selectedStyle);
    const basePrice = dinner.base_price * (style?.price_multiplier || 1);

    const itemsPrice = orderItems.reduce((sum, item) => {
      const menuItem = menuItems.find(m => m.id === item.menu_item_id);
      return sum + (menuItem?.price || 0) * item.quantity;
    }, 0);

    return Math.round(basePrice + itemsPrice);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (!selectedDinner) {
      setError('디너를 선택해주세요.');
      setLoading(false);
      return;
    }

    if (!deliveryTime) {
      setError('배달 시간을 입력해주세요.');
      setLoading(false);
      return;
    }

    try {
      const token = localStorage.getItem('token');
      console.log('[주문 생성] 토큰 확인:', token ? `토큰 존재 (길이: ${token.length})` : '토큰 없음');
      
      if (!token) {
        setError('로그인이 필요합니다.');
        setLoading(false);
        navigate('/login');
        return;
      }

      // 토큰 앞부분 확인
      console.log('[주문 생성] 토큰 앞부분:', token.substring(0, Math.min(50, token.length)));
      
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      console.log('[주문 생성] 사용자 정보:', user ? `ID: ${user.id}, 역할: ${user.role}` : '사용자 정보 없음');

      console.log('[주문 생성] API 요청 시작:', `${API_URL}/orders`);
      console.log('[주문 생성] 요청 데이터:', {
        dinner_type_id: selectedDinner,
        serving_style: selectedStyle,
        delivery_time: deliveryTime,
        delivery_address: deliveryAddress,
        items: orderItems,
        payment_method: 'card'
      });

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
      alert('주문이 완료되었습니다!');
      navigate('/orders');
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
          setError(`[인증 실패] 로그인이 필요합니다. (상태: 401)\n상세: ${JSON.stringify(errorData)}\n\n토큰을 확인하고 다시 로그인해주세요.`);
          // 로그아웃하지 않고 에러만 표시
          console.error('[주문 생성] 401 에러 - 로그아웃하지 않음');
        } else {
          setError(`[오류] 주문 처리 중 오류가 발생했습니다. (상태: ${status})\n상세: ${errorData?.error || err.message}`);
        }
      } else if (err.request) {
        setError('[네트워크 오류] 서버에 연결할 수 없습니다.\n서버가 실행 중인지 확인하세요.');
      } else {
        setError(`[오류] 주문 처리 중 오류가 발생했습니다.\n${err.message}`);
      }
    } finally {
      setLoading(false);
    }
  };

  const selectedDinnerData = dinners.find(d => d.id === selectedDinner);
  const isChampagneDinner = selectedDinnerData?.name.includes('샴페인');

  return (
    <div className="order-page">
      <nav className="navbar">
        <div className="nav-container">
          <h1 className="logo">미스터 대박</h1>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            홈으로
          </button>
        </div>
      </nav>

      <div className="container">
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
          {voiceTranscript && (
            <div className="voice-transcript">
              인식된 음성: {voiceTranscript}
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="order-form">
          <div className="card">
            <h3>디너 선택</h3>
            <div className="dinner-grid">
              {dinners.map(dinner => (
                <div
                  key={dinner.id}
                  className={`dinner-card ${selectedDinner === dinner.id ? 'selected' : ''}`}
                  onClick={() => setSelectedDinner(dinner.id)}
                >
                  <h4>{dinner.name}</h4>
                  <p>{dinner.description}</p>
                  <p className="price">기본 가격: {dinner.base_price.toLocaleString()}원</p>
                </div>
              ))}
            </div>
          </div>

          {selectedDinner && (
            <>
              <div className="card">
                <h3>서빙 스타일</h3>
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
                      <div>
                        <strong>{style.name_ko}</strong>
                        <p>{style.description}</p>
                        <span>가격 배수: {style.price_multiplier}x</span>
                      </div>
                    </label>
                  );
                })}
                {isChampagneDinner && (
                  <p className="info">샴페인 축제 디너는 그랜드 또는 디럭스 스타일만 선택 가능합니다.</p>
                )}
              </div>

              <div className="card">
                <h3>주문 항목 수정</h3>
                <div className="order-items">
                  {orderItems.map(item => {
                    const menuItem = menuItems.find(m => m.id === item.menu_item_id);
                    if (!menuItem) return null;
                    return (
                      <div key={item.menu_item_id} className="order-item">
                        <span>{menuItem.name} - {menuItem.price.toLocaleString()}원</span>
                        <div className="quantity-controls">
                          <button
                            type="button"
                            onClick={() => updateItemQuantity(item.menu_item_id, -1)}
                            className="btn btn-secondary"
                          >
                            -
                          </button>
                          <span>{item.quantity}</span>
                          <button
                            type="button"
                            onClick={() => updateItemQuantity(item.menu_item_id, 1)}
                            className="btn btn-secondary"
                          >
                            +
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
                <h4>추가 메뉴</h4>
                <div className="additional-items">
                  {menuItems.filter(mi => !orderItems.find(oi => oi.menu_item_id === mi.id)).map(item => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => updateItemQuantity(item.id, 1)}
                      className="btn btn-secondary"
                    >
                      {item.name} 추가 (+{item.price.toLocaleString()}원)
                    </button>
                  ))}
                </div>
              </div>

              <div className="card">
                <h3>배달 정보</h3>
                <div className="form-group">
                  <label>배달 주소</label>
                  <input
                    type="text"
                    value={deliveryAddress}
                    onChange={(e) => setDeliveryAddress(e.target.value)}
                    required
                  />
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
              </div>

              <div className="card">
                <h3>주문 요약</h3>
                <div className="order-summary">
                  <p>디너: {selectedDinnerData?.name}</p>
                  <p>스타일: {servingStyles.find(s => s.name === selectedStyle)?.name_ko}</p>
                  <p>총 가격: <strong>{calculateTotal().toLocaleString()}원</strong></p>
                </div>
              </div>

              {error && <div className="error">{error}</div>}

              <button type="submit" className="btn btn-primary btn-large" disabled={loading}>
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

