import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './StaffHome.css';

const StaffHome: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="staff-home">
      <nav className="navbar">
        <div className="nav-container">
          <h1 className="logo">미스터 대박 - 직원 시스템</h1>
          <div className="nav-links">
            <span className="user-name">안녕하세요, {user?.name}님 ({user?.role === 'admin' ? '관리자' : '직원'})</span>
            {user?.role === 'admin' && (
              <button onClick={() => navigate('/admin')} className="btn btn-primary">
                관리자 대시보드
              </button>
            )}
            <button onClick={() => navigate('/employee')} className="btn btn-primary">
              주문 관리
            </button>
            <button onClick={logout} className="btn btn-secondary">
              로그아웃
            </button>
          </div>
        </div>
      </nav>

      <div className="container">
        <div className="staff-welcome">
          <h2>직원 대시보드</h2>
          <p>주문 관리 및 시스템 관리를 수행할 수 있습니다.</p>
        </div>

        <div className="staff-actions">
          {user?.role === 'admin' && (
            <div className="action-card" onClick={() => navigate('/admin')}>
              <h3>회원 관리</h3>
              <p>모든 회원 정보를 조회하고 관리합니다</p>
              <ul>
                <li>고객 목록 조회</li>
                <li>직원 목록 조회</li>
                <li>회원 통계</li>
              </ul>
            </div>
          )}
          
          <div className="action-card" onClick={() => navigate('/employee')}>
            <h3>주문 관리</h3>
            <p>고객 주문을 확인하고 상태를 업데이트합니다</p>
            <ul>
              <li>전체 주문 조회</li>
              <li>주문 상태 필터링</li>
              <li>주문 상태 업데이트</li>
              <li>고객 정보 확인</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StaffHome;




