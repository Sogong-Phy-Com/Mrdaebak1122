import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';
import './EmployeeDashboard.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

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

const EmployeeInventoryManagement: React.FC = () => {
  const navigate = useNavigate();
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [error, setError] = useState('');
  const [restockItemId, setRestockItemId] = useState<number | null>(null);
  const [restockCapacity, setRestockCapacity] = useState<string>('');
  const [restockNotes, setRestockNotes] = useState<string>('');

  useEffect(() => {
    fetchInventory();
  }, []);

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
      console.error('[EmployeeInventoryManagement] 재고 목록 조회 실패:', err);
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
      console.error('[EmployeeInventoryManagement] 재고 보충 실패:', err);
      if (err.response) {
        setError(`재고 보충에 실패했습니다. (상태: ${err.response.status}): ${err.response.data?.error || '알 수 없는 오류'}`);
      } else {
        setError('재고 보충에 실패했습니다.');
      }
    }
  };

  return (
    <div className="employee-dashboard">
      <TopLogo />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>

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
      </div>
    </div>
  );
};

export default EmployeeInventoryManagement;

