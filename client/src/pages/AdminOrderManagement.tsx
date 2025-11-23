import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

const AdminOrderManagement: React.FC = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<any[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState('');

  useEffect(() => {
    fetchOrders();
  }, []);

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('관리자 로그인이 필요합니다.');
    }
    return {
      Authorization: `Bearer ${token}`
    };
  };

  const fetchOrders = async () => {
    try {
      setOrdersLoading(true);
      setOrdersError('');
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/employee/orders`, { headers });
      setOrders(response.data);
    } catch (err: any) {
      setOrdersError(err.response?.data?.error || err.message || '주문 목록을 불러오는데 실패했습니다.');
      setOrders([]);
    } finally {
      setOrdersLoading(false);
    }
  };


  return (
    <div className="admin-dashboard">
      <TopLogo />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
          <button onClick={() => navigate('/admin/inventory')} className="btn btn-primary" style={{ marginLeft: '10px' }}>
            📦 재고 관리
          </button>
          <button onClick={() => navigate('/schedule')} className="btn btn-primary" style={{ marginLeft: '10px' }}>
            📅 스케줄 캘린더
          </button>
        </div>

        <div className="admin-section">
          <h2>주문 관리 및 작업 할당</h2>
          {ordersError && <div className="error">{ordersError}</div>}
          {ordersLoading ? (
            <div className="loading">주문 목록을 불러오는 중...</div>
          ) : (
            <div className="users-table">
              {orders.length === 0 ? (
                <p style={{ textAlign: 'center', padding: '20px' }}>주문이 없습니다.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>주문 ID</th>
                      <th>고객</th>
                      <th>디너</th>
                      <th>상태</th>
                      <th>조리 담당</th>
                      <th>배달 담당</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.map((order: any) => (
                      <tr key={order.id}>
                        <td>{order.id}</td>
                        <td>
                          <div className="text-strong">{order.customer_name}</div>
                          <div className="text-muted">{order.customer_phone}</div>
                        </td>
                        <td>{order.dinner_name}</td>
                        <td>
                          <span className={`status-badge ${order.status}`}>
                            {order.status}
                          </span>
                        </td>
                        <td>{order.cooking_employee_name || '자동 할당 대기'}</td>
                        <td>{order.delivery_employee_name || '자동 할당 대기'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminOrderManagement;

