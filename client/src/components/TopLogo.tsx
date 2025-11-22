import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './TopLogo.css';

const TopLogo: React.FC = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();

  return (
    <div className="top-logo-container">
      <h1 className="top-logo" onClick={() => navigate('/')}>
        미스터 대박 서비스
      </h1>
      <button 
        onClick={logout} 
        className="btn btn-secondary"
        style={{ 
          marginLeft: 'auto',
          padding: '8px 16px',
          fontSize: '14px'
        }}
      >
        로그아웃
      </button>
    </div>
  );
};

export default TopLogo;

