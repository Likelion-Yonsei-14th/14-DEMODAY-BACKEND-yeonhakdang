package kr.yeonhakdang;

import java.time.Instant;
import java.util.List;

public interface PaymentGateway {
    record Cancellation(String id,String status,int amount) {}
    record Payment(String id,String status,int total,int cancelled,String currency,String storeId,
                   String orderId,Instant paidAt,List<Cancellation> cancellations) {}
    Payment get(String paymentId);
    Cancellation cancel(String paymentId,int amount,int remaining,String reason);
}
