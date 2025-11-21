import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';
import './AdminDashboard.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

const AdminOrderManagement: React.FC = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<any[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState('');
  const [users, setUsers] = useState<any[]>([]);
  const [assigningOrderId, setAssigningOrderId] = useState<number | null>(null);
  const [assignCookingEmployee, setAssignCookingEmployee] = useState<number | null>(null);
  const [assignDeliveryEmployee, setAssignDeliveryEmployee] = useState<number | null>(null);

  useEffect(() => {
    fetchOrders();
    fetchUsers();
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

  const fetchUsers = async () => {
    try {
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/admin/users`, { headers });
      setUsers(response.data);
    } catch (err: any) {
      console.error('회원 정보를 불러오는데 실패했습니다:', err);
    }
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

  const handleAssignEmployees = async (orderId: number) => {
    try {
      const headers = getAuthHeaders();
      await axios.post(
        `${API_URL}/admin/orders/${orderId}/assign`,
        {
          cookingEmployeeId: assignCookingEmployee,
          deliveryEmployeeId: assignDeliveryEmployee
        },
        { headers }
      );
      
      setAssigningOrderId(null);
      setAssignCookingEmployee(null);
      setAssignDeliveryEmployee(null);
      await fetchOrders();
    } catch (err: any) {
      setOrdersError(err.response?.data?.error || err.message || '직원 배당에 실패했습니다.');
    }
  };

  const getEmployees = () => {
    return users.filter(u => (u.role === 'employee' || u.role === 'admin') && u.id);
  };

  return (
    <div className="admin-dashboard">
      <TopLogo />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
          <button onClick={() => navigate('/schedule')} className="btn btn-primary" style={{ marginLeft: '10px' }}>
            📅 스케줄 캘린더
          </button>
        </div>

        <div className="admin-section">
          <h2>주문 관리 및 배당</h2>
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
                      <th>작업</th>
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
                        <td>{order.cooking_employee_name || '-'}</td>
                        <td>{order.delivery_employee_name || '-'}</td>
                        <td>
                          {(() => {
                            const isAssigned = order.cooking_employee_id || order.delivery_employee_id;
                            
                            if (assigningOrderId === order.id) {
                              return (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                                  <select
                                    value={assignCookingEmployee || ''}
                                    onChange={(e) => setAssignCookingEmployee(e.target.value ? Number(e.target.value) : null)}
                                    style={{ padding: '5px', fontSize: '12px' }}
                                  >
                                    <option value="">조리 담당 선택</option>
                                    {getEmployees().map(emp => (
                                      <option key={emp.id} value={emp.id}>{emp.name}</option>
                                    ))}
                                  </select>
                                  <select
                                    value={assignDeliveryEmployee || ''}
                                    onChange={(e) => setAssignDeliveryEmployee(e.target.value ? Number(e.target.value) : null)}
                                    style={{ padding: '5px', fontSize: '12px' }}
                                  >
                                    <option value="">배달 담당 선택</option>
                                    {getEmployees().map(emp => (
                                      <option key={emp.id} value={emp.id}>{emp.name}</option>
                                    ))}
                                  </select>
                                  <div style={{ display: 'flex', gap: '5px' }}>
                                    <button
                                      onClick={() => handleAssignEmployees(order.id)}
                                      className="btn btn-primary"
                                      style={{ padding: '5px 10px', fontSize: '12px' }}
                                    >
                                      배당 완료
                                    </button>
                                    <button
                                      onClick={() => {
                                        setAssigningOrderId(null);
                                        setAssignCookingEmployee(null);
                                        setAssignDeliveryEmployee(null);
                                      }}
                                      className="btn btn-secondary"
                                      style={{ padding: '5px 10px', fontSize: '12px' }}
                                    >
                                      취소
                                    </button>
                                  </div>
                                </div>
                              );
                            } else if (isAssigned) {
                              return (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                                  <div style={{ 
                                    padding: '5px 10px', 
                                    fontSize: '12px', 
                                    backgroundColor: '#d4edda', 
                                    color: '#155724',
                                    borderRadius: '4px',
                                    textAlign: 'center'
                                  }}>
                                    배당 완료됨
                                  </div>
                                  <button
                                    onClick={() => {
                                      setAssigningOrderId(order.id);
                                      setAssignCookingEmployee(order.cooking_employee_id || null);
                                      setAssignDeliveryEmployee(order.delivery_employee_id || null);
                                    }}
                                    className="btn btn-secondary"
                                    style={{ padding: '5px 10px', fontSize: '12px' }}
                                  >
                                    배당 변경하기
                                  </button>
                                </div>
                              );
                            } else {
                              return (
                                <button
                                  onClick={() => {
                                    setAssigningOrderId(order.id);
                                    setAssignCookingEmployee(null);
                                    setAssignDeliveryEmployee(null);
                                  }}
                                  className="btn btn-primary"
                                  style={{ padding: '5px 10px', fontSize: '12px' }}
                                >
                                  배당
                                </button>
                              );
                            }
                          })()}
                        </td>
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

