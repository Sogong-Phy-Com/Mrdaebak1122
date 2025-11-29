import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Clock, MapPin, ShoppingBag } from 'lucide-react';

interface OrderHistoryProps {
  orders: any[];
}

export function OrderHistory({ orders }: OrderHistoryProps) {
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'preparing':
        return <Badge variant="secondary">준비중</Badge>;
      case 'delivering':
        return <Badge className="bg-blue-500">배달중</Badge>;
      case 'delivered':
        return <Badge className="bg-green-500">배달완료</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${month}월 ${day}일 ${hours}:${minutes}`;
  };

  const formatDeliveryTime = (dateString: string) => {
    const date = new Date(dateString);
    date.setMinutes(date.getMinutes() + 60);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  };

  if (orders.length === 0) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 pb-20">
        <div className="bg-gradient-to-r from-orange-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
          <h1 className="text-2xl mb-2">주문 내역</h1>
          <p className="opacity-90">이전 주문을 확인하세요</p>
        </div>
        
        <div className="p-4 mt-6">
          <Card>
            <CardContent className="pt-6 text-center">
              <ShoppingBag className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
              <p className="text-muted-foreground">주문 내역이 없습니다</p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50 pb-20">
      <div className="bg-gradient-to-r from-orange-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
        <h1 className="text-2xl mb-2">주문 내역</h1>
        <p className="opacity-90">총 {orders.length}건의 주문</p>
      </div>

      <div className="p-4 space-y-4 mt-6">
        {orders.map((order, index) => (
          <Card key={index}>
            <CardHeader>
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle className="text-base">주문 #{orders.length - index}</CardTitle>
                  <CardDescription className="flex items-center gap-1 mt-1">
                    <Clock className="w-3 h-3" />
                    {formatDate(order.orderedAt)}
                  </CardDescription>
                </div>
                {getStatusBadge(order.status)}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-sm mb-2">주문 내용:</p>
                <div className="space-y-2">
                  {order.items.map((item: any, idx: number) => (
                    <div key={idx} className="flex items-center justify-between text-sm bg-muted p-2 rounded">
                      <div>
                        <p>{item.dinner.name}</p>
                        <p className="text-xs text-muted-foreground">
                          {item.servingStyle.name} × {item.quantity}
                        </p>
                      </div>
                      <span>₩{item.totalPrice.toLocaleString()}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="pt-3 border-t">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm">소계</span>
                  <span className="text-sm">₩{order.subtotal.toLocaleString()}</span>
                </div>
                
                {order.discount > 0 && (
                  <div className="flex items-center justify-between mb-2 text-green-600">
                    <span className="text-sm">할인</span>
                    <span className="text-sm">-₩{order.discount.toLocaleString()}</span>
                  </div>
                )}
                
                <div className="flex items-center justify-between">
                  <span>총 금액</span>
                  <span className="text-orange-600">₩{order.total.toLocaleString()}</span>
                </div>
              </div>

              <div className="pt-3 border-t space-y-2">
                <div className="flex items-start gap-2 text-sm">
                  <MapPin className="w-4 h-4 mt-0.5 text-muted-foreground flex-shrink-0" />
                  <div>
                    <p>{order.deliveryAddress}</p>
                    <p className="text-muted-foreground">{order.city}</p>
                  </div>
                </div>
                
                <div className="flex items-center gap-2 text-sm">
                  <Clock className="w-4 h-4 text-muted-foreground" />
                  <span>예상 배달시간: {formatDeliveryTime(order.orderedAt)}</span>
                </div>
              </div>

              {order.specialRequest && (
                <div className="pt-3 border-t">
                  <p className="text-sm text-muted-foreground mb-1">특별 요청사항:</p>
                  <p className="text-sm">{order.specialRequest}</p>
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
