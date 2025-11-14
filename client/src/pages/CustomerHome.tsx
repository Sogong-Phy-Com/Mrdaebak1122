import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import BottomNav from '../components/BottomNav';
import './Home.css';

const CustomerHome: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="home-page">
      <nav className="navbar">
        <div className="nav-container">
          <h1 className="logo" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>미스터 대박</h1>
          <div className="nav-links">
            <span className="user-name">{user?.name}님</span>
          </div>
        </div>
      </nav>

      <div className="hero">
        <div className="hero-content">
          <h2>특별한 날의 특별한 디너</h2>
          <h3>집에서 편안히 보내면서 소중한 사람을 감동시키세요</h3>
          <button onClick={() => navigate('/order')} className="btn btn-large" style={{ background: 'var(--gold)', color: 'var(--black)', fontWeight: '700' }}>
            🛒 지금 주문하기
          </button>
        </div>
      </div>

      <div className="container">
        {/* 디너 메뉴 */}
        <div className="features">
          <div className="feature-card" onClick={() => navigate('/order')}>
            <h3>💝 발렌타인 디너</h3>
            <p>와인과 스테이크가 하트 모양 접시와 큐피드 장식과 함께 제공됩니다</p>
            <div className="feature-badge">로맨틱한 특별한 날</div>
          </div>
          <div className="feature-card" onClick={() => navigate('/order')}>
            <h3>🇫🇷 프렌치 디너</h3>
            <p>커피, 와인, 샐러드, 스테이크가 제공됩니다</p>
            <div className="feature-badge">프랑스식 정찬</div>
          </div>
          <div className="feature-card" onClick={() => navigate('/order')}>
            <h3>🇬🇧 잉글리시 디너</h3>
            <p>에그 스크램블, 베이컨, 빵, 스테이크가 제공됩니다</p>
            <div className="feature-badge">영국식 브런치</div>
          </div>
          <div className="feature-card" onClick={() => navigate('/order')}>
            <h3>🍾 샴페인 축제 디너</h3>
            <p>2인 식사, 샴페인 1병, 바게트빵 4개, 커피 포트, 와인, 스테이크</p>
            <div className="feature-badge">프리미엄 패키지</div>
          </div>
        </div>
      </div>

      <BottomNav />
    </div>
  );
};

export default CustomerHome;
