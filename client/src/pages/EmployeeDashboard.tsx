import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './EmployeeDashboard.css';

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
  cooking_employee_id?: number;
  delivery_employee_id?: number;
  cooking_employee_name?: string;
  delivery_employee_name?: string;
  items: OrderItem[];
}

interface InventoryItem {
  menu_item_id: number;
  menu_item_name: string;
  menu_item_name_en: string;
  category: string;
  capacity_per_window: number;
  reserved: number;
  remaining: number;
  window_start: string;
  window_end: string;
  notes: string | null;
}

const EmployeeDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState<'orders' | 'inventory'>('orders');
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [restockItemId, setRestockItemId] = useState<number | null>(null);
  const [restockCapacity, setRestockCapacity] = useState<string>('');
  const [restockNotes, setRestockNotes] = useState<string>('');

  useEffect(() => {
    if (activeTab === 'orders') {
      fetchOrders();
    } else if (activeTab === 'inventory') {
      fetchInventory();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterStatus, activeTab]);

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

  const fetchInventory = async () => {
    setInventoryLoading(true);
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('로그인이 필요합니다.');
        setInventoryLoading(false);
        return;
      }

      const response = await axios.get(`${API_URL}/inventory`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      setInventory(response.data);
    } catch (err: any) {
      console.error('[EmployeeDashboard] 재고 목록 조회 실패:', err);
      if (err.response) {
        setError(`재고 목록을 불러오는데 실패했습니다. (상태: ${err.response.status})`);
      } else {
        setError('재고 목록을 불러오는데 실패했습니다.');
      }
    } finally {
      setInventoryLoading(false);
    }
  };

  const handleRestock = async (menuItemId: number) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('로그인이 필요합니다.');
        return;
      }

      const capacity = parseInt(restockCapacity);
      if (isNaN(capacity) || capacity <= 0) {
        setError('유효한 재고 용량을 입력해주세요.');
        return;
      }

      await axios.post(`${API_URL}/inventory/${menuItemId}/restock`, {
        capacity_per_window: capacity,
        notes: restockNotes || null
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      setRestockItemId(null);
      setRestockCapacity('');
      setRestockNotes('');
      fetchInventory();
      setError('');
    } catch (err: any) {
      console.error('[EmployeeDashboard] 재고 보충 실패:', err);
      if (err.response) {
        setError(`재고 보충에 실패했습니다. (상태: ${err.response.status}): ${err.response.data?.error || '알 수 없는 오류'}`);
      } else {
        setError('재고 보충에 실패했습니다.');
      }
    }
  };

  const exportToExcel = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('로그인이 필요합니다.');
        return;
      }

      const url = filterStatus
        ? `${API_URL}/employee/orders/export?status=${filterStatus}`
        : `${API_URL}/employee/orders/export`;

      const response = await axios.get(url, {
        headers: {
          'Authorization': `Bearer ${token}`
        },
        responseType: 'blob'
      });

      // Create download link
      const url_blob = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url_blob;
      
      // Get filename from Content-Disposition header or use default
      const contentDisposition = response.headers['content-disposition'];
      let filename = 'orders.xlsx';
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }
      
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url_blob);
    } catch (err: any) {
      console.error('[EmployeeDashboard] 엑셀 다운로드 실패:', err);
      if (err.response) {
        setError(`엑셀 다운로드에 실패했습니다. (상태: ${err.response.status})`);
      } else {
        setError('엑셀 다운로드에 실패했습니다.');
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
      <TopLogo />

      <div className="container">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '10px', borderBottom: '2px solid #d4af37' }}>
            <button
              onClick={() => setActiveTab('orders')}
              style={{
                padding: '10px 20px',
                border: 'none',
                background: activeTab === 'orders' ? '#d4af37' : 'transparent',
                color: activeTab === 'orders' ? '#000' : '#d4af37',
                cursor: 'pointer',
                fontWeight: activeTab === 'orders' ? 'bold' : 'normal'
              }}
            >
              주문 관리
            </button>
            <button
              onClick={() => setActiveTab('inventory')}
              style={{
                padding: '10px 20px',
                border: 'none',
                background: activeTab === 'inventory' ? '#d4af37' : 'transparent',
                color: activeTab === 'inventory' ? '#000' : '#d4af37',
                cursor: 'pointer',
                fontWeight: activeTab === 'inventory' ? 'bold' : 'normal'
              }}
            >
              재고 관리
            </button>
          </div>
          <button
            onClick={() => navigate('/schedule')}
            className="btn btn-primary"
            style={{ padding: '8px 16px' }}
          >
            📅 스케줄 캘린더
          </button>
        </div>

        {activeTab === 'orders' && (
          <>
            <h2>주문 관리</h2>

            <div className="filter-section">
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
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
            <button
              onClick={exportToExcel}
              className="btn btn-primary"
              style={{ marginLeft: '10px' }}
            >
              📊 엑셀 다운로드
            </button>
          </div>
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
                    {order.cooking_employee_name && (
                      <div className="detail-row">
                        <span className="label">조리 담당:</span>
                        <span>{order.cooking_employee_name}</span>
                      </div>
                    )}
                    {order.delivery_employee_name && (
                      <div className="detail-row">
                        <span className="label">배달 담당:</span>
                        <span>{order.delivery_employee_name}</span>
                      </div>
                    )}
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
                      {(() => {
                        // 자신이 배당받은 작업인지 확인
                        const isAssignedToMe = 
                          (nextStatus === 'cooking' && order.cooking_employee_id === user?.id) ||
                          (nextStatus === 'out_for_delivery' && order.delivery_employee_id === user?.id) ||
                          (nextStatus === 'delivered' && order.delivery_employee_id === user?.id);
                        
                        const isDisabled = !isAssignedToMe;
                        
                        return (
                          <button
                            onClick={() => updateOrderStatus(order.id, nextStatus)}
                            className="btn btn-success"
                            disabled={isDisabled}
                            title={isDisabled ? '자신이 배당받은 작업만 상태를 변경할 수 있습니다.' : ''}
                            style={{
                              opacity: isDisabled ? 0.5 : 1,
                              cursor: isDisabled ? 'not-allowed' : 'pointer'
                            }}
                          >
                            {getStatusLabel(nextStatus)}로 변경
                          </button>
                        );
                      })()}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
          </>
        )}

        {activeTab === 'inventory' && (
          <>
            <h2>재고 관리</h2>
            {error && <div className="error">{error}</div>}
            
            {inventoryLoading ? (
              <div className="loading">로딩 중...</div>
            ) : (
              <div className="inventory-list">
                {inventory.length === 0 ? (
                  <div className="no-orders">
                    <p>재고 정보가 없습니다.</p>
                  </div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
                    <thead>
                      <tr style={{ background: '#d4af37', color: '#000' }}>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>메뉴 항목</th>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>카테고리</th>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>창구당 용량</th>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>예약됨</th>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>남은 재고</th>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>시간대</th>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>비고</th>
                        <th style={{ padding: '10px', border: '1px solid #000' }}>작업</th>
                      </tr>
                    </thead>
                    <tbody>
                      {inventory.map((item) => (
                        <tr key={item.menu_item_id} style={{ background: item.remaining < 5 ? '#ffcccc' : 'transparent' }}>
                          <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                            {item.menu_item_name} ({item.menu_item_name_en})
                          </td>
                          <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{item.category}</td>
                          <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{item.capacity_per_window}</td>
                          <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{item.reserved}</td>
                          <td style={{ padding: '10px', border: '1px solid #d4af37', fontWeight: item.remaining < 5 ? 'bold' : 'normal' }}>
                            {item.remaining}
                          </td>
                          <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                            {new Date(item.window_start).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} - {new Date(item.window_end).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                          </td>
                          <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{item.notes || '-'}</td>
                          <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                            {restockItemId === item.menu_item_id ? (
                              <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                                <input
                                  type="number"
                                  placeholder="용량"
                                  value={restockCapacity}
                                  onChange={(e) => setRestockCapacity(e.target.value)}
                                  style={{ padding: '5px', width: '80px' }}
                                />
                                <input
                                  type="text"
                                  placeholder="비고 (선택)"
                                  value={restockNotes}
                                  onChange={(e) => setRestockNotes(e.target.value)}
                                  style={{ padding: '5px', width: '150px' }}
                                />
                                <div style={{ display: 'flex', gap: '5px' }}>
                                  <button
                                    onClick={() => handleRestock(item.menu_item_id)}
                                    className="btn btn-success"
                                    style={{ padding: '5px 10px', fontSize: '12px' }}
                                  >
                                    확인
                                  </button>
                                  <button
                                    onClick={() => {
                                      setRestockItemId(null);
                                      setRestockCapacity('');
                                      setRestockNotes('');
                                    }}
                                    className="btn btn-secondary"
                                    style={{ padding: '5px 10px', fontSize: '12px' }}
                                  >
                                    취소
                                  </button>
                                </div>
                              </div>
                            ) : (
                              <button
                                onClick={() => {
                                  setRestockItemId(item.menu_item_id);
                                  setRestockCapacity(item.capacity_per_window.toString());
                                }}
                                className="btn btn-primary"
                                style={{ padding: '5px 10px', fontSize: '12px' }}
                              >
                                보충
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default EmployeeDashboard;

