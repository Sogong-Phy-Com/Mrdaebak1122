import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Heart, Coffee, Egg, Sparkles, ChevronRight } from 'lucide-react';
import { RadioGroup, RadioGroupItem } from './ui/radio-group';
import { Label } from './ui/label';

interface MenuSelectionProps {
  onAddToOrder: (item: any) => void;
  onNavigate: (screen: string) => void;
}

const dinnerMenus = [
  {
    id: 'valentine',
    name: '발렌타인 디너',
    price: 85000,
    icon: Heart,
    description: '하트 모양과 큐피드가 장식된 접시',
    items: ['와인', '스테이크', '하트 장식'],
    color: 'bg-pink-100 text-pink-600'
  },
  {
    id: 'french',
    name: '프렌치 디너',
    price: 95000,
    icon: Coffee,
    description: '프랑스식 정찬 코스',
    items: ['커피', '와인', '샐러드', '스테이크'],
    color: 'bg-blue-100 text-blue-600'
  },
  {
    id: 'english',
    name: '잉글리시 디너',
    price: 75000,
    icon: Egg,
    description: '영국식 브런치 스타일',
    items: ['에그 스크램블', '베이컨', '빵', '스테이크'],
    color: 'bg-amber-100 text-amber-600'
  },
  {
    id: 'champagne',
    name: '샴페인 축제 디너',
    price: 195000,
    icon: Sparkles,
    description: '2인분 특별 세트',
    items: ['샴페인 1병', '바게트빵 4개', '커피포트', '와인', '스테이크'],
    color: 'bg-purple-100 text-purple-600',
    specialRestriction: true
  }
];

const servingStyles = [
  {
    id: 'simple',
    name: '심플',
    priceMultiplier: 0,
    description: '플라스틱 접시와 컵, 종이 냅킨, 플라스틱 쟁반',
    available: ['valentine', 'french', 'english', 'champagne']
  },
  {
    id: 'grand',
    name: '그랜드',
    priceMultiplier: 0.15,
    description: '도자기 접시와 컵, 흰색 면 냅킨, 나무 쟁반',
    available: ['valentine', 'french', 'english', 'champagne']
  },
  {
    id: 'deluxe',
    name: '디럭스',
    priceMultiplier: 0.30,
    description: '꽃병, 도자기 접시와 컵, 린넨 냅킨, 나무 쟁반',
    available: ['valentine', 'french', 'english', 'champagne']
  }
];

