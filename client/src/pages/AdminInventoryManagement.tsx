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
  weekly_reserved?: number;
  ordered_quantity?: number;
  window_start: string;
  window_end: string;
  notes?: string;
}

const AdminInventoryManagement: React.FC = () => {
  const navigate = useNavigate();
  const [inventoryItems, setInventoryItems] = useState<InventoryItem[]>([]);
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [inventoryError, setInventoryError] = useState('');
  const [restockValues, setRestockValues] = useState<Record<number, number | ''>>({});
  const [orderedInventory, setOrderedInventory] = useState<Record<number, number>>({});
  const [restockNotes, setRestockNotes] = useState<Record<number, string>>({});
  const [restockMessage, setRestockMessage] = useState('');
  const [selectedItems, setSelectedItems] = useState<Set<number>>(new Set());
  const [bulkRestockValue, setBulkRestockValue] = useState<number | ''>('');

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
        const defaultValues: Record<number, number | ''> = {};
        const defaultNotes: Record<number, string> = {};
        const orderedInv: Record<number, number> = {};
        response.data.forEach((item: InventoryItem) => {
          defaultValues[item.menu_item_id] = ''; // Empty by default
          // Clear all notes
          defaultNotes[item.menu_item_id] = '';
          orderedInv[item.menu_item_id] = item.ordered_quantity || 0;
        });
        setRestockValues(defaultValues);
        setRestockNotes(defaultNotes);
        setOrderedInventory(orderedInv);
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
    const restockValue = restockValues[menuItemId];
    const currentCapacity = inventoryItems.find(item => item.menu_item_id === menuItemId)?.capacity_per_window || 0;
    
    // If restock value is empty or 0, just update ordered inventory
    if (restockValue === '' || restockValue === 0) {
      // Just update ordered inventory to 0
      setOrderedInventory(prev => ({ ...prev, [menuItemId]: 0 }));
      setRestockValues(prev => ({ ...prev, [menuItemId]: '' }));
      return;
    }
    
    // Calculate ordered inventory (restock value - current capacity)
    const ordered = Math.max(0, Number(restockValue) - currentCapacity);
    
    try {
      setRestockMessage('');
      const headers = getAuthHeaders();
      // Save ordered inventory
      await axios.post(`${API_URL}/inventory/${menuItemId}/order`, {
        ordered_quantity: ordered
      }, { headers });
      
      setOrderedInventory(prev => ({ ...prev, [menuItemId]: ordered }));
      setRestockValues(prev => ({ ...prev, [menuItemId]: '' }));
      setRestockMessage('주문 재고가 저장되었습니다.');
      setTimeout(() => setRestockMessage(''), 3000);
      await fetchInventory();
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '주문 재고 저장에 실패했습니다.';
      setRestockMessage(errorMsg);
      setTimeout(() => setRestockMessage(''), 5000);
    }
  };

  const handleBulkRestock = async () => {
    if (selectedItems.size === 0) {
      alert('보충할 항목을 선택해주세요.');
      return;
    }

    if (bulkRestockValue === '' || bulkRestockValue === 0) {
      alert('보충 수량을 입력해주세요.');
      return;
    }

    try {
      setRestockMessage('');
      const headers = getAuthHeaders();
      let successCount = 0;
      let failCount = 0;

      for (const menuItemId of selectedItems) {
        try {
          const currentCapacity = inventoryItems.find(item => item.menu_item_id === menuItemId)?.capacity_per_window || 0;
          const ordered = Math.max(0, Number(bulkRestockValue) - currentCapacity);
          
          await axios.post(`${API_URL}/inventory/${menuItemId}/order`, {
            ordered_quantity: ordered
          }, { headers });
          
          setOrderedInventory(prev => ({ ...prev, [menuItemId]: ordered }));
          successCount++;
        } catch (err: any) {
          console.error(`재고 보충 실패 (메뉴 ID: ${menuItemId}):`, err);
          failCount++;
        }
      }

      setSelectedItems(new Set());
      setBulkRestockValue('');
      setRestockMessage(`${successCount}개 항목의 주문 재고가 저장되었습니다.${failCount > 0 ? ` (${failCount}개 실패)` : ''}`);
      setTimeout(() => setRestockMessage(''), 5000);
      await fetchInventory();
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '일괄 보충에 실패했습니다.';
      setRestockMessage(errorMsg);
      setTimeout(() => setRestockMessage(''), 5000);
    }
  };

  const toggleItemSelection = (menuItemId: number) => {
    setSelectedItems(prev => {
      const newSet = new Set(prev);
      if (newSet.has(menuItemId)) {
        newSet.delete(menuItemId);
      } else {
        newSet.add(menuItemId);
      }
      return newSet;
    });
  };

  const selectAllItems = () => {
    if (selectedItems.size === inventoryItems.length) {
      setSelectedItems(new Set());
    } else {
      setSelectedItems(new Set(inventoryItems.map(item => item.menu_item_id)));
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

          {/* 일괄 보충 섹션 */}
          <div style={{ 
            marginBottom: '20px', 
            padding: '15px', 
            background: '#2a2a2a', 
            borderRadius: '8px',
            border: '1px solid #d4af37'
          }}>
            <h3 style={{ marginTop: 0, marginBottom: '15px', color: '#FFD700' }}>일괄 보충</h3>
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
              <button
                className="btn btn-secondary"
                onClick={selectAllItems}
                style={{ fontSize: '14px' }}
              >
                {selectedItems.size === inventoryItems.length ? '전체 해제' : '전체 선택'}
              </button>
              <span style={{ color: '#fff' }}>
                선택된 항목: {selectedItems.size}개
              </span>
              <input
                type="number"
                min={0}
                placeholder="일괄 보충 수량"
                value={bulkRestockValue === '' ? '' : bulkRestockValue}
                onChange={(e) => {
                  const value = e.target.value === '' ? '' : Number(e.target.value);
                  setBulkRestockValue(value);
                }}
                style={{ 
                  padding: '8px', 
                  borderRadius: '4px', 
                  border: '1px solid #d4af37',
                  background: '#1a1a1a',
                  color: '#fff',
                  width: '150px'
                }}
              />
              <button
                className="btn btn-primary"
                onClick={handleBulkRestock}
                disabled={selectedItems.size === 0 || bulkRestockValue === '' || bulkRestockValue === 0}
              >
                선택 항목 일괄 보충
              </button>
            </div>
          </div>

          <div className="users-table">
            {inventoryLoading ? (
              <div className="loading">재고를 불러오는 중...</div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>
                      <input
                        type="checkbox"
                        checked={selectedItems.size === inventoryItems.length && inventoryItems.length > 0}
                        onChange={selectAllItems}
                        style={{ cursor: 'pointer' }}
                      />
                    </th>
                    <th>메뉴</th>
                    <th>카테고리</th>
                    <th>현재 보유량</th>
                    <th>주문 재고</th>
                    <th>이번주 예약 수량</th>
                    <th>예비 수량</th>
                    <th>보충일</th>
                    <th>비고</th>
                    <th>보충</th>
                  </tr>
                </thead>
                <tbody>
                  {inventoryItems.length === 0 ? (
                    <tr>
                      <td colSpan={10} style={{ textAlign: 'center', padding: '20px' }}>
                        등록된 재고가 없습니다.
                      </td>
                    </tr>
                  ) : (
                    inventoryItems.map(item => (
                      <tr key={item.menu_item_id}>
                        <td>
                          <input
                            type="checkbox"
                            checked={selectedItems.has(item.menu_item_id)}
                            onChange={() => toggleItemSelection(item.menu_item_id)}
                            style={{ cursor: 'pointer' }}
                          />
                        </td>
                        <td>
                          <div className="text-strong">{item.menu_item_name || `메뉴 ${item.menu_item_id}`}</div>
                          <div className="text-muted">{item.menu_item_name_en}</div>
                        </td>
                        <td>{item.category || '-'}</td>
                        <td>{item.capacity_per_window?.toLocaleString()}개</td>
                        <td>
                          {orderedInventory[item.menu_item_id] ? `${orderedInventory[item.menu_item_id].toLocaleString()}개` : '0개'}
                        </td>
                        <td>{item.weekly_reserved || item.reserved || 0}개</td>
                        <td>
                          {(() => {
                            const currentCapacity = item.capacity_per_window || 0;
                            const weeklyReserved = item.weekly_reserved || item.reserved || 0;
                            const spareQuantity = Math.max(0, currentCapacity - weeklyReserved);
                            return `${spareQuantity.toLocaleString()}개`;
                          })()}
                        </td>
                        <td>
                          {(() => {
                            const today = new Date();
                            const dayOfWeek = today.getDay();
                            // Monday = 1, Friday = 5
                            if (dayOfWeek === 1) return '월요일';
                            if (dayOfWeek === 5) return '금요일';
                            // Calculate next restock day
                            const daysUntilMonday = (1 - dayOfWeek + 7) % 7 || 7;
                            const daysUntilFriday = (5 - dayOfWeek + 7) % 7 || 7;
                            const nextRestockDay = Math.min(daysUntilMonday, daysUntilFriday);
                            if (nextRestockDay === daysUntilMonday && nextRestockDay <= daysUntilFriday) return `다음 월요일 (${nextRestockDay}일 후)`;
                            if (nextRestockDay === daysUntilFriday) return `다음 금요일 (${nextRestockDay}일 후)`;
                            return '-';
                          })()}
                        </td>
                        <td>{item.notes || '-'}</td>
                        <td>
                          <div className="restock-controls">
                            <input
                              type="number"
                              min={0}
                              placeholder="보충 수량"
                              value={restockValues[item.menu_item_id] === '' ? '' : (restockValues[item.menu_item_id] || '')}
                              onChange={(e) => {
                                const value = e.target.value === '' ? '' : Number(e.target.value);
                                setRestockValues(prev => ({
                                  ...prev,
                                  [item.menu_item_id]: value
                                }));
                              }}
                            />
                            <input
                              type="text"
                              placeholder="비고 작성"
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

