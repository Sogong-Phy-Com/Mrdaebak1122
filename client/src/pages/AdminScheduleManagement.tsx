import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';
import './AdminDashboard.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface Employee {
  id: number;
  name: string;
  email: string;
  employeeType?: string;
}

interface DayAssignment {
  date: string;
  cookingEmployees: number[];
  deliveryEmployees: number[];
}

const AdminScheduleManagement: React.FC = () => {
  const navigate = useNavigate();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [currentMonth, setCurrentMonth] = useState(new Date().getMonth());
  const [currentYear, setCurrentYear] = useState(new Date().getFullYear());
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [dayAssignments, setDayAssignments] = useState<{ [key: string]: DayAssignment }>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchEmployees();
    fetchDayAssignments();
  }, [currentMonth, currentYear]);

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('Admin login required');
    }
    return {
      Authorization: `Bearer ${token}`
    };
  };

  const fetchEmployees = async () => {
    try {
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/admin/employees`, { headers });
      setEmployees(response.data || []);
    } catch (err: any) {
      console.error('Failed to fetch employees:', err);
      // Fallback to users endpoint
      try {
        const headers = getAuthHeaders();
        const response = await axios.get(`${API_URL}/admin/users`, { headers });
        const employeeList = response.data.filter((u: any) => (u.role === 'employee' || u.role === 'admin') && u.approvalStatus === 'approved');
        setEmployees(employeeList);
      } catch (err2: any) {
        setError('직원 목록을 불러오는데 실패했습니다.');
      }
    }
  };

  const fetchDayAssignments = async () => {
    // This would fetch assignments for the current month
    // For now, we'll use local state
  };

  const getDaysInMonth = (year: number, month: number): number => {
    return new Date(year, month + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (year: number, month: number): number => {
    return new Date(year, month, 1).getDay();
  };

  const isDateInPast = (year: number, month: number, day: number): boolean => {
    const date = new Date(year, month, day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    date.setHours(0, 0, 0, 0);
    return date < today;
  };

  const getDateKey = (year: number, month: number, day: number): string => {
    return `${year}-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
  };

  const getAssignmentStatus = (dateKey: string): 'full' | 'partial' | 'empty' => {
    const assignment = dayAssignments[dateKey];
    if (!assignment) return 'empty';
    const totalAssigned = (assignment.cookingEmployees?.length || 0) + (assignment.deliveryEmployees?.length || 0);
    if (totalAssigned >= 10) return 'full';
    if (totalAssigned > 0) return 'partial';
    return 'empty';
  };

  const handleDateClick = (dateKey: string) => {
    setSelectedDate(dateKey);
  };

  const handleSaveAssignment = async () => {
    if (!selectedDate) return;
    
    const assignment = dayAssignments[selectedDate] || {
      date: selectedDate,
      cookingEmployees: [],
      deliveryEmployees: []
    };
    
    // Check minimum 5 employees for each type
    if (assignment.cookingEmployees.length < 5) {
      alert('작업 할당이 완료되지 않았습니다. 조리 담당 직원은 최소 5명이 필요합니다.');
      return;
    }
    
    if (assignment.deliveryEmployees.length < 5) {
      alert('작업 할당이 완료되지 않았습니다. 배달 담당 직원은 최소 5명이 필요합니다.');
      return;
    }
    
    // Check if any employee is assigned to both tasks
    const duplicateEmployees = assignment.cookingEmployees.filter(id => 
      assignment.deliveryEmployees.includes(id)
    );
    if (duplicateEmployees.length > 0) {
      alert('한 명의 직원이 하루에 두 가지 일을 할 수 없습니다.');
      return;
    }
    
    try {
      setLoading(true);
      setError('');
      const headers = getAuthHeaders();
      
      // Save assignment to backend
      await axios.post(`${API_URL}/admin/schedule/assign`, {
        date: selectedDate,
        cookingEmployees: assignment.cookingEmployees,
        deliveryEmployees: assignment.deliveryEmployees
      }, { headers });
      
      setDayAssignments({
        ...dayAssignments,
        [selectedDate]: assignment
      });
      alert('직원 할당이 저장되었습니다.');
      setSelectedDate(null);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || '할당 저장에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const updateDayAssignment = (dateKey: string, type: 'cooking' | 'delivery', employeeId: number, add: boolean) => {
    const assignment = dayAssignments[dateKey] || {
      date: dateKey,
      cookingEmployees: [],
      deliveryEmployees: []
    };

    const targetArray = type === 'cooking' ? assignment.cookingEmployees : assignment.deliveryEmployees;
    
    if (add) {
      if (!targetArray.includes(employeeId) && targetArray.length < 5) {
        targetArray.push(employeeId);
      }
    } else {
      const index = targetArray.indexOf(employeeId);
      if (index > -1) {
        targetArray.splice(index, 1);
      }
    }

    setDayAssignments({
      ...dayAssignments,
      [dateKey]: assignment
    });
  };

  const daysInMonth = getDaysInMonth(currentYear, currentMonth);
  const firstDay = getFirstDayOfMonth(currentYear, currentMonth);
  const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];
  const dayNames = ['일', '월', '화', '수', '목', '금', '토'];

  const selectedAssignment = selectedDate ? dayAssignments[selectedDate] : null;

  return (
    <div className="admin-dashboard">
      <TopLogo />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>

        <h2>스케줄 관리</h2>
        {error && <div className="error">{error}</div>}

        {/* Tab Menu for Calendar Views */}
        <div style={{ 
          display: 'flex', 
          gap: '10px', 
          marginBottom: '20px',
          borderBottom: '2px solid #FFD700',
          paddingBottom: '10px'
        }}>
          <button
            className="btn btn-secondary"
            onClick={() => navigate('/schedule?type=schedule')}
            style={{
              borderBottom: '3px solid #FFD700',
              borderRadius: '4px 4px 0 0'
            }}
          >
            📅 스케줄 캘린더
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => navigate('/schedule?type=orders')}
            style={{
              borderBottom: '3px solid #FFD700',
              borderRadius: '4px 4px 0 0'
            }}
          >
            📋 주문 캘린더
          </button>
        </div>

        <div style={{ marginBottom: '20px', display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            onClick={() => {
              if (currentMonth === 0) {
                setCurrentMonth(11);
                setCurrentYear(currentYear - 1);
              } else {
                setCurrentMonth(currentMonth - 1);
              }
            }}
            className="btn btn-secondary"
          >
            이전 달
          </button>
          <h3 style={{ margin: 0, minWidth: '150px', textAlign: 'center' }}>
            {currentYear}년 {monthNames[currentMonth]}
          </h3>
          <button
            onClick={() => {
              if (currentMonth === 11) {
                setCurrentMonth(0);
                setCurrentYear(currentYear + 1);
              } else {
                setCurrentMonth(currentMonth + 1);
              }
            }}
            className="btn btn-secondary"
          >
            다음 달
          </button>
        </div>

        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(7, 1fr)', 
          gap: '5px',
          marginBottom: '30px'
        }}>
          {dayNames.map(day => (
            <div key={day} style={{ 
              padding: '10px', 
              textAlign: 'center', 
              fontWeight: 'bold',
              background: '#d4af37',
              color: '#000'
            }}>
              {day}
            </div>
          ))}
          {Array.from({ length: firstDay }).map((_, i) => (
            <div key={`empty-${i}`} style={{ padding: '20px' }} />
          ))}
          {Array.from({ length: daysInMonth }).map((_, i) => {
            const day = i + 1;
            const dateKey = getDateKey(currentYear, currentMonth, day);
            const isPast = isDateInPast(currentYear, currentMonth, day);
            const status = getAssignmentStatus(dateKey);
            const isToday = dateKey === new Date().toISOString().split('T')[0];

            return (
              <div
                key={day}
                onClick={() => !isPast && handleDateClick(dateKey)}
                style={{
                  padding: '15px',
                  textAlign: 'center',
                  cursor: isPast ? 'not-allowed' : 'pointer',
                  background: isPast ? '#ccc' : status === 'full' ? '#4CAF50' : status === 'partial' ? '#ff4444' : '#f5f5f5',
                  color: isPast ? '#666' : status === 'empty' ? '#000' : '#fff',
                  border: isToday ? '2px solid #FFD700' : '1px solid #ddd',
                  borderRadius: '4px',
                  opacity: isPast ? 0.5 : 1
                }}
              >
                <div style={{ fontWeight: 'bold' }}>{day}</div>
                {!isPast && (
                  <div style={{ fontSize: '10px', marginTop: '5px' }}>
                    {status === 'full' ? '10명 할당' : status === 'partial' ? '부분 할당' : '미할당'}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {selectedDate && (
          <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.8)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000
          }}>
            <div style={{
              background: '#1a1a1a',
              color: '#fff',
              padding: '30px',
              borderRadius: '12px',
              maxWidth: '600px',
              width: '90%',
              maxHeight: '80vh',
              overflow: 'auto',
              border: '2px solid #d4af37'
            }}>
              <h3>{selectedDate} 직원 할당</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginTop: '20px' }}>
                <div>
                  <h4>조리 담당 (5명 선택)</h4>
                  <div style={{ 
                    border: '1px solid #d4af37', 
                    padding: '10px', 
                    borderRadius: '4px',
                    minHeight: '200px',
                    maxHeight: '300px',
                    overflow: 'auto',
                    background: '#2a2a2a'
                  }}>
                    {employees.map(emp => {
                      const isAssigned = selectedAssignment?.cookingEmployees?.includes(emp.id) || false;
                      const isAssignedToDelivery = selectedAssignment?.deliveryEmployees?.includes(emp.id) || false;
                      const isDisabled = isAssignedToDelivery || (!isAssigned && (selectedAssignment?.cookingEmployees?.length || 0) >= 5);
                      return (
                        <div key={emp.id} style={{ 
                          display: 'flex', 
                          justifyContent: 'space-between', 
                          alignItems: 'center',
                          padding: '8px',
                          marginBottom: '5px',
                          background: isAssigned ? '#4CAF50' : isAssignedToDelivery ? '#666' : '#3a3a3a',
                          borderRadius: '4px',
                          opacity: isDisabled && !isAssigned ? 0.5 : 1
                        }}>
                          <span>{emp.name}</span>
                          <button
                            onClick={() => updateDayAssignment(selectedDate, 'cooking', emp.id, !isAssigned)}
                            className={`btn ${isAssigned ? 'btn-danger' : 'btn-success'}`}
                            style={{ padding: '5px 10px', fontSize: '12px' }}
                            disabled={isDisabled}
                          >
                            {isAssigned ? '제거' : '추가'}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
                <div>
                  <h4>배달 담당 (5명 선택)</h4>
                  <div style={{ 
                    border: '1px solid #d4af37', 
                    padding: '10px', 
                    borderRadius: '4px',
                    minHeight: '200px',
                    maxHeight: '300px',
                    overflow: 'auto',
                    background: '#2a2a2a'
                  }}>
                    {employees.map(emp => {
                      const isAssigned = selectedAssignment?.deliveryEmployees?.includes(emp.id) || false;
                      const isAssignedToCooking = selectedAssignment?.cookingEmployees?.includes(emp.id) || false;
                      const isDisabled = isAssignedToCooking || (!isAssigned && (selectedAssignment?.deliveryEmployees?.length || 0) >= 5);
                      return (
                        <div key={emp.id} style={{ 
                          display: 'flex', 
                          justifyContent: 'space-between', 
                          alignItems: 'center',
                          padding: '8px',
                          marginBottom: '5px',
                          background: isAssigned ? '#4CAF50' : isAssignedToCooking ? '#666' : '#3a3a3a',
                          borderRadius: '4px',
                          opacity: isDisabled && !isAssigned ? 0.5 : 1
                        }}>
                          <span>{emp.name}</span>
                          <button
                            onClick={() => updateDayAssignment(selectedDate, 'delivery', emp.id, !isAssigned)}
                            className={`btn ${isAssigned ? 'btn-danger' : 'btn-success'}`}
                            style={{ padding: '5px 10px', fontSize: '12px' }}
                            disabled={isDisabled}
                          >
                            {isAssigned ? '제거' : '추가'}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <button
                  onClick={handleSaveAssignment}
                  className="btn btn-primary"
                  disabled={loading}
                >
                  저장
                </button>
                <button
                  onClick={() => setSelectedDate(null)}
                  className="btn btn-secondary"
                >
                  취소
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminScheduleManagement;
