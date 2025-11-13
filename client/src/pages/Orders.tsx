import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import './Orders.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5000/api';

interface OrderItem {
  id: number;
  menu_item_id: number;
  name: string;
  name_en: string;
  price: number;
  quantity: number;
}

interface Order {
  id: number;
  dinner_name: string;
  dinner_name_en: string;
  serving_style: string;
  delivery_time: string;
  delivery_address: string;
  total_price: number;
  status: string;
  payment_status: string;
  created_at: string;
  items: OrderItem[];
}

const Orders: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    // Redirect staff to their home
    if (user && (user.role === 'admin' || user.role === 'employee')) {
      navigate('/');
      return;
    }
    fetchOrders();
  }, [user, navigate]);

  const fetchOrders = async () => {
    console.log('[주문 목록 조회] 시작');
    
    try {
      // 1단계: 토큰 확인
      const token = localStorage.getItem('token');
      console.log('[1단계] 토큰 확인:', token ? '토큰 존재 (길이: ' + token.length + ')' : '토큰 없음');
      
      if (!token) {
        console.error('[에러] 토큰이 없습니다.');
        setError('[에러 1] 로그인이 필요합니다. (토큰 없음)');
        setLoading(false);
        navigate('/login');
        return;
      }

      // 2단계: 사용자 정보 확인
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      console.log('[2단계] 사용자 정보:', user ? `ID: ${user.id}, 이메일: ${user.email}, 역할: ${user.role}` : '사용자 정보 없음');

      // 3단계: API 요청 준비
      const apiUrl = `${API_URL}/orders`;
      console.log('[3단계] API 요청 URL:', apiUrl);
      console.log('[3단계] 요청 헤더:', { 'Authorization': `Bearer ${token.substring(0, 20)}...` });

      // 4단계: API 요청 실행
      console.log('[4단계] API 요청 시작...');
      const response = await axios.get(apiUrl, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      console.log('[5단계] API 응답 성공');
      console.log('[5단계] 응답 상태 코드:', response.status);
      console.log('[5단계] 응답 데이터 타입:', typeof response.data);
      console.log('[5단계] 응답 데이터:', response.data);
      console.log('[5단계] 주문 개수:', Array.isArray(response.data) ? response.data.length : '배열이 아님');

      if (!Array.isArray(response.data)) {
        console.error('[에러] 응답 데이터가 배열이 아닙니다:', response.data);
        setError('[에러 2] 서버 응답 형식이 올바르지 않습니다. (배열이 아님)');
        setLoading(false);
        return;
      }

      setOrders(response.data);
      console.log('[성공] 주문 목록 로드 완료:', response.data.length, '개');
    } catch (err: any) {
      console.error('========== [주문 목록 조회 실패] ==========');
      console.error('[에러] 에러 객체:', err);
      console.error('[에러] 에러 타입:', err?.constructor?.name || typeof err);
      console.error('[에러] 에러 메시지:', err?.message || '알 수 없는 오류');
      console.error('[에러] 전체 에러:', JSON.stringify(err, null, 2));
      
      let errorMessage = '주문 목록을 불러오는데 실패했습니다.';
      
      if (err?.response) {
        // HTTP 응답이 있는 경우
        const status = err.response.status;
        const errorData = err.response.data;
        
        console.error('[에러] HTTP 상태 코드:', status);
        console.error('[에러] HTTP 상태 텍스트:', err.response.statusText);
        console.error('[에러] 응답 데이터:', errorData);
        console.error('[에러] 응답 헤더:', err.response.headers);

        if (status === 401) {
          errorMessage = `[인증 실패] 로그인이 필요하거나 토큰이 만료되었습니다. (상태: 401)\n상세: ${JSON.stringify(errorData)}`;
          console.error('[에러 3] 상세: 인증 토큰이 유효하지 않거나 만료되었습니다.');
          navigate('/login');
        } else if (status === 403) {
          errorMessage = `[권한 없음] 이 기능에 접근할 권한이 없습니다. (상태: 403)\n상세: ${JSON.stringify(errorData)}`;
          console.error('[에러 4] 상세: 접근 권한이 없습니다.');
        } else if (status === 404) {
          errorMessage = `[엔드포인트 없음] API를 찾을 수 없습니다. (상태: 404)\n상세: ${JSON.stringify(errorData)}`;
          console.error('[에러 5] 상세: API 엔드포인트를 찾을 수 없습니다.');
        } else if (status === 500) {
          errorMessage = `[서버 오류] 서버에서 오류가 발생했습니다. (상태: 500)\n상세: ${JSON.stringify(errorData)}`;
          console.error('[에러 6] 상세: 서버 내부 오류입니다.');
        } else {
          errorMessage = `[HTTP 오류] 상태 코드: ${status}\n상세: ${JSON.stringify(errorData || err.message)}`;
          console.error(`[에러 7] 상세: HTTP ${status} 오류가 발생했습니다.`);
        }
      } else if (err?.request) {
        // 요청은 보냈지만 응답을 받지 못한 경우
        console.error('[에러] 요청은 보냈지만 응답을 받지 못함');
        console.error('[에러] 요청 정보:', err.request);
        errorMessage = '[네트워크 오류] 서버에 연결할 수 없습니다.\n서버가 실행 중인지 확인하세요. (http://localhost:5000)';
        console.error('[에러 8] 상세: 서버가 실행 중인지 확인하세요.');
      } else {
        // 요청 설정 중 오류가 발생한 경우
        console.error('[에러] 요청 설정 중 오류');
        errorMessage = `[요청 설정 오류] ${err?.message || '알 수 없는 오류'}\n전체 에러: ${JSON.stringify(err)}`;
        console.error('[에러 9] 상세: 요청을 구성하는 중 오류가 발생했습니다.');
      }
      
      console.error('==========================================');
      setError(errorMessage);
    } finally {
      setLoading(false);
      console.log('[주문 목록 조회] 완료');
    }
  };

  const getStatusLabel = (status: string) => {
    const labels: { [key: string]: string } = {
      pending: '대기 중',
      cooking: '조리 중',
      ready: '준비 완료',
      out_for_delivery: '배달 중',
      delivered: '배달 완료',
      cancelled: '취소됨'
    };
    return labels[status] || status;
  };

  const getStatusClass = (status: string) => {
    const classes: { [key: string]: string } = {
      pending: 'status-pending',
      cooking: 'status-cooking',
      ready: 'status-ready',
      out_for_delivery: 'status-delivery',
      delivered: 'status-delivered',
      cancelled: 'status-cancelled'
    };
    return classes[status] || '';
  };

  const getStyleLabel = (style: string) => {
    const labels: { [key: string]: string } = {
      simple: '심플',
      grand: '그랜드',
      deluxe: '디럭스'
    };
    return labels[style] || style;
  };

  if (loading) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="orders-page">
      <nav className="navbar">
        <div className="nav-container">
          <h1 className="logo">미스터 대박</h1>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            홈으로
          </button>
        </div>
      </nav>

      <div className="container">
        <h2>주문 내역</h2>
        {error && (
          <div className="error" style={{ 
            whiteSpace: 'pre-line', 
            padding: '15px', 
            marginBottom: '20px',
            backgroundColor: '#ffebee',
            border: '1px solid #f44336',
            borderRadius: '4px',
            color: '#c62828',
            fontSize: '14px',
            lineHeight: '1.6'
          }}>
            <strong>오류 발생:</strong><br />
            {error}
            <br /><br />
            <small style={{ color: '#666' }}>
              브라우저 개발자 도구(F12)의 Console 탭에서 더 자세한 정보를 확인할 수 있습니다.
            </small>
          </div>
        )}

        {orders.length === 0 ? (
          <div className="no-orders">
            <p>주문 내역이 없습니다.</p>
            <button onClick={() => navigate('/order')} className="btn btn-primary">
              주문하기
            </button>
          </div>
        ) : (
          <div className="orders-list">
            {orders.map(order => (
              <div key={order.id} className="order-card">
                <div className="order-header">
                  <h3>{order.dinner_name}</h3>
                  <span className={`status-badge ${getStatusClass(order.status)}`}>
                    {getStatusLabel(order.status)}
                  </span>
                </div>

                <div className="order-details">
                  <div className="detail-row">
                    <span className="label">서빙 스타일:</span>
                    <span>{getStyleLabel(order.serving_style)}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">주문 시간:</span>
                    <span>{new Date(order.created_at).toLocaleString('ko-KR')}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">배달 시간:</span>
                    <span>{new Date(order.delivery_time).toLocaleString('ko-KR')}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">배달 주소:</span>
                    <span>{order.delivery_address}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">결제 상태:</span>
                    <span>{order.payment_status === 'paid' ? '결제 완료' : '결제 대기'}</span>
                  </div>
                </div>

                <div className="order-items-section">
                  <h4>주문 항목:</h4>
                  <ul>
                    {order.items.map(item => (
                      <li key={item.id}>
                        {item.name} x{item.quantity} - {(item.price * item.quantity).toLocaleString()}원
                      </li>
                    ))}
                  </ul>
                </div>

                <div className="order-total">
                  <strong>총 가격: {order.total_price.toLocaleString()}원</strong>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Orders;

