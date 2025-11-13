import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Home.css';

const Home: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="home-page">
      <nav className="navbar">
        <div className="nav-container">
          <h1 className="logo">미스터 대박</h1>
          <div className="nav-links">
            <span className="user-name">안녕하세요, {user?.name}님</span>
            <button onClick={() => navigate('/order')} className="btn btn-primary">
              주문하기
            </button>
            <button onClick={() => navigate('/orders')} className="btn btn-secondary">
              주문 내역
            </button>
            {user?.role === 'admin' && (
              <button onClick={() => navigate('/admin')} className="btn btn-secondary">
                관리자 대시보드
              </button>
            )}
            {(user?.role === 'employee' || user?.role === 'admin') && (
              <button onClick={() => navigate('/employee')} className="btn btn-secondary">
                직원 대시보드
              </button>
            )}
            <button onClick={logout} className="btn btn-secondary">
              로그아웃
            </button>
          </div>
        </div>
      </nav>

      <div className="hero">
        <div className="hero-content">
          <h2>특별한 날에 집에서 편안히 보내면서</h2>
          <h3>당신의 남편, 아내, 엄마, 아버지, 또는 친구를 감동시켜라</h3>
          <button onClick={() => navigate('/order')} className="btn btn-primary btn-large">
            지금 주문하기
          </button>
        </div>
      </div>

      <div className="container">
        <div className="features">
          <div className="feature-card">
            <h3>발렌타인 디너</h3>
            <p>와인과 스테이크가 하트 모양 접시와 큐피드 장식과 함께 제공됩니다</p>
          </div>
          <div className="feature-card">
            <h3>프렌치 디너</h3>
            <p>커피, 와인, 샐러드, 스테이크가 제공됩니다</p>
          </div>
          <div className="feature-card">
            <h3>잉글리시 디너</h3>
            <p>에그 스크램블, 베이컨, 빵, 스테이크가 제공됩니다</p>
          </div>
          <div className="feature-card">
            <h3>샴페인 축제 디너</h3>
            <p>2인 식사, 샴페인 1병, 바게트빵 4개, 커피 포트, 와인, 스테이크</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;

