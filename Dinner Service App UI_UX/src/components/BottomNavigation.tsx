import { Home, UtensilsCrossed, History, User } from 'lucide-react';

interface BottomNavigationProps {
  currentScreen: string;
  onNavigate: (screen: string) => void;
}

export function BottomNavigation({ currentScreen, onNavigate }: BottomNavigationProps) {
  const navItems = [
    { id: 'dashboard', icon: Home, label: '홈' },
    { id: 'menu', icon: UtensilsCrossed, label: '메뉴' },
    { id: 'history', icon: History, label: '주문내역' },
    { id: 'profile', icon: User, label: '프로필' }
  ];

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-border shadow-lg z-50">
      <div className="max-w-md mx-auto flex items-center justify-around">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentScreen === item.id;
          
          return (
            <button
              key={item.id}
              onClick={() => onNavigate(item.id)}
              className={`flex flex-col items-center justify-center py-3 px-4 flex-1 transition-colors ${
                isActive ? 'text-orange-600' : 'text-muted-foreground'
              }`}
            >
              <Icon className={`w-6 h-6 mb-1 ${isActive ? 'fill-orange-600' : ''}`} />
              <span className="text-xs">{item.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
