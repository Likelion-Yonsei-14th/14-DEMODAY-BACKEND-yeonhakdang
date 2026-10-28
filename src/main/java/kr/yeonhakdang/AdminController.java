package kr.yeonhakdang;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    record ContentInput(@NotBlank @Size(max=80) String version,@NotBlank @Size(max=48000) String json) {}
    record ReconcileInput(@NotBlank @Size(max=120) String cancellationId) {}
    private final ContentService content;private final RefundService refunds;private final OrderService orders;private final Store db;
    public AdminController(ContentService content,RefundService refunds,OrderService orders,Store db) {this.content=content;this.refunds=refunds;this.orders=orders;this.db=db;}
    @PutMapping("/orders/{id}/products/{product}/content") void publish(Authentication a,@PathVariable UUID id,@PathVariable String product,@Valid @RequestBody ContentInput input) {content.publish(ApiController.user(a),id,product,input.version(),input.json());}
    @GetMapping("/refunds") Object queue() {return db.rows("select id,order_id,reason,status,amount,created_at,reviewed_at,cancellation_id from refund_request order by created_at");}
    @PostMapping("/refunds/{id}/process") Object process(Authentication a,@PathVariable UUID id) {return refunds.process(ApiController.user(a),id);}
    @PostMapping("/refunds/{id}/reject") void reject(Authentication a,@PathVariable UUID id) {refunds.reject(ApiController.user(a),id);}
    @PostMapping("/refunds/{id}/reconcile") Object reconcile(Authentication a,@PathVariable UUID id,@Valid @RequestBody ReconcileInput input) {return refunds.reconcile(ApiController.user(a),id,input.cancellationId());}
    @PostMapping("/orders/{id}/reconcile") Object sync(@PathVariable UUID id) {return orders.sync(id);}
}
