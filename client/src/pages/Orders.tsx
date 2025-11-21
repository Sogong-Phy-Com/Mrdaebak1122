import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './Orders.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface OrderItem {
  id: number;
  menu_item_id: number;
  name: string;
  name_en: string;
  price: number;
  quantity: number;
}

interface Order {
  id: number;
  dinner_name: string;
  dinner_name_en: string;
  serving_style: string;
  delivery_time: string;
  delivery_address: string;
  total_price: number;
  status: string;
  payment_status: string;
  created_at: string;
  items: OrderItem[];
}

const Orders: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, navigate]);

  const fetchOrders = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('로그인이 필요합니다.');
        setLoading(false);
        navigate('/login');
        return;
      }

      const response = await axios.get(`${API_URL}/orders`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!Array.isArray(response.data)) {
        setError('서버 응답 형식이 올바르지 않습니다.');
        setLoading(false);
        return;
      }

      setOrders(response.data);
    } catch (err: any) {
      console.error('주문 목록 조회 실패:', err);
      if (err.response) {
        setError(`주문 목록을 불러오는데 실패했습니다. (상태: ${err.response.status})`);
      } else {
        setError('주문 목록을 불러오는데 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const getStatusLabel = (status: string) => {
    const labels: { [key: string]: string } = {
      pending: '주문 접수',
      cooking: '조리 중',
      ready: '준비 완료',
      out_for_delivery: '배달 중',
      delivered: '배달 완료',
      cancelled: '취소됨'
    };
    return labels[status] || status;
  };

  const getStatusClass = (status: string) => {
    const classes: { [key: string]: string } = {
      pending: 'status-pending',
      cooking: 'status-cooking',
      ready: 'status-ready',
      out_for_delivery: 'status-delivery',
      delivered: 'status-delivered',
      cancelled: 'status-cancelled'
    };
    return classes[status] || '';
  };

  const getStyleLabel = (style: string) => {
    const labels: { [key: string]: string } = {
      simple: '심플',
      grand: '그랜드',
      deluxe: '디럭스'
    };
    return labels[style] || style;
  };

  if (loading) {
    return (
      <div className="orders-page">
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="orders-page">
      <TopLogo />

      <div className="page-content">
        <div className="container">
          <div style={{ marginBottom: '20px' }}>
            <button onClick={() => navigate('/')} className="btn btn-secondary">
              ← 홈으로
            </button>
          </div>
          {error && (
            <div className="error">
              {error}
            </div>
          )}

          {orders.length === 0 ? (
            <div className="no-orders">
              <div className="no-orders-icon">📦</div>
              <h3>주문 내역이 없습니다</h3>
              <p>첫 주문을 시작해보세요!</p>
              <button onClick={() => navigate('/order')} className="btn btn-primary">
                🛒 주문하기
              </button>
            </div>
          ) : (
            <div className="orders-list">
              {orders.map(order => (
                <div key={order.id} className="order-card-modern" onClick={() => navigate(`/delivery/${order.id}`)}>
                  <div className="order-card-header">
                    <div className="order-card-title">
                      <h3>{order.dinner_name}</h3>
                      <span className="order-date">
                        {new Date(order.created_at).toLocaleDateString('ko-KR')}
                      </span>
                    </div>
                    <span className={`status-badge-modern ${getStatusClass(order.status)}`}>
                      {getStatusLabel(order.status)}
                    </span>
                  </div>

                  <div className="order-card-body">
                    <div className="order-info-row">
                      <span className="info-icon">📍</span>
                      <span className="info-text">{order.delivery_address}</span>
                    </div>
                    <div className="order-info-row">
                      <span className="info-icon">⏰</span>
                      <span className="info-text">
                        {new Date(order.delivery_time).toLocaleString('ko-KR')}
                      </span>
                    </div>
                    <div className="order-info-row">
                      <span className="info-icon">🎨</span>
                      <span className="info-text">{getStyleLabel(order.serving_style)} 스타일</span>
                    </div>
                  </div>

                  <div className="order-card-footer">
                    <div className="order-items-preview">
                      {order.items.slice(0, 2).map(item => (
                        <span key={item.id} className="item-tag">
                          {item.name} x{item.quantity}
                        </span>
                      ))}
                      {order.items.length > 2 && (
                        <span className="item-tag">+{order.items.length - 2}개</span>
                      )}
                    </div>
                    <div className="order-total-modern">
                      {order.total_price.toLocaleString()}원
                    </div>
                  </div>

                  {order.status !== 'delivered' && order.status !== 'cancelled' && (
                    <div className="order-action">
                      <button
                        className="btn btn-primary"
                        style={{ width: '100%', marginTop: '12px' }}
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/delivery/${order.id}`);
                        }}
                      >
                        배달 현황 보기
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Orders;
