import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './AdminDashboard.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5000/api';

interface User {
  id: number;
  email: string;
  name: string;
  address: string;
  phone: string;
  role: string;
}

const AdminDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [filter, setFilter] = useState<string>('all'); // all, customer, employee, admin
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await axios.get(`${API_URL}/admin/users`);
      setUsers(response.data);
    } catch (err: any) {
      setError('회원 정보를 불러오는데 실패했습니다.');
      if (err.response?.status === 403) {
        setError('관리자 권한이 필요합니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const getRoleLabel = (role: string) => {
    const labels: { [key: string]: string } = {
      customer: '고객',
      employee: '직원',
      admin: '관리자'
    };
    return labels[role] || role;
  };

  const getRoleClass = (role: string) => {
    const classes: { [key: string]: string } = {
      customer: 'role-customer',
      employee: 'role-employee',
      admin: 'role-admin'
    };
    return classes[role] || '';
  };

  const filteredUsers = filter === 'all' 
    ? users 
    : users.filter(user => user.role === filter);

  const stats = {
    total: users.length,
    customers: users.filter(u => u.role === 'customer').length,
    employees: users.filter(u => u.role === 'employee').length,
    admins: users.filter(u => u.role === 'admin').length
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="admin-dashboard">
      <nav className="navbar">
        <div className="nav-container">
          <h1 className="logo">관리자 대시보드</h1>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            홈으로
          </button>
        </div>
      </nav>

      <div className="container">
        <h2>회원 관리</h2>
        {error && <div className="error">{error}</div>}

        <div className="stats-section">
          <div className="stat-card">
            <h3>전체 회원</h3>
            <p className="stat-number">{stats.total}</p>
          </div>
          <div className="stat-card">
            <h3>고객</h3>
            <p className="stat-number">{stats.customers}</p>
          </div>
          <div className="stat-card">
            <h3>직원</h3>
            <p className="stat-number">{stats.employees}</p>
          </div>
          <div className="stat-card">
            <h3>관리자</h3>
            <p className="stat-number">{stats.admins}</p>
          </div>
        </div>

        <div className="filter-section">
          <label>필터:</label>
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="filter-select"
          >
            <option value="all">전체</option>
            <option value="customer">고객</option>
            <option value="employee">직원</option>
            <option value="admin">관리자</option>
          </select>
        </div>

        <div className="users-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>이름</th>
                <th>이메일</th>
                <th>전화번호</th>
                <th>주소</th>
                <th>역할</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '20px' }}>
                    회원이 없습니다.
                  </td>
                </tr>
              ) : (
                filteredUsers.map(user => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.name}</td>
                    <td>{user.email}</td>
                    <td>{user.phone}</td>
                    <td>{user.address}</td>
                    <td>
                      <span className={`role-badge ${getRoleClass(user.role)}`}>
                        {getRoleLabel(user.role)}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;

