import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './EmployeeDashboard.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5000/api';

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
  customer_name: string;
  customer_phone: string;
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

const EmployeeDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchOrders();
  }, [filterStatus]);

  const fetchOrders = async () => {
    console.log('[EmployeeDashboard] 주문 목록 조회 시작');
    
    try {
      const token = localStorage.getItem('token');
      console.log('[EmployeeDashboard] 토큰 확인:', token ? '토큰 존재' : '토큰 없음');
      
      if (!token) {
        setError('[에러] 로그인이 필요합니다. (토큰 없음)');
        setLoading(false);
        return;
      }

      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      console.log('[EmployeeDashboard] 사용자 정보:', user ? `ID: ${user.id}, 역할: ${user.role}` : '사용자 정보 없음');

      const url = filterStatus
        ? `${API_URL}/employee/orders?status=${filterStatus}`
        : `${API_URL}/employee/orders`;
      
      console.log('[EmployeeDashboard] API 요청 URL:', url);
      
      const response = await axios.get(url, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      console.log('[EmployeeDashboard] API 응답 성공:', response.data);
      setOrders(response.data);
    } catch (err: any) {
      console.error('[EmployeeDashboard] 주문 목록 조회 실패');
      console.error('[EmployeeDashboard] 에러:', err);
      
      if (err.response) {
        const status = err.response.status;
        const errorData = err.response.data;
        console.error('[EmployeeDashboard] HTTP 상태 코드:', status);
        console.error('[EmployeeDashboard] 응답 데이터:', errorData);
        
        if (status === 403) {
          const userStr = localStorage.getItem('user');
          const user = userStr ? JSON.parse(userStr) : null;
          setError(`[권한 없음] 직원 권한이 필요합니다. (상태: 403)\n현재 역할: ${user?.role || '알 수 없음'}\n상세: ${JSON.stringify(errorData)}`);
        } else if (status === 401) {
          setError(`[인증 실패] 로그인이 필요합니다. (상태: 401)\n상세: ${JSON.stringify(errorData)}`);
        } else {
          setError(`[오류] 주문 목록을 불러오는데 실패했습니다. (상태: ${status})\n상세: ${JSON.stringify(errorData)}`);
        }
      } else {
        setError('[오류] 주문 목록을 불러오는데 실패했습니다.\n서버에 연결할 수 없습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const updateOrderStatus = async (orderId: number, newStatus: string) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('로그인이 필요합니다.');
        return;
      }

      await axios.patch(`${API_URL}/employee/orders/${orderId}/status`, {
        status: newStatus
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      fetchOrders();
    } catch (err: any) {
      console.error('[EmployeeDashboard] 주문 상태 업데이트 실패:', err);
      if (err.response) {
        setError(`주문 상태 업데이트에 실패했습니다. (상태: ${err.response.status})`);
      } else {
        setError('주문 상태 업데이트에 실패했습니다.');
      }
    }
  };

  const getStatusLabel = (status: string) => {
    const labels: { [key: string]: string } = {
      pending: '대기 중',
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

  const getNextStatus = (currentStatus: string) => {
    const statusFlow: { [key: string]: string } = {
      pending: 'cooking',
      cooking: 'ready',
      ready: 'out_for_delivery',
      out_for_delivery: 'delivered'
    };
    return statusFlow[currentStatus];
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="employee-dashboard">
      <nav className="navbar">
        <div className="nav-container">
          <h1 className="logo">직원 대시보드</h1>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            홈으로
          </button>
        </div>
      </nav>

      <div className="container">
        <h2>주문 관리</h2>

        <div className="filter-section">
          <label>상태 필터:</label>
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="filter-select"
          >
            <option value="">전체</option>
            <option value="pending">대기 중</option>
            <option value="cooking">조리 중</option>
            <option value="ready">준비 완료</option>
            <option value="out_for_delivery">배달 중</option>
            <option value="delivered">배달 완료</option>
          </select>
        </div>

        {error && <div className="error">{error}</div>}

        <div className="orders-list">
          {orders.length === 0 ? (
            <div className="no-orders">
              <p>주문이 없습니다.</p>
            </div>
          ) : (
            orders.map(order => {
              const nextStatus = getNextStatus(order.status);
              return (
                <div key={order.id} className="order-card">
                  <div className="order-header">
                    <div>
                      <h3>주문 #{order.id} - {order.dinner_name}</h3>
                      <p className="customer-info">
                        고객: {order.customer_name} ({order.customer_phone})
                      </p>
                    </div>
                    <span className={`status-badge ${getStatusClass(order.status)}`}>
                      {getStatusLabel(order.status)}
                    </span>
                  </div>

                  <div className="order-details">
                    <div className="detail-row">
                      <span className="label">서빙 스타일:</span>
                      <span>{order.serving_style}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">배달 시간:</span>
                      <span>{new Date(order.delivery_time).toLocaleString('ko-KR')}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">배달 주소:</span>
                      <span>{order.delivery_address}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">주문 시간:</span>
                      <span>{new Date(order.created_at).toLocaleString('ko-KR')}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">총 가격:</span>
                      <span><strong>{order.total_price.toLocaleString()}원</strong></span>
                    </div>
                  </div>

                  <div className="order-items-section">
                    <h4>주문 항목:</h4>
                    <ul>
                      {order.items.map(item => (
                        <li key={item.id}>
                          {item.name} x{item.quantity}
                        </li>
                      ))}
                    </ul>
                  </div>

                  {nextStatus && order.status !== 'delivered' && order.status !== 'cancelled' && (
                    <div className="order-actions">
                      <button
                        onClick={() => updateOrderStatus(order.id, nextStatus)}
                        className="btn btn-success"
                      >
                        {getStatusLabel(nextStatus)}로 변경
                      </button>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

export default EmployeeDashboard;

