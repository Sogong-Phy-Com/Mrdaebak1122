import React from 'react';
import { useNavigate } from 'react-router-dom';
import './TopLogo.css';

const TopLogo: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="top-logo-container">
      <h1 className="top-logo" onClick={() => navigate('/')}>
        미스터 대박 서비스
      </h1>
    </div>
  );
};

export default TopLogo;