export function MenuSelection({ onAddToOrder, onNavigate }: MenuSelectionProps) {
  const [selectedMenu, setSelectedMenu] = useState<string | null>(null);
  const [selectedStyle, setSelectedStyle] = useState<string>('simple');
  const [quantity, setQuantity] = useState(1);

  const selectedDinner = dinnerMenus.find(m => m.id === selectedMenu);
  const selectedServingStyle = servingStyles.find(s => s.id === selectedStyle);

  const calculatePrice = () => {
    if (!selectedDinner || !selectedServingStyle) return 0;
    return selectedDinner.price * (1 + selectedServingStyle.priceMultiplier) * quantity;
  };

  const handleAddToOrder = () => {
    if (selectedDinner && selectedServingStyle) {
      const orderItem = {
        id: Date.now(),
        dinner: selectedDinner,
        servingStyle: selectedServingStyle,
        quantity: quantity,
        totalPrice: calculatePrice(),
        orderedAt: new Date().toISOString()
      };
      onAddToOrder(orderItem);
      onNavigate('order');
    }
  };

  const isStyleAvailable = (styleId: string) => {
    if (!selectedDinner) return true;
    if (selectedDinner.specialRestriction && styleId === 'simple') {
      return false;
    }
    return true;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 pb-20">
      <div className="bg-gradient-to-r from-orange-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
        <h1 className="text-2xl mb-2">메뉴 선택</h1>
        <p className="opacity-90">원하시는 디너를 선택하세요</p>
      </div>

      <div className="p-4 space-y-6 mt-6">
        <div>
          <h3 className="mb-4">디너 타입</h3>
          <div className="space-y-3">
            {dinnerMenus.map((menu) => {
              const Icon = menu.icon;
              const isSelected = selectedMenu === menu.id;
              
              return (
                <Card 
                  key={menu.id}
                  className={`cursor-pointer transition-all ${isSelected ? 'ring-2 ring-orange-500 shadow-lg' : ''}`}
                  onClick={() => setSelectedMenu(menu.id)}
                >
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className={`w-12 h-12 rounded-full ${menu.color} flex items-center justify-center`}>
                          <Icon className="w-6 h-6" />
                        </div>
                        <div>
                          <CardTitle className="text-base">{menu.name}</CardTitle>
                          <p className="text-orange-600 mt-1">₩{menu.price.toLocaleString()}</p>
                        </div>
                      </div>
                      {menu.specialRestriction && (
                        <Badge variant="secondary">2인분</Badge>
                      )}
                    </div>
                  </CardHeader>
                  <CardContent>
                    <CardDescription className="mb-2">{menu.description}</CardDescription>
                    <div className="flex flex-wrap gap-2">
                      {menu.items.map((item, idx) => (
                        <Badge key={idx} variant="outline" className="text-xs">
                          {item}
                        </Badge>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </div>

        {selectedMenu && (
          <>
            <div>
              <h3 className="mb-4">서빙 스타일</h3>
              <RadioGroup value={selectedStyle} onValueChange={setSelectedStyle}>
                <div className="space-y-3">
                  {servingStyles.map((style) => {
                    const available = isStyleAvailable(style.id);
                    
                    return (
                      <Card 
                        key={style.id}
                        className={`${!available ? 'opacity-50' : 'cursor-pointer'} ${selectedStyle === style.id ? 'ring-2 ring-orange-500' : ''}`}
                      >
                        <CardHeader>
                          <div className="flex items-center gap-3">
                            <RadioGroupItem value={style.id} id={style.id} disabled={!available} />
                            <div className="flex-1">
                              <Label htmlFor={style.id} className="cursor-pointer">
                                <div className="flex items-center justify-between mb-1">
                                  <span>{style.name}</span>
                                  {style.priceMultiplier > 0 && (
                                    <span className="text-orange-600">+{(style.priceMultiplier * 100)}%</span>
                                  )}
                                </div>
                                <p className="text-sm text-muted-foreground">{style.description}</p>
                              </Label>
                            </div>
                          </div>
                        </CardHeader>
                      </Card>
                    );
                  })}
                </div>
              </RadioGroup>
              {selectedDinner?.specialRestriction && (
                <p className="text-sm text-muted-foreground mt-2">
                  * 샴페인 축제 디너는 그랜드 또는 디럭스 스타일만 선택 가능합니다
                </p>
              )}
            </div>

            <div>
              <h3 className="mb-4">수량</h3>
              <div className="flex items-center gap-4">
                <Button 
                  variant="outline" 
                  size="icon"
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                >
                  -
                </Button>
                <span className="w-12 text-center">{quantity}</span>
                <Button 
                  variant="outline" 
                  size="icon"
                  onClick={() => setQuantity(Math.min(10, quantity + 1))}
                >
                  +
                </Button>
              </div>
            </div>

            <Card className="bg-orange-50 border-orange-200">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between mb-4">
                  <span>기본가격</span>
                  <span>₩{(selectedDinner.price * quantity).toLocaleString()}</span>
                </div>
                {selectedServingStyle && selectedServingStyle.priceMultiplier > 0 && (
                  <div className="flex items-center justify-between mb-4 text-sm text-muted-foreground">
                    <span>서빙 스타일 추가 (+{(selectedServingStyle.priceMultiplier * 100)}%)</span>
                    <span>₩{(selectedDinner.price * selectedServingStyle.priceMultiplier * quantity).toLocaleString()}</span>
                  </div>
                )}
                <div className="flex items-center justify-between pt-4 border-t border-orange-200">
                  <span>총 금액</span>
                  <span className="text-orange-600">₩{calculatePrice().toLocaleString()}</span>
                </div>
              </CardContent>
            </Card>

            <Button 
              className="w-full" 
              size="lg"
              onClick={handleAddToOrder}
            >
              주문하기
              <ChevronRight className="ml-2 w-5 h-5" />
            </Button>
          </>
        )}
      </div>
    </div>
  );
}
