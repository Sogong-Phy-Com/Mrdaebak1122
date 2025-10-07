import { ShoppingBag, History, User, UtensilsCrossed } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';

interface DashboardProps {
  user: any;
  onNavigate: (screen: string) => void;
}

export function Dashboard({ user, onNavigate }: DashboardProps) {
  const menuItems = [
    {
      icon: ShoppingBag,
      title: '주문하기',
      description: '프리미엄 디너를 주문하세요',
      screen: 'menu',
      color: 'bg-orange-100 text-orange-600'
    },
    {
      icon: UtensilsCrossed,
      title: '메뉴 보기',
      description: '다양한 디너 메뉴',
      screen: 'menu',
      color: 'bg-red-100 text-red-600'
    },
    {
      icon: History,
      title: '주문 내역',
      description: '이전 주문 확인',
      screen: 'history',
      color: 'bg-blue-100 text-blue-600'
    },
    {
      icon: User,
      title: '프로필 설정',
      description: '내 정보 관리',
      screen: 'profile',
      color: 'bg-purple-100 text-purple-600'
    }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 pb-20">
      <div className="bg-gradient-to-r from-orange-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
        <h1 className="text-2xl mb-2">안녕하세요, {user.name}님!</h1>
        <p className="opacity-90">오늘은 어떤 디너를 즐겨보시겠어요?</p>
      </div>

      <div className="p-4 space-y-4 mt-6">
        <div className="grid grid-cols-2 gap-4">
          {menuItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <Card 
                key={index} 
                className="cursor-pointer hover:shadow-lg transition-shadow"
                onClick={() => onNavigate(item.screen)}
              >
                <CardHeader className="space-y-2">
                  <div className={`w-12 h-12 rounded-full ${item.color} flex items-center justify-center`}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <CardTitle className="text-base">{item.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <CardDescription>{item.description}</CardDescription>
                </CardContent>
              </Card>
            );
          })}
        </div>

        <Card className="bg-gradient-to-r from-orange-500 to-red-500 text-white border-0">
          <CardHeader>
            <CardTitle>특별 할인</CardTitle>
            <CardDescription className="text-white/90">
              단골 고객님께 특별한 혜택을 드립니다
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="bg-white/20 rounded-lg p-4 backdrop-blur-sm">
              <p className="mb-2">현재 등급: {
                user.orderCount >= 20 ? '플래티넘 (15% 할인)' :
                user.orderCount >= 10 ? '골드 (10% 할인)' :
                user.orderCount >= 5 ? '실버 (5% 할인)' : '일반'
              }</p>
              <p className="text-sm opacity-90">총 주문 횟수: {user.orderCount}회</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>이용 안내</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-orange-500"></div>
              <p className="text-sm">주문 후 60분 내 배달</p>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-orange-500"></div>
              <p className="text-sm">프리미엄 식재료 사용</p>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-orange-500"></div>
              <p className="text-sm">다양한 서빙 스타일 선택</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
