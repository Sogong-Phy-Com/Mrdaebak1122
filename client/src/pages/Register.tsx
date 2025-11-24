import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Register.css';

const Register: React.FC = () => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    name: '',
    address: '',
    phone: '',
    role: 'customer',
    securityQuestion: '',
    securityAnswer: ''
  });
  const [error, setError] = useState('');
  const [showWelcomeModal, setShowWelcomeModal] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      await register(
        formData.email,
        formData.password,
        formData.name,
        formData.address,
        formData.phone,
        formData.role,
        formData.securityQuestion,
        formData.securityAnswer
      );
      // Show welcome modal first
      setShowWelcomeModal(true);
    } catch (err: any) {
      setError(err.message);
      // 승인 대기 메시지 표시
      if (formData.role !== 'customer' && err.message && err.message.includes('승인')) {
        // 승인 대기 메시지는 이미 error에 포함됨
      }
    }
  };

  return (
    <div className="register-page">
      <div className="register-container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/login')} className="btn btn-secondary">
            ← 로그인으로
          </button>
        </div>
        <h1>회원가입</h1>
        <form onSubmit={handleSubmit} className="register-form">
          <div className="form-group">
            <label>이메일</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>비밀번호 (최소 6자)</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              minLength={6}
            />
          </div>
          <div className="form-group">
            <label>이름</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>주소</label>
            <input
              type="text"
              name="address"
              value={formData.address}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>전화번호</label>
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>회원 유형</label>
            <select
              name="role"
              value={formData.role}
              onChange={handleChange}
              className="form-group select"
              required
            >
              <option value="customer">고객</option>
              <option value="employee">직원</option>
            </select>
            {formData.role === 'employee' && (
              <p style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                직원 계정은 관리자 승인이 필요합니다. 승인 후 관리자로 승급 가능합니다.
              </p>
            )}
          </div>
          <div className="form-group">
            <label>비밀번호 찾기 질문</label>
            <select
              name="securityQuestion"
              value={formData.securityQuestion}
              onChange={handleChange}
              className="form-group select"
              required
            >
              <option value="">질문을 선택하세요</option>
              <option value="내가 처음으로 산 차는?">내가 처음으로 산 차는?</option>
              <option value="어릴적 별명은?">어릴적 별명은?</option>
              <option value="내 어릴적 별명은?">내 어릴적 별명은?</option>
              <option value="가장 좋아하는 음식은?">가장 좋아하는 음식은?</option>
              <option value="출신 초등학교는?">출신 초등학교는?</option>
            </select>
          </div>
          <div className="form-group">
            <label>비밀번호 찾기 답변</label>
            <input
              type="text"
              name="securityAnswer"
              value={formData.securityAnswer}
              onChange={handleChange}
              required
              placeholder="답변을 입력하세요"
            />
          </div>
          {error && <div className="error">{error}</div>}
          <button type="submit" className="btn btn-primary">회원가입</button>
          <p className="login-link">
            이미 계정이 있으신가요? <Link to="/login">로그인</Link>
          </p>
        </form>
      </div>

      {/* Welcome Modal */}
      {showWelcomeModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.8)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 9999
        }}>
          <div style={{
            background: '#1a1a1a',
            padding: '40px',
            borderRadius: '12px',
            maxWidth: '500px',
            width: '90%',
            textAlign: 'center',
            color: '#fff',
            border: '2px solid #FFD700'
          }}>
            <h2 style={{ color: '#FFD700', marginBottom: '20px' }}>🎉 환영합니다!</h2>
            <p style={{ fontSize: '18px', marginBottom: '30px' }}>
              회원가입이 완료되었습니다.
              <br />
              {formData.role === 'employee' ? '관리자 승인 후 로그인할 수 있습니다.' : '이제 로그인하여 서비스를 이용하실 수 있습니다.'}
            </p>
            <button
              onClick={() => {
                setShowWelcomeModal(false);
                const loggedInUser = JSON.parse(localStorage.getItem('user') || '{}');
                if (loggedInUser.role === 'admin') {
                  navigate('/admin');
                } else if (loggedInUser.role === 'employee') {
                  navigate('/employee');
                } else {
                  navigate('/');
                }
              }}
              className="btn btn-primary"
              style={{ padding: '10px 30px', fontSize: '16px' }}
            >
              확인
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Register;

