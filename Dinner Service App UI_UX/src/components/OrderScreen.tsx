import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Textarea } from './ui/textarea';
import { Label } from './ui/label';
import { Badge } from './ui/badge';
import { Trash2, Plus, Minus, CheckCircle2 } from 'lucide-react';
import { AlertDialog, AlertDialogAction, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from './ui/alert-dialog';

interface OrderScreenProps {
  orderItems: any[];
  onUpdateOrder: (items: any[]) => void;
  onPlaceOrder: (specialRequest: string) => void;
  onNavigate: (screen: string) => void;
  user: any;
}

export function OrderScreen({ orderItems, onUpdateOrder, onPlaceOrder, onNavigate, user }: OrderScreenProps) {
  const [specialRequest, setSpecialRequest] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);

  const calculateDiscount = () => {
    if (user.orderCount >= 20) return 0.15;
    if (user.orderCount >= 10) return 0.10;
    if (user.orderCount >= 5) return 0.05;
    return 0;
  };

  const getDiscountLabel = () => {
    if (user.orderCount >= 20) return '플래티넘 15% 할인';
    if (user.orderCount >= 10) return '골드 10% 할인';
    if (user.orderCount >= 5) return '실버 5% 할인';
    return '';
  };

  const updateQuantity = (itemId: number, newQuantity: number) => {
    const updatedItems = orderItems.map(item => {
      if (item.id === itemId) {
        const pricePerUnit = item.totalPrice / item.quantity;
        return {
          ...item,
          quantity: newQuantity,
          totalPrice: pricePerUnit * newQuantity
        };
      }
      return item;
    });
    onUpdateOrder(updatedItems);
  };

  const removeItem = (itemId: number) => {
    const updatedItems = orderItems.filter(item => item.id !== itemId);
    onUpdateOrder(updatedItems);
  };

  const subtotal = orderItems.reduce((sum, item) => sum + item.totalPrice, 0);
  const discount = subtotal * calculateDiscount();
  const total = subtotal - discount;

  const handlePlaceOrder = () => {
    onPlaceOrder(specialRequest);
    setShowSuccess(true);
    setTimeout(() => {
      setShowSuccess(false);
      onNavigate('history');
    }, 2000);
  };

  if (orderItems.length === 0) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 pb-20">
        <div className="bg-gradient-to-r from-orange-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
          <h1 className="text-2xl mb-2">주문 확인</h1>
          <p className="opacity-90">주문 내역을 확인하세요</p>
        </div>
        
        <div className="p-4 mt-6">
          <Card>
            <CardContent className="pt-6 text-center">
              <p className="text-muted-foreground mb-4">주문 내역이 없습니다</p>
              <Button onClick={() => onNavigate('menu')}>메뉴 선택하기</Button>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 pb-20">
        <div className="bg-gradient-to-r from-orange-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
          <h1 className="text-2xl mb-2">주문 확인</h1>
          <p className="opacity-90">주문 내역을 확인하세요</p>
        </div>

        <div className="p-4 space-y-4 mt-6">
          <div>
            <h3 className="mb-4">주문 내역</h3>
            <div className="space-y-3">
              {orderItems.map((item) => {
                const Icon = item.dinner.icon;
                
                return (
                  <Card key={item.id}>
                    <CardHeader>
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-3 flex-1">
                          <div className={`w-12 h-12 rounded-full ${item.dinner.color} flex items-center justify-center`}>
                            <Icon className="w-6 h-6" />
                          </div>
                          <div className="flex-1">
                            <CardTitle className="text-base">{item.dinner.name}</CardTitle>
                            <CardDescription>{item.servingStyle.name} 스타일</CardDescription>
                          </div>
                        </div>
                        <Button 
                          variant="ghost" 
                          size="icon"
                          onClick={() => removeItem(item.id)}
                        >
                          <Trash2 className="w-4 h-4 text-destructive" />
                        </Button>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <Button 
                            variant="outline" 
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => updateQuantity(item.id, Math.max(1, item.quantity - 1))}
                          >
                            <Minus className="w-4 h-4" />
                          </Button>
                          <span className="w-8 text-center">{item.quantity}</span>
                          <Button 
                            variant="outline" 
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => updateQuantity(item.id, Math.min(10, item.quantity + 1))}
                          >
                            <Plus className="w-4 h-4" />
                          </Button>
                        </div>
                        <span className="text-orange-600">₩{item.totalPrice.toLocaleString()}</span>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
            
            <Button 
              variant="outline" 
              className="w-full mt-4"
              onClick={() => onNavigate('menu')}
            >
              <Plus className="w-4 h-4 mr-2" />
              메뉴 추가하기
            </Button>
          </div>

          <div>
            <h3 className="mb-4">배달 주소</h3>
            <Card>
              <CardContent className="pt-6">
                <p>{user.address}</p>
                <p className="text-sm text-muted-foreground mt-1">
                  {user.city}, {user.postalCode}, {user.country}
                </p>
                <p className="text-sm text-muted-foreground mt-2">
                  연락처: {user.phone}
                </p>
              </CardContent>
            </Card>
          </div>

          <div>
            <Label htmlFor="specialRequest" className="mb-2 block">특별 요청사항</Label>
            <Textarea
              id="specialRequest"
              value={specialRequest}
              onChange={(e) => setSpecialRequest(e.target.value)}
              placeholder="예: 초인종 대신 노크 부탁드립니다"
              rows={3}
            />
          </div>

          <Card className="bg-orange-50 border-orange-200">
            <CardContent className="pt-6 space-y-3">
              <div className="flex items-center justify-between">
                <span>소계</span>
                <span>₩{subtotal.toLocaleString()}</span>
              </div>
              
              {calculateDiscount() > 0 && (
                <div className="flex items-center justify-between text-green-600">
                  <div className="flex items-center gap-2">
                    <span>할인</span>
                    <Badge variant="secondary" className="text-xs">{getDiscountLabel()}</Badge>
                  </div>
                  <span>-₩{discount.toLocaleString()}</span>
                </div>
              )}
              
              <div className="flex items-center justify-between pt-3 border-t border-orange-200">
                <span>총 결제금액</span>
                <span className="text-orange-600">₩{total.toLocaleString()}</span>
              </div>
            </CardContent>
          </Card>

          <Button 
            className="w-full" 
            size="lg"
            onClick={handlePlaceOrder}
          >
            주문 완료하기
            <CheckCircle2 className="ml-2 w-5 h-5" />
          </Button>
        </div>
      </div>

      <AlertDialog open={showSuccess}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <div className="flex justify-center mb-4">
              <div className="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
              </div>
            </div>
            <AlertDialogTitle className="text-center">주문이 완료되었습니다!</AlertDialogTitle>
            <AlertDialogDescription className="text-center">
              약 60분 내에 배달될 예정입니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogAction onClick={() => {
              setShowSuccess(false);
              onNavigate('history');
            }}>
              확인
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
