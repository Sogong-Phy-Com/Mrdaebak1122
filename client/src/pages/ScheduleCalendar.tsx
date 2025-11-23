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

interface ScheduleCalendarProps {
  type?: 'schedule' | 'orders';
}

const ScheduleCalendar: React.FC<ScheduleCalendarProps> = ({ type: propType }) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [currentDate, setCurrentDate] = useState<Date>(new Date());
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | null>(null);
  const [schedules, setSchedules] = useState<DeliverySchedule[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [employees, setEmployees] = useState<User[]>([]);
  const [workAssignments, setWorkAssignments] = useState<{[key: string]: {tasks: string[]}}>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedSchedules, setSelectedSchedules] = useState<DeliverySchedule[]>([]);
  const [selectedOrders, setSelectedOrders] = useState<Order[]>([]);
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  
  // Get calendar type from prop or URL parameter
  const [searchParams] = React.useState(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('type') || 'schedule'; // default to 'schedule'
  });
  const calendarType = propType || (searchParams === 'orders' ? 'orders' : 'schedule');

  const isAdmin = user?.role === 'admin';

  useEffect(() => {
    if (isAdmin) {
      fetchEmployees();
    }
    if (calendarType === 'schedule') {
      fetchSchedules();
      fetchWorkAssignments();
    } else {
      fetchOrders();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentDate.getMonth(), currentDate.getFullYear(), selectedEmployeeId, isAdmin, calendarType]);

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

  const fetchWorkAssignments = async () => {
    try {
      if (!user) {
        return;
      }

      const headers = getAuthHeaders();
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth();
      const firstDay = new Date(year, month, 1);
      const lastDay = new Date(year, month + 1, 0);
      
      const startDateStr = firstDay.toISOString().split('T')[0];
      const endDateStr = lastDay.toISOString().split('T')[0];
      
      // 한 번의 API 호출로 월 전체 할당 조회
      try {
        const response = await axios.get(`${API_URL}/employee/schedule/assignments?startDate=${startDateStr}&endDate=${endDateStr}`, { headers });
        if (response.data && typeof response.data === 'object') {
          const assignments: {[key: string]: {tasks: string[]}} = {};
          Object.keys(response.data).forEach((dateStr: string) => {
            const data = response.data[dateStr];
            if (data && data.isWorking && data.tasks) {
              assignments[dateStr] = {
                tasks: data.tasks || []
              };
            }
          });
          setWorkAssignments(assignments);
        } else {
          setWorkAssignments({});
        }
      } catch (err: any) {
        console.error('작업 할당 조회 실패:', err);
        setWorkAssignments({});
      }
    } catch (err: any) {
      console.error('작업 할당 조회 실패:', err);
      setWorkAssignments({});
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
    if (calendarType === 'schedule') {
      // For schedule calendar, check if employee has work assignments
      const dateStr = date.toISOString().split('T')[0];
      const assignment = workAssignments[dateStr];
      if (assignment && assignment.tasks && assignment.tasks.length > 0) {
        return true;
      }
      // Fallback to old logic for backward compatibility
      const daySchedules = getSchedulesForDate(date);
      const dayOrders = getOrdersForDate(date);
      return daySchedules.some(schedule => schedule.employee_id === user.id) ||
             dayOrders.some(order => 
               order.cooking_employee_id === user.id || 
               order.delivery_employee_id === user.id
             );
    } else {
      // For orders calendar, check if there are any orders
      const dayOrders = getOrdersForDate(date);
      return dayOrders.length > 0;
    }
  };

  const getOrderColor = (order: Order, date: Date | null): 'red' | 'green' => {
    if (!date) return 'green';
    
    // 끝난 주문은 초록색
    if (order.status === 'delivered' || order.status === 'cancelled') {
      return 'green';
    }
    
    // 해당 날짜에 근무하는 직원인지 확인
    const dateStr = date.toISOString().split('T')[0];
    const assignment = workAssignments[dateStr];
    
    if (!assignment || !assignment.tasks || assignment.tasks.length === 0) {
      // 할당된 작업이 없으면 초록색
      return 'green';
    }
    
    // 할당된 작업이 있는 경우, 작업 완료 여부 확인
    const hasCookingTask = assignment.tasks.includes('조리');
    const hasDeliveryTask = assignment.tasks.includes('배달');
    
    // 조리 작업이 할당된 경우: ready 상태 이상이면 초록색
    if (hasCookingTask) {
      if (order.status === 'ready' || order.status === 'out_for_delivery' || order.status === 'delivered') {
        return 'green';
      }
    }
    
    // 배달 작업이 할당된 경우: delivered 상태면 초록색
    if (hasDeliveryTask) {
      if (order.status === 'delivered') {
        return 'green';
      }
    }
    
    // 할당된 작업이 있지만 아직 완료되지 않았으면 빨간색
    return 'red';
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

  // Don't show navigation if used as component
  const showNavigation = !propType;

  return (
    <div className="schedule-calendar-page">
      {showNavigation && <TopLogo />}
      {showNavigation && (
        <div style={{ marginBottom: '20px', paddingLeft: '20px', display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button onClick={() => navigate('/employee/orders')} className="btn btn-secondary">
            ← 스케줄 탭으로
          </button>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              className={`btn ${calendarType === 'schedule' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => navigate('/schedule?type=schedule')}
            >
              📅 스케줄 캘린더
            </button>
            <button
              className={`btn ${calendarType === 'orders' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => navigate('/schedule?type=orders')}
            >
              📋 주문 캘린더
            </button>
          </div>
        </div>
      )}

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
                const isClickable = date && (calendarType === 'schedule' ? hasMySchedules : (dayOrders.length > 0 || daySchedules.length > 0));
                
                // For schedule calendar: red if employee has work, green if not
                // For orders calendar: show order count
                const getDayColor = () => {
                  if (!date) return '';
                  if (calendarType === 'schedule') {
                    // 해당 날에 할당된 작업이 있으면 빨간색
                    const dateStr = date.toISOString().split('T')[0];
                    const assignment = workAssignments[dateStr];
                    if (assignment && assignment.tasks && assignment.tasks.length > 0) {
                      return 'red';
                    }
                    return hasMySchedules ? 'red' : 'green';
                  } else {
                    return dayOrders.length > 0 ? 'blue' : '';
                  }
                };
                const dayColor = getDayColor();

                return (
                  <div
                    key={index}
                    className={`calendar-day ${!isCurrentMonth ? 'other-month' : ''} ${isToday ? 'today' : ''} ${isClickable ? 'clickable' : ''} ${hasMySchedules ? 'has-my-schedule' : ''}`}
                    onClick={() => isClickable && handleDateClick(date)}
                    style={{
                      backgroundColor: dayColor === 'red' ? '#ff4444' : dayColor === 'green' ? '#4CAF50' : dayColor === 'blue' ? '#2196F3' : '',
                      color: dayColor ? '#fff' : '',
                      fontWeight: dayColor ? 'bold' : 'normal'
                    }}
                  >
                    {date && (
                      <>
                        <div className="calendar-day-header">
                          <div className="calendar-day-number">{date.getDate()}</div>
                          {calendarType === 'orders' && dayOrders.length > 0 && (
                            <div className="order-count-indicator" title={`${dayOrders.length}개 주문`} style={{
                              fontSize: '10px',
                              background: '#2196F3',
                              color: '#fff',
                              borderRadius: '50%',
                              width: '18px',
                              height: '18px',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              marginTop: '2px'
                            }}>
                              {dayOrders.length}
                            </div>
                          )}
                          {calendarType === 'schedule' && hasMySchedules && (
                            <div className="my-schedule-indicator" title="내 작업 일정" style={{
                              width: '8px',
                              height: '8px',
                              background: '#FFD700',
                              borderRadius: '50%',
                              marginTop: '2px'
                            }}></div>
                          )}
                        </div>
                        <div className="calendar-day-schedules">
                          {calendarType === 'schedule' ? (
                            // Schedule calendar: show assignments
                            <>
                              {daySchedules.slice(0, 2).map(schedule => (
                                <div
                                  key={schedule.id}
                                  className="schedule-item"
                                  style={{ borderLeftColor: '#FFD700' }}
                                  title={`배달 일정 - ${schedule.delivery_address || '주소 없음'}`}
                                >
                                  <div className="schedule-time">{formatTime(schedule.departure_time)}</div>
                                  <div className="schedule-status" style={{ color: '#FFD700' }}>
                                    배달
                                  </div>
                                </div>
                              ))}
                              {dayOrders.filter(order => 
                                order.cooking_employee_id === user?.id || 
                                order.delivery_employee_id === user?.id
                              ).slice(0, 2 - daySchedules.length).map(order => {
                                const isCooking = order.cooking_employee_id === user?.id;
                                const isDelivery = order.delivery_employee_id === user?.id;
                                return (
                                  <div
                                    key={order.id}
                                    className="schedule-item"
                                    style={{ borderLeftColor: '#FFD700' }}
                                    title={`${isCooking ? '조리' : ''}${isCooking && isDelivery ? ' / ' : ''}${isDelivery ? '배달' : ''} - 주문 #${order.id}`}
                                  >
                                    <div className="schedule-time">{formatTime(order.delivery_time)}</div>
                                    <div className="schedule-status" style={{ color: '#FFD700' }}>
                                      {isCooking && isDelivery ? '조리/배달' : isCooking ? '조리' : '배달'}
                                    </div>
                                  </div>
                                );
                              })}
                            </>
                          ) : (
                            // Orders calendar: show orders
                            <>
                              {dayOrders.slice(0, 2).map(order => {
                                const orderColor = getOrderColor(order, date);
                                return (
                                  <div
                                    key={order.id}
                                    className={`schedule-item order-item ${orderColor === 'red' ? 'my-schedule' : 'other-schedule'}`}
                                    style={{ borderLeftColor: orderColor === 'red' ? '#ff4444' : '#4CAF50' }}
                                    title={`주문 #${order.id} - ${order.customer_name || '고객'} | ${order.dinner_name || '디너'}`}
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
                            </>
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
                      const orderColor = getOrderColor(order, selectedDate);
                      const dateStr = selectedDate ? selectedDate.toISOString().split('T')[0] : '';
                      const assignment = workAssignments[dateStr];
                      const tasks = assignment?.tasks || [];
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
                              </p>
                              {calendarType === 'schedule' && tasks.length > 0 && (
                                <p className="employee-name" style={{ fontSize: '14px', marginTop: '8px', color: '#FFD700', fontWeight: 'bold' }}>
                                  {tasks.includes('조리') && tasks.includes('배달') ? '🔧 조리 / 🚚 배달 담당' : 
                                   tasks.includes('조리') ? '🔧 조리 담당' : 
                                   tasks.includes('배달') ? '🚚 배달 담당' : ''}
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
                          {calendarType === 'orders' && (
                            <div style={{ marginTop: '15px', display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                              {(() => {
                                // 관리자는 주문 상태 변경 불가, 할당받은 직원만 가능
                                if (isAdmin) return null;
                                
                                const dateStr = selectedDate ? selectedDate.toISOString().split('T')[0] : '';
                                const assignment = workAssignments[dateStr];
                                const tasks = assignment?.tasks || [];
                                const canChangeStatus = tasks.includes('조리') || tasks.includes('배달');
                                
                                if (!canChangeStatus) return null;
                                
                                const getNextStatus = () => {
                                  if (order.status === 'pending') return 'cooking';
                                  if (order.status === 'cooking') return 'ready';
                                  if (order.status === 'ready') return 'out_for_delivery';
                                  if (order.status === 'out_for_delivery') return 'delivered';
                                  return null;
                                };
                                
                                const nextStatus = getNextStatus();
                                if (!nextStatus) return null;
                                
                                const statusLabels: { [key: string]: string } = {
                                  'cooking': '조리 시작',
                                  'ready': '조리 완료',
                                  'out_for_delivery': '배달 시작',
                                  'delivered': '배달 완료'
                                };
                                
                                return (
                                  <button
                                    className="btn btn-primary"
                                    onClick={async () => {
                                      try {
                                        const headers = getAuthHeaders();
                                        
                                        // 상태 변경 메시지 표시
                                        const statusMessages: { [key: string]: string } = {
                                          'cooking': '조리를 시작합니다',
                                          'ready': '조리를 완료했습니다',
                                          'out_for_delivery': '배달을 시작합니다',
                                          'delivered': '배달을 완료했습니다'
                                        };
                                        const message = statusMessages[nextStatus] || '주문 상태가 변경되었습니다';
                                        
                                        // 즉시 로컬 상태 업데이트하여 UI에 반영
                                        setOrders(prevOrders => 
                                          prevOrders.map(o => 
                                            o.id === order.id ? { ...o, status: nextStatus } : o
                                          )
                                        );
                                        
                                        // 팝업 알림 표시
                                        alert(message);
                                        
                                        await axios.patch(`${API_URL}/employee/orders/${order.id}/status`, 
                                          { status: nextStatus }, 
                                          { headers }
                                        );
                                        
                                        // 서버에서 최신 데이터 가져오기 (백그라운드)
                                        fetchOrders().catch(console.error);
                                        fetchWorkAssignments().catch(console.error);
                                      } catch (err: any) {
                                        // 실패 시 이전 상태로 복원
                                        setOrders(prevOrders => 
                                          prevOrders.map(o => 
                                            o.id === order.id ? { ...o, status: order.status } : o
                                          )
                                        );
                                        alert(err.response?.data?.error || '주문 상태 변경에 실패했습니다.');
                                      }
                                    }}
                                  >
                                    {statusLabels[nextStatus]}
                                  </button>
                                );
                              })()}
                            </div>
                          )}
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

