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
    role: 'customer'
  });
  const [error, setError] = useState('');
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
        formData.role
      );
      // Navigate based on role
      const loggedInUser = JSON.parse(localStorage.getItem('user') || '{}');
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
    <div className="register-page">
      <div className="register-container">
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
              <option value="admin">관리자</option>
            </select>
            <p style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
              고객: 디너 주문 및 주문 내역 조회<br />
              직원: 주문 관리 및 상태 업데이트<br />
              관리자: 회원 관리 및 모든 기능 접근
            </p>
          </div>
          {error && <div className="error">{error}</div>}
          <button type="submit" className="btn btn-primary">회원가입</button>
          <p className="login-link">
            이미 계정이 있으신가요? <Link to="/login">로그인</Link>
          </p>
        </form>
      </div>
    </div>
  );
};

export default Register;

