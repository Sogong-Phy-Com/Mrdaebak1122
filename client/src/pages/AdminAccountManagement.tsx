import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';
import './AdminDashboard.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface User {
  id: number;
  email: string;
  name: string;
  address: string;
  phone: string;
  role: string;
}

const AdminAccountManagement: React.FC = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [filter, setFilter] = useState<string>('all');
  const [loading, setLoading] = useState(true);
  const [userError, setUserError] = useState('');
  const [promotingUserId, setPromotingUserId] = useState<number | null>(null);
  const [pendingApprovals, setPendingApprovals] = useState<any[]>([]);
  const [pendingLoading, setPendingLoading] = useState(false);
  const [pendingError, setPendingError] = useState('');
  const [activeTab, setActiveTab] = useState<'accounts' | 'approvals'>('accounts');

  useEffect(() => {
    if (activeTab === 'accounts') {
      fetchUsers();
    } else {
      fetchPendingApprovals();
    }
  }, [activeTab]);

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('관리자 로그인이 필요합니다.');
    }
    return {
      Authorization: `Bearer ${token}`
    };
  };

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/admin/users`, { headers });
      setUsers(response.data);
      setUserError('');
    } catch (err: any) {
      setUserError(err.message || '회원 정보를 불러오는데 실패했습니다.');
      if (err.response?.status === 403) {
        setUserError('관리자 권한이 필요합니다.');
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

  const fetchPendingApprovals = async () => {
    try {
      setPendingLoading(true);
      setPendingError('');
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/admin/pending-approvals`, { headers });
      setPendingApprovals(response.data);
    } catch (err: any) {
      setPendingError(err.response?.data?.error || err.message || '승인 대기 목록을 불러오는데 실패했습니다.');
      setPendingApprovals([]);
    } finally {
      setPendingLoading(false);
    }
  };

  const handleApproveUser = async (userId: number) => {
    try {
      const headers = getAuthHeaders();
      await axios.post(`${API_URL}/admin/approve-user/${userId}`, {}, { headers });
      await fetchPendingApprovals();
    } catch (err: any) {
      setPendingError(err.response?.data?.error || err.message || '승인에 실패했습니다.');
    }
  };

  const handleRejectUser = async (userId: number) => {
    if (!window.confirm('이 사용자의 가입을 거부하시겠습니까?')) {
      return;
    }
    try {
      const headers = getAuthHeaders();
      await axios.post(`${API_URL}/admin/reject-user/${userId}`, {}, { headers });
      await fetchPendingApprovals();
    } catch (err: any) {
      setPendingError(err.response?.data?.error || err.message || '거부에 실패했습니다.');
    }
  };

  const handlePromoteToAdmin = async (userId: number) => {
    if (!window.confirm('이 직원을 관리자로 승급시키시겠습니까?')) {
      return;
    }
    
    try {
      setPromotingUserId(userId);
      const headers = getAuthHeaders();
      await axios.patch(`${API_URL}/admin/users/${userId}/promote`, {}, { headers });
      await fetchUsers(); // Refresh user list
      setUserError('');
    } catch (err: any) {
      setUserError(err.response?.data?.error || err.message || '관리자 승급에 실패했습니다.');
    } finally {
      setPromotingUserId(null);
    }
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
    return (
      <div className="admin-dashboard">
        <TopLogo />
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="admin-dashboard">
      <TopLogo />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>

        <div className="admin-section">
          <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
            <button
              className={`btn ${activeTab === 'accounts' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveTab('accounts')}
            >
              계정 관리
            </button>
            <button
              className={`btn ${activeTab === 'approvals' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveTab('approvals')}
            >
              계정 승인
            </button>
          </div>

          {activeTab === 'accounts' && (
            <>
              <h2>회원 관리</h2>
              {userError && <div className="error">{userError}</div>}

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
            {filteredUsers.length === 0 ? (
              <p style={{ textAlign: 'center', padding: '20px' }}>회원이 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>이름</th>
                    <th>이메일</th>
                    <th>전화번호</th>
                    <th>주소</th>
                    <th>역할</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map(user => (
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
                      <td>
                        {user.role === 'employee' && (
                          <button
                            className="btn btn-primary"
                            onClick={() => handlePromoteToAdmin(user.id)}
                            disabled={promotingUserId === user.id}
                            style={{ padding: '5px 10px', fontSize: '12px' }}
                          >
                            {promotingUserId === user.id ? '처리 중...' : '관리자 승급'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
            </>
          )}

          {activeTab === 'approvals' && (
            <>
              <h2>계정 승인</h2>
              {pendingError && <div className="error">{pendingError}</div>}
              {pendingLoading ? (
                <div className="loading">로딩 중...</div>
              ) : pendingApprovals.length === 0 ? (
                <p style={{ textAlign: 'center', padding: '20px' }}>승인 대기 중인 계정이 없습니다.</p>
              ) : (
                <div className="users-table">
                  <table>
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>이름</th>
                        <th>이메일</th>
                        <th>전화번호</th>
                        <th>역할</th>
                        <th>작업</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingApprovals.map(user => (
                        <tr key={user.id}>
                          <td>{user.id}</td>
                          <td>{user.name}</td>
                          <td>{user.email}</td>
                          <td>{user.phone}</td>
                          <td>
                            <span className={`role-badge ${getRoleClass(user.role)}`}>
                              {getRoleLabel(user.role)}
                            </span>
                          </td>
                          <td>
                            <div style={{ display: 'flex', gap: '5px' }}>
                              <button
                                className="btn btn-success"
                                onClick={() => handleApproveUser(user.id)}
                                style={{ padding: '5px 10px', fontSize: '12px' }}
                              >
                                승인
                              </button>
                              <button
                                className="btn btn-danger"
                                onClick={() => handleRejectUser(user.id)}
                                style={{ padding: '5px 10px', fontSize: '12px' }}
                              >
                                거부
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminAccountManagement;

