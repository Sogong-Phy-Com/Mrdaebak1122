import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';
import './AdminDashboard.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface InventoryItem {
  menu_item_id: number;
  menu_item_name?: string;
  menu_item_name_en?: string;
  category?: string;
  capacity_per_window: number;
  reserved: number;
  remaining: number;
  window_start: string;
  window_end: string;
  notes?: string;
}

const AdminInventoryManagement: React.FC = () => {
  const navigate = useNavigate();
  const [inventoryItems, setInventoryItems] = useState<InventoryItem[]>([]);
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [inventoryError, setInventoryError] = useState('');
  const [restockValues, setRestockValues] = useState<Record<number, number>>({});
  const [restockNotes, setRestockNotes] = useState<Record<number, string>>({});
  const [restockMessage, setRestockMessage] = useState('');

  useEffect(() => {
    fetchInventory();
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

  const fetchInventory = async () => {
    try {
      setInventoryLoading(true);
      setInventoryError('');
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/inventory`, { headers });
      if (response.data && Array.isArray(response.data)) {
        setInventoryItems(response.data);
        const defaultValues: Record<number, number> = {};
        const defaultNotes: Record<number, string> = {};
        response.data.forEach((item: InventoryItem) => {
          defaultValues[item.menu_item_id] = item.capacity_per_window;
          defaultNotes[item.menu_item_id] = item.notes || '';
        });
        setRestockValues(defaultValues);
        setRestockNotes(defaultNotes);
      } else {
        setInventoryItems([]);
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '재고 정보를 불러오는데 실패했습니다.';
      setInventoryError(errorMsg);
      setInventoryItems([]);
    } finally {
      setInventoryLoading(false);
    }
  };

  const handleRestock = async (menuItemId: number) => {
    const capacity = restockValues[menuItemId];
    if (!capacity || capacity <= 0) {
      setRestockMessage('보충 수량은 1 이상이어야 합니다.');
      setTimeout(() => setRestockMessage(''), 3000);
      return;
    }
    try {
      setRestockMessage('');
      const headers = getAuthHeaders();
      await axios.post(`${API_URL}/inventory/${menuItemId}/restock`, {
        capacity_per_window: capacity,
        notes: restockNotes[menuItemId] || ''
      }, { headers });
      setRestockMessage('재고가 성공적으로 업데이트되었습니다.');
      setTimeout(() => setRestockMessage(''), 3000);
      await fetchInventory();
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '재고 보충에 실패했습니다.';
      setRestockMessage(errorMsg);
      setTimeout(() => setRestockMessage(''), 5000);
    }
  };

  const formatDateTime = (value: string) => {
    return new Date(value).toLocaleString('ko-KR', { hour12: false });
  };

  return (
    <div className="admin-dashboard">
      <TopLogo />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>

        <div className="admin-section">
          <h2>재고 관리</h2>
          {inventoryError && <div className="error">{inventoryError}</div>}
          {restockMessage && <div className="success">{restockMessage}</div>}

          <div className="users-table">
            {inventoryLoading ? (
              <div className="loading">재고를 불러오는 중...</div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>메뉴</th>
                    <th>카테고리</th>
                    <th>현재 용량</th>
                    <th>예약</th>
                    <th>잔여</th>
                    <th>보충 창</th>
                    <th>비고</th>
                    <th>보충</th>
                  </tr>
                </thead>
                <tbody>
                  {inventoryItems.length === 0 ? (
                    <tr>
                      <td colSpan={8} style={{ textAlign: 'center', padding: '20px' }}>
                        등록된 재고가 없습니다.
                      </td>
                    </tr>
                  ) : (
                    inventoryItems.map(item => (
                      <tr key={item.menu_item_id}>
                        <td>
                          <div className="text-strong">{item.menu_item_name || `메뉴 ${item.menu_item_id}`}</div>
                          <div className="text-muted">{item.menu_item_name_en}</div>
                        </td>
                        <td>{item.category || '-'}</td>
                        <td>{item.capacity_per_window?.toLocaleString()}개</td>
                        <td>{item.reserved?.toLocaleString()}개</td>
                        <td>{item.remaining?.toLocaleString()}개</td>
                        <td>
                          <div>{formatDateTime(item.window_start)}</div>
                          <div className="text-muted">~ {formatDateTime(item.window_end)}</div>
                        </td>
                        <td>{item.notes || '-'}</td>
                        <td>
                          <div className="restock-controls">
                            <input
                              type="number"
                              min={1}
                              value={restockValues[item.menu_item_id] ?? item.capacity_per_window}
                              onChange={(e) =>
                                setRestockValues(prev => ({
                                  ...prev,
                                  [item.menu_item_id]: Number(e.target.value)
                                }))
                              }
                            />
                            <input
                              type="text"
                              placeholder="메모 (선택)"
                              value={restockNotes[item.menu_item_id] ?? ''}
                              onChange={(e) =>
                                setRestockNotes(prev => ({
                                  ...prev,
                                  [item.menu_item_id]: e.target.value
                                }))
                              }
                            />
                            <button
                              className="btn btn-primary"
                              onClick={() => handleRestock(item.menu_item_id)}
                            >
                              보충
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminInventoryManagement;

