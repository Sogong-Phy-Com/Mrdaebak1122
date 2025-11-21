import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Login.css';

const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      await login(email, password);
      // Check user role and approval status after login
      const loggedInUser = JSON.parse(localStorage.getItem('user') || '{}');
      
      // 승인 대기 상태면 홈으로 이동 (승인 대기 화면 표시)
      if (loggedInUser.approvalStatus === 'pending') {
        navigate('/');
        return;
      }
      
      if (loggedInUser.role === 'admin') {
        navigate('/admin');
      } else if (loggedInUser.role === 'employee') {
        navigate('/employee');
      } else {
        navigate('/');
      }
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <h1>미스터 대박</h1>
        <h2>특별한 날의 특별한 디너</h2>
        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label>이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <div className="error">{error}</div>}
          <button type="submit" className="btn btn-primary">로그인</button>
          <p className="register-link">
            계정이 없으신가요? <Link to="/register">회원가입</Link>
          </p>
          <div style={{ marginTop: '20px', padding: '15px', background: '#f0f0f0', borderRadius: '8px', fontSize: '14px' }}>
            <strong>관리자/직원 로그인 테스트 계정:</strong><br />
            관리자: admin@mrdabak.com / admin123<br />
            직원1: employee1@mrdabak.com / emp123<br />
            직원2: employee2@mrdabak.com / emp123
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;

