import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './BottomNav.css';

const BottomNav: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  // 승인 대기 상태면 네비게이션 숨김
  if (user?.approvalStatus === 'pending') {
    return null;
  }

  // 고객용 네비게이션
  if (!user || user.role === 'customer') {
    const navItems = [
      { path: '/', icon: '🏠', label: '홈' },
      { path: '/order', icon: '🛒', label: '주문' },
      { path: '/orders', icon: '📋', label: '주문내역' },
      { path: '/profile', icon: '👤', label: '내정보' }
    ];

    return (
      <nav className="bottom-nav">
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <div
              key={item.path}
              className={`bottom-nav-item ${isActive ? 'active' : ''}`}
              onClick={() => navigate(item.path)}
            >
              <span className="bottom-nav-item-icon">{item.icon}</span>
              <span className="bottom-nav-item-label">{item.label}</span>
            </div>
          );
        })}
      </nav>
    );
  }

  // 직원/관리자용 네비게이션
  const staffNavItems = [
    { path: '/', icon: '🏠', label: '홈' },
    { path: '/employee', icon: '📋', label: '주문관리' },
    ...(user?.role === 'admin' ? [{ path: '/admin', icon: '⚙️', label: '관리' }] : []),
    { path: '/profile', icon: '👤', label: '내정보' }
  ];

  return (
    <nav className="bottom-nav">
      {staffNavItems.map((item) => {
        const isActive = location.pathname === item.path;
        return (
          <div
            key={item.path}
            className={`bottom-nav-item ${isActive ? 'active' : ''}`}
            onClick={() => navigate(item.path)}
          >
            <span className="bottom-nav-item-icon">{item.icon}</span>
            <span className="bottom-nav-item-label">{item.label}</span>
          </div>
        );
      })}
    </nav>
  );
};

export default BottomNav;



