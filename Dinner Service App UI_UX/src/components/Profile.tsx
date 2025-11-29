import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { User, Mail, Phone, MapPin, Award, ShoppingBag, Calendar } from 'lucide-react';
import { Button } from './ui/button';

interface ProfileProps {
  user: any;
  orderCount: number;
  onLogout: () => void;
}

export function Profile({ user, orderCount, onLogout }: ProfileProps) {
  const getCustomerTier = () => {
    if (orderCount >= 20) return { name: '플래티넘', discount: '15%', color: 'bg-purple-100 text-purple-600' };
    if (orderCount >= 10) return { name: '골드', discount: '10%', color: 'bg-amber-100 text-amber-600' };
    if (orderCount >= 5) return { name: '실버', discount: '5%', color: 'bg-gray-100 text-gray-600' };
    return { name: '일반', discount: '0%', color: 'bg-blue-100 text-blue-600' };
  };

  const tier = getCustomerTier();
  
  const nextTierInfo = () => {
    if (orderCount < 5) return { tier: '실버', remaining: 5 - orderCount, discount: '5%' };
    if (orderCount < 10) return { tier: '골드', remaining: 10 - orderCount, discount: '10%' };
    if (orderCount < 20) return { tier: '플래티넘', remaining: 20 - orderCount, discount: '15%' };
    return null;
  };

  const nextTier = nextTierInfo();

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}.${month}.${day}`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 pb-20">
      <div className="bg-gradient-to-r from-orange-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
        <h1 className="text-2xl mb-2">프로필</h1>
        <p className="opacity-90">내 정보 및 혜택 확인</p>
      </div>

      <div className="p-4 space-y-4 mt-6">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-full bg-orange-100 flex items-center justify-center">
                <User className="w-8 h-8 text-orange-600" />
              </div>
              <div>
                <CardTitle>{user.name}</CardTitle>
                <CardDescription>미스터대박디너서비스 회원</CardDescription>
              </div>
            </div>
          </CardHeader>
        </Card>

        <Card className={`${tier.color} border-0`}>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Award className="w-8 h-8" />
                <div>
                  <CardTitle className="text-base">{tier.name} 등급</CardTitle>
                  <CardDescription className="text-current opacity-75">
                    {tier.discount} 할인 혜택
                  </CardDescription>
                </div>
              </div>
              <Badge variant="secondary" className="text-sm">
                {orderCount}회 주문
              </Badge>
            </div>
          </CardHeader>
          {nextTier && (
            <CardContent>
              <div className="bg-white/50 rounded-lg p-3">
                <p className="text-sm mb-2">
                  {nextTier.tier} 등급까지 {nextTier.remaining}회 남았습니다!
                </p>
                <div className="w-full bg-white/50 rounded-full h-2">
                  <div 
                    className="bg-current h-2 rounded-full transition-all"
                    style={{ width: `${((orderCount % (nextTier.tier === '실버' ? 5 : nextTier.tier === '골드' ? 10 : 20)) / (nextTier.tier === '실버' ? 5 : nextTier.tier === '골드' ? 10 : 20)) * 100}%` }}
                  />
                </div>
              </div>
            </CardContent>
          )}
        </Card>

        <div>
          <h3 className="mb-4">개인 정보</h3>
          <Card>
            <CardContent className="pt-6 space-y-4">
              <div className="flex items-center gap-3">
                <Mail className="w-5 h-5 text-muted-foreground" />
                <div className="flex-1">
                  <p className="text-sm text-muted-foreground">이메일</p>
                  <p>{user.email}</p>
                </div>
              </div>
              
              <div className="flex items-center gap-3">
                <Phone className="w-5 h-5 text-muted-foreground" />
                <div className="flex-1">
                  <p className="text-sm text-muted-foreground">전화번호</p>
                  <p>{user.phone}</p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <MapPin className="w-5 h-5 text-muted-foreground mt-1" />
                <div className="flex-1">
                  <p className="text-sm text-muted-foreground">배달 주소</p>
                  <p>{user.address}</p>
                  <p className="text-sm text-muted-foreground mt-1">
                    {user.city}, {user.postalCode}, {user.country}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div>
          <h3 className="mb-4">주문 통계</h3>
          <div className="grid grid-cols-2 gap-4">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center">
                    <ShoppingBag className="w-5 h-5 text-orange-600" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">총 주문</p>
                    <p className="text-orange-600">{orderCount}회</p>
                  </div>
                </div>
              </CardContent>
            </Card>
            
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
                    <Calendar className="w-5 h-5 text-blue-600" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">마지막 로그인</p>
                    <p className="text-blue-600 text-sm">{formatDate(user.lastLogin)}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">등급별 혜택 안내</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center justify-between text-sm">
              <span>실버 (5회 이상)</span>
              <Badge variant="outline">5% 할인</Badge>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span>골드 (10회 이상)</span>
              <Badge variant="outline">10% 할인</Badge>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span>플래티넘 (20회 이상)</span>
              <Badge variant="outline">15% 할인</Badge>
            </div>
          </CardContent>
        </Card>

        <Button 
          variant="outline" 
          className="w-full"
          onClick={onLogout}
        >
          로그아웃
        </Button>
      </div>
    </div>
  );
}
