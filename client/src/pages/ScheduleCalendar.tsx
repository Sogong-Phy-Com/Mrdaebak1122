import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './ScheduleCalendar.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface DeliverySchedule {
  id: number;
  order_id: number;
  employee_id: number;
  employee_name?: string;
  employee_phone?: string;
  delivery_address: string;
  departure_time: string;
  arrival_time: string;
  return_time: string;
  one_way_minutes: number;
  status: string;
}

interface Order {
  id: number;
  customer_name?: string;
  customer_phone?: string;
  dinner_name?: string;
  delivery_time: string;
  delivery_address: string;
  status: string;
  cooking_employee_id?: number;
  delivery_employee_id?: number;
  cooking_employee_name?: string;
  delivery_employee_name?: string;
}

interface User {
  id: number;
  name: string;
  email: string;
  role: string;
}

const ScheduleCalendar: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [currentDate, setCurrentDate] = useState<Date>(new Date());
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | null>(null);
  const [schedules, setSchedules] = useState<DeliverySchedule[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [employees, setEmployees] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedSchedules, setSelectedSchedules] = useState<DeliverySchedule[]>([]);
  const [selectedOrders, setSelectedOrders] = useState<Order[]>([]);
  const [showScheduleModal, setShowScheduleModal] = useState(false);

  const isAdmin = user?.role === 'admin';

  useEffect(() => {
    if (isAdmin) {
      fetchEmployees();
    }
    fetchSchedules();
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentDate, selectedEmployeeId, isAdmin]);

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('로그인이 필요합니다.');
    }
    return {
      Authorization: `Bearer ${token}`
    };
  };

  const fetchEmployees = async () => {
    try {
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/admin/users`, { headers });
      if (response.data && Array.isArray(response.data)) {
        const employeeList = response.data.filter((u: User) => u && u.role === 'employee');
        setEmployees(employeeList);
      } else {
        setEmployees([]);
      }
    } catch (err: any) {
      console.error('직원 목록 조회 실패:', err);
      // Don't show error to user for employee list fetch failure
      // It's not critical - they can still use the calendar
      setEmployees([]);
    }
  };

  const fetchSchedules = async () => {
    try {
      if (!user) {
        return;
      }

      const headers = getAuthHeaders();
      const dateStr = currentDate.toISOString().split('T')[0];
      let url = `${API_URL}/employee/delivery-schedule?date=${dateStr}`;
      
      if (isAdmin && selectedEmployeeId) {
        url += `&employeeId=${selectedEmployeeId}`;
      }

      const response = await axios.get(url, { headers });
      if (response.data && Array.isArray(response.data)) {
        const validSchedules = response.data.filter((schedule: any) => 
          schedule && 
          typeof schedule.id === 'number' &&
          typeof schedule.order_id === 'number' &&
          schedule.departure_time
        );
        setSchedules(validSchedules);
      } else {
        setSchedules([]);
      }
    } catch (err: any) {
      console.error('배달 스케줄 조회 실패:', err);
      setSchedules([]);
    }
  };

  const fetchOrders = async () => {
    try {
      if (!user) {
        return;
      }

      const headers = getAuthHeaders();
      // 관리자는 모든 주문, 직원은 모든 주문을 가져와서 필터링
      const url = `${API_URL}/employee/orders`;
      
      const response = await axios.get(url, { headers });
      if (response.data && Array.isArray(response.data)) {
        // 현재 월의 주문만 필터링
        const currentMonth = currentDate.getMonth();
        const currentYear = currentDate.getFullYear();
        const filteredOrders = response.data.filter((order: Order) => {
          if (!order.delivery_time) return false;
          try {
            let orderDate: Date;
            try {
              orderDate = new Date(order.delivery_time);
            } catch {
              const parts = order.delivery_time.split('T');
              if (parts.length === 2) {
                orderDate = new Date(order.delivery_time + ':00');
              } else {
                return false;
              }
            }
            if (isNaN(orderDate.getTime())) return false;
            return orderDate.getMonth() === currentMonth && orderDate.getFullYear() === currentYear;
          } catch {
            return false;
          }
        });
        setOrders(filteredOrders);
      } else {
        setOrders([]);
      }
    } catch (err: any) {
      console.error('주문 목록 조회 실패:', err);
      setOrders([]);
    }
  };

  const getDaysInMonth = (date: Date): (Date | null)[] => {
    try {
      if (!date || isNaN(date.getTime())) {
        date = new Date(); // Fallback to current date if invalid
      }
      const year = date.getFullYear();
      const month = date.getMonth();
      
      // Validate year and month
      if (year < 1900 || year > 2100 || month < 0 || month > 11) {
        date = new Date(); // Fallback to current date if invalid
        return getDaysInMonth(date);
      }
      
      const firstDay = new Date(year, month, 1);
      const lastDay = new Date(year, month + 1, 0);
      const daysInMonth = lastDay.getDate();
      const startingDayOfWeek = firstDay.getDay();

      const days: (Date | null)[] = [];
      // Add empty cells for days before the first day of the month
      for (let i = 0; i < startingDayOfWeek; i++) {
        days.push(null);
      }
      // Add all days of the month
      for (let day = 1; day <= daysInMonth; day++) {
        const dayDate = new Date(year, month, day);
        if (isNaN(dayDate.getTime())) {
          continue; // Skip invalid dates
        }
        days.push(dayDate);
      }
      return days;
    } catch {
      // Fallback: return current month
      return getDaysInMonth(new Date());
    }
  };

  const getOrdersForDate = (date: Date | null): Order[] => {
    if (!date) return [];
    try {
      const dateStr = date.toISOString().split('T')[0];
      return orders.filter(order => {
        if (!order || !order.delivery_time) return false;
        try {
          // delivery_time 파싱 (다양한 형식 지원)
          let orderDate: Date;
          try {
            orderDate = new Date(order.delivery_time);
          } catch {
            // datetime-local 형식 시도
            const parts = order.delivery_time.split('T');
            if (parts.length === 2) {
              orderDate = new Date(order.delivery_time + ':00');
            } else {
              return false;
            }
          }
          if (isNaN(orderDate.getTime())) return false;
          return orderDate.toISOString().split('T')[0] === dateStr;
        } catch {
          return false;
        }
      });
    } catch {
      return [];
    }
  };

  const getSchedulesForDate = (date: Date | null): DeliverySchedule[] => {
    if (!date) return [];
    try {
      const dateStr = date.toISOString().split('T')[0];
      return schedules.filter(schedule => {
        if (!schedule || !schedule.departure_time) return false;
        try {
          const scheduleDate = new Date(schedule.departure_time);
          if (isNaN(scheduleDate.getTime())) return false;
          return scheduleDate.toISOString().split('T')[0] === dateStr;
        } catch {
          return false;
        }
      });
    } catch {
      return [];
    }
  };

  const hasMySchedule = (date: Date | null): boolean => {
    if (!date || !user) return false;
    const dayOrders = getOrdersForDate(date);
    return dayOrders.some(order => 
      order.cooking_employee_id === user.id || 
      order.delivery_employee_id === user.id
    );
  };

  const getOrderColor = (order: Order): 'red' | 'green' => {
    if (isAdmin) {
      // 관리자: 끝난 주문(delivered, cancelled) = 초록색, 나머지 = 빨간색
      return (order.status === 'delivered' || order.status === 'cancelled') ? 'green' : 'red';
    } else {
      // 직원: 본인 할당 = 빨간색, 나머지 또는 끝난 주문 = 초록색
      const isMyOrder = order.cooking_employee_id === user?.id || order.delivery_employee_id === user?.id;
      if (isMyOrder && order.status !== 'delivered' && order.status !== 'cancelled') {
        return 'red';
      }
      return 'green';
    }
  };

  const handleDateClick = (date: Date | null) => {
    if (!date) return;
    const dayOrders = getOrdersForDate(date);
    const daySchedules = getSchedulesForDate(date);
    if (dayOrders.length > 0 || daySchedules.length > 0) {
      setSelectedDate(date);
      setSelectedOrders(dayOrders);
      setSelectedSchedules(daySchedules);
      setShowScheduleModal(true);
    }
  };

  const navigateMonth = (direction: 'prev' | 'next') => {
    setCurrentDate(prev => {
      try {
        const newDate = new Date(prev);
        if (isNaN(newDate.getTime())) {
          return new Date(); // Fallback to current date if invalid
        }
        if (direction === 'prev') {
          newDate.setMonth(prev.getMonth() - 1);
        } else {
          newDate.setMonth(prev.getMonth() + 1);
        }
        // Validate the new date
        if (isNaN(newDate.getTime())) {
          return new Date(); // Fallback to current date if invalid
        }
        return newDate;
      } catch {
        return new Date(); // Fallback to current date on error
      }
    });
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  const formatTime = (dateString: string) => {
    try {
      if (!dateString) return '--:--';
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return '--:--';
      return date.toLocaleTimeString('ko-KR', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: false 
      });
    } catch {
      return '--:--';
    }
  };

  const getStatusColor = (status: string) => {
    const colors: { [key: string]: string } = {
      SCHEDULED: '#4CAF50',
      IN_PROGRESS: '#2196F3',
      COMPLETED: '#9E9E9E',
      CANCELLED: '#F44336'
    };
    return colors[status] || '#757575';
  };

  const getStatusLabel = (status: string) => {
    const labels: { [key: string]: string } = {
      SCHEDULED: '배정됨',
      IN_PROGRESS: '배달 중',
      COMPLETED: '완료',
      CANCELLED: '취소'
    };
    return labels[status] || status;
  };

  const days = getDaysInMonth(currentDate);
  const monthYear = (() => {
    try {
      if (!currentDate || isNaN(currentDate.getTime())) {
        return new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long' });
      }
      return currentDate.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long' });
    } catch {
      return new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long' });
    }
  })();

  return (
    <div className="schedule-calendar-page">
      <TopLogo />

      <div className="container">
        {error && <div className="error">{error}</div>}

        {/* Employee Filter (Admin only) */}
        {isAdmin && (
          <div className="employee-filter">
            <label>직원 선택:</label>
            <select
              value={selectedEmployeeId || ''}
              onChange={(e) => setSelectedEmployeeId(e.target.value ? Number(e.target.value) : null)}
              className="filter-select"
            >
              <option value="">전체 직원</option>
              {employees.map(emp => (
                <option key={emp.id} value={emp.id}>
                  {emp.name} ({emp.email})
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Calendar Controls */}
        <div className="calendar-controls">
          <button onClick={() => navigateMonth('prev')} className="btn btn-secondary">
            ← 이전 달
          </button>
          <h2>{monthYear}</h2>
          <button onClick={() => navigateMonth('next')} className="btn btn-secondary">
            다음 달 →
          </button>
          <button onClick={goToToday} className="btn btn-primary">
            오늘
          </button>
        </div>

        {/* Calendar Grid */}
        {loading ? (
          <div className="loading">스케줄을 불러오는 중...</div>
        ) : (
          <div className="calendar-grid">
            <div className="calendar-weekdays">
              {['일', '월', '화', '수', '목', '금', '토'].map(day => (
                <div key={day} className="calendar-weekday">{day}</div>
              ))}
            </div>
            <div className="calendar-days">
              {days.map((date: Date | null, index: number) => {
                const dayOrders = getOrdersForDate(date);
                const daySchedules = getSchedulesForDate(date);
                const isToday = date && 
                  date.toDateString() === new Date().toDateString() &&
                  !isNaN(date.getTime());
                const isCurrentMonth = date !== null;

                const hasMySchedules = hasMySchedule(date);
                const isClickable = date && (dayOrders.length > 0 || daySchedules.length > 0);

                return (
                  <div
                    key={index}
                    className={`calendar-day ${!isCurrentMonth ? 'other-month' : ''} ${isToday ? 'today' : ''} ${isClickable ? 'clickable' : ''} ${hasMySchedules ? 'has-my-schedule' : ''}`}
                    onClick={() => isClickable && handleDateClick(date)}
                  >
                    {date && (
                      <>
                        <div className="calendar-day-header">
                          <div className="calendar-day-number">{date.getDate()}</div>
                          {hasMySchedules && <div className="my-schedule-indicator" title="내 배달 일정"></div>}
                        </div>
                        <div className="calendar-day-schedules">
                          {dayOrders.slice(0, 2).map(order => {
                            const orderColor = getOrderColor(order);
                            return (
                              <div
                                key={order.id}
                                className={`schedule-item order-item ${orderColor === 'red' ? 'my-schedule' : 'other-schedule'}`}
                                style={{ borderLeftColor: orderColor === 'red' ? '#ff4444' : '#4CAF50' }}
                                title={`주문 #${order.id} - ${order.delivery_address || '주소 없음'} (${formatTime(order.delivery_time || '')})`}
                              >
                                <div className="schedule-time">{formatTime(order.delivery_time)}</div>
                                <div className="schedule-status" style={{ color: orderColor === 'red' ? '#ff4444' : '#4CAF50' }}>
                                  주문 #{order.id}
                                </div>
                              </div>
                            );
                          })}
                          {dayOrders.length > 2 && (
                            <div className="schedule-more">
                              +{dayOrders.length - 2}개 더
                            </div>
                          )}
                        </div>
                      </>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Schedule Modal */}
        {showScheduleModal && selectedDate && (
          <div className="schedule-modal-overlay" onClick={() => setShowScheduleModal(false)}>
            <div className="schedule-modal" onClick={(e) => e.stopPropagation()}>
              <div className="schedule-modal-header">
                <h2>
                  {selectedDate.toLocaleDateString('ko-KR', { 
                    year: 'numeric', 
                    month: 'long', 
                    day: 'numeric',
                    weekday: 'long'
                  })}
                </h2>
                <button 
                  className="modal-close-btn"
                  onClick={() => setShowScheduleModal(false)}
                >
                  ×
                </button>
              </div>
              <div className="schedule-modal-content">
                {selectedOrders.length === 0 && selectedSchedules.length === 0 ? (
                  <p className="no-schedules">이 날짜에 주문이 없습니다.</p>
                ) : (
                  <div className="schedule-list">
                    {selectedOrders.map(order => {
                      const orderColor = getOrderColor(order);
                      const isMyOrder = order.cooking_employee_id === user?.id || order.delivery_employee_id === user?.id;
                      return (
                        <div 
                          key={order.id} 
                          className={`schedule-card ${orderColor === 'red' ? 'my-schedule-card' : 'other-schedule-card'}`}
                        >
                          <div className="schedule-header">
                            <div>
                              <h4>주문 #{order.id || 'N/A'}</h4>
                              <p className="employee-name">
                                {order.customer_name && `고객: ${order.customer_name}`}
                                {order.dinner_name && ` | ${order.dinner_name}`}
                                {isMyOrder && (
                                  <span className="my-badge">내 배당</span>
                                )}
                              </p>
                              {(order.cooking_employee_name || order.delivery_employee_name) && (
                                <p className="employee-name" style={{ fontSize: '12px', marginTop: '4px' }}>
                                  {order.cooking_employee_name && `조리: ${order.cooking_employee_name}`}
                                  {order.cooking_employee_name && order.delivery_employee_name && ' | '}
                                  {order.delivery_employee_name && `배달: ${order.delivery_employee_name}`}
                                </p>
                              )}
                            </div>
                            <span 
                              className="status-badge"
                              style={{ backgroundColor: orderColor === 'red' ? '#ff4444' : '#4CAF50' }}
                            >
                              {order.status === 'delivered' ? '배달 완료' : 
                               order.status === 'cancelled' ? '취소됨' :
                               order.status === 'cooking' ? '조리 중' :
                               order.status === 'out_for_delivery' ? '배달 중' :
                               order.status === 'ready' ? '준비 완료' : '주문 접수'}
                            </span>
                          </div>
                          <div className="schedule-details">
                            <div className="detail-item">
                              <span className="detail-label">배달 주소:</span>
                              <span className="detail-value">{order.delivery_address || '주소 없음'}</span>
                            </div>
                            <div className="detail-item">
                              <span className="detail-label">배달 시간:</span>
                              <span className="detail-value">{formatTime(order.delivery_time || '')}</span>
                            </div>
                            <div className="detail-item">
                              <span className="detail-label">상태:</span>
                              <span className="detail-value">
                                {order.status === 'delivered' ? '배달 완료' : 
                                 order.status === 'cancelled' ? '취소됨' :
                                 order.status === 'cooking' ? '조리 중' :
                                 order.status === 'out_for_delivery' ? '배달 중' :
                                 order.status === 'ready' ? '준비 완료' : '주문 접수'}
                              </span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                    {selectedSchedules.map(schedule => (
                      <div 
                        key={`schedule-${schedule.id}`} 
                        className={`schedule-card ${schedule.employee_id === user?.id ? 'my-schedule-card' : ''}`}
                      >
                        <div className="schedule-header">
                          <div>
                            <h4>배달 스케줄 #{schedule.id} (주문 #{schedule.order_id || 'N/A'})</h4>
                            <p className="employee-name">
                              {schedule.employee_name || `직원 ${schedule.employee_id || 'N/A'}`}
                              {schedule.employee_phone && ` (${schedule.employee_phone})`}
                              {schedule.employee_id === user?.id && (
                                <span className="my-badge">내 배달</span>
                              )}
                            </p>
                          </div>
                          <span 
                            className="status-badge"
                            style={{ backgroundColor: getStatusColor(schedule.status || 'SCHEDULED') }}
                          >
                            {getStatusLabel(schedule.status || 'SCHEDULED')}
                          </span>
                        </div>
                        <div className="schedule-details">
                          <div className="detail-item">
                            <span className="detail-label">배달 주소:</span>
                            <span className="detail-value">{schedule.delivery_address || '주소 없음'}</span>
                          </div>
                          <div className="detail-item">
                            <span className="detail-label">출발:</span>
                            <span className="detail-value">{formatTime(schedule.departure_time || '')}</span>
                          </div>
                          <div className="detail-item">
                            <span className="detail-label">도착:</span>
                            <span className="detail-value">{formatTime(schedule.arrival_time || '')}</span>
                          </div>
                          <div className="detail-item">
                            <span className="detail-label">복귀:</span>
                            <span className="detail-value">{formatTime(schedule.return_time || '')}</span>
                          </div>
                          <div className="detail-item">
                            <span className="detail-label">소요 시간:</span>
                            <span className="detail-value">{schedule.one_way_minutes || 0}분 (편도)</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ScheduleCalendar;

