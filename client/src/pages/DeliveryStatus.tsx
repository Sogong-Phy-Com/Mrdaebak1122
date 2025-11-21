import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './DeliveryStatus.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface Order {
  id: number;
  dinner_name: string;
  serving_style: string;
  delivery_time: string;
  delivery_address: string;
  total_price: number;
  status: string;
  payment_status: string;
  created_at: string;
}

const DeliveryStatus: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchOrder = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      const response = await axios.get(`${API_URL}/orders`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      const foundOrder = response.data.find((o: Order) => o.id === Number(orderId));
      if (foundOrder) {
        setOrder(foundOrder);
      } else {
        navigate('/orders');
      }
    } catch (err) {
      console.error('주문 조회 실패:', err);
      navigate('/orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!orderId) {
      navigate('/orders');
      return;
    }
    fetchOrder();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId, navigate]);

  const getStatusSteps = () => {
    const steps = [
      { key: 'pending', label: '주문 접수', icon: '📝' },
      { key: 'cooking', label: '조리 중', icon: '👨‍🍳' },
      { key: 'ready', label: '준비 완료', icon: '✅' },
      { key: 'out_for_delivery', label: '배달 중', icon: '🚚' },
      { key: 'delivered', label: '배달 완료', icon: '🎉' }
    ];
    return steps;
  };

  const getCurrentStepIndex = () => {
    const steps = getStatusSteps();
    return steps.findIndex(step => step.key === order?.status);
  };

  if (loading) {
    return (
      <div className="delivery-status-page">
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="delivery-status-page">
        <div className="error">주문을 찾을 수 없습니다.</div>
      </div>
    );
  }

  const steps = getStatusSteps();
  const currentStepIndex = getCurrentStepIndex();

  return (
    <div className="delivery-status-page">
      <TopLogo />

      <div className="page-content">
        <div className="container">
          {/* 주문 정보 카드 */}
          <div className="order-info-card">
            <div className="order-header">
              <h2>주문 #{order.id}</h2>
              <span className={`status-badge status-${order.status}`}>
                {steps[currentStepIndex]?.label || order.status}
              </span>
            </div>
            <div className="order-details">
              <div className="detail-item">
                <span className="detail-label">디너</span>
                <span className="detail-value">{order.dinner_name}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">배달 주소</span>
                <span className="detail-value">{order.delivery_address}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">배달 시간</span>
                <span className="detail-value">
                  {new Date(order.delivery_time).toLocaleString('ko-KR')}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">총 금액</span>
                <span className="detail-value price">{order.total_price.toLocaleString()}원</span>
              </div>
            </div>
          </div>

          {/* 배달 진행 상황 */}
          <div className="delivery-timeline">
            <h3 className="timeline-title">배달 진행 상황</h3>
            {steps.map((step, index) => {
              const isCompleted = index <= currentStepIndex;
              const isCurrent = index === currentStepIndex;
              
              return (
                <div key={step.key} className={`timeline-step ${isCompleted ? 'completed' : ''} ${isCurrent ? 'current' : ''}`}>
                  <div className="timeline-icon">
                    {isCompleted ? (
                      <div className="icon-circle completed">
                        <span>{step.icon}</span>
                      </div>
                    ) : (
                      <div className="icon-circle">
                        <span>{step.icon}</span>
                      </div>
                    )}
                  </div>
                  <div className="timeline-content">
                    <div className="timeline-label">{step.label}</div>
                    {isCurrent && (
                      <div className="timeline-status">진행 중</div>
                    )}
                    {isCompleted && !isCurrent && (
                      <div className="timeline-status completed">완료</div>
                    )}
                  </div>
                  {index < steps.length - 1 && (
                    <div className={`timeline-line ${isCompleted ? 'completed' : ''}`} />
                  )}
                </div>
              );
            })}
          </div>

          {/* 예상 도착 시간 */}
          {order.status !== 'delivered' && (
            <div className="estimated-time">
              <div className="estimated-time-icon">⏰</div>
              <div className="estimated-time-content">
                <div className="estimated-time-label">예상 도착 시간</div>
                <div className="estimated-time-value">
                  {new Date(order.delivery_time).toLocaleString('ko-KR')}
                </div>
              </div>
            </div>
          )}

          {order.status === 'delivered' && (
            <div className="delivery-complete">
              <div className="complete-icon">🎉</div>
              <h3>배달이 완료되었습니다!</h3>
              <p>맛있게 드세요!</p>
            </div>
          )}
        </div>
      </div>

    </div>
  );
};

export default DeliveryStatus;

