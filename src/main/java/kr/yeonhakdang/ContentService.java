package kr.yeonhakdang;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.*;

@Service
public class ContentService {
    private final Store db;private final Clock clock;private final ObjectMapper json;private final AccountService accounts;
    public ContentService(Store db,Clock clock,ObjectMapper json,AccountService accounts) {this.db=db;this.clock=clock;this.json=json;this.accounts=accounts;}
    @Transactional
    public Map<String,Object> save(UUID user,ApiController.AnalysisInput input) {
        db.active(user);accounts.requireConsent(user);
        if(!input.counts().keySet().equals(Set.of("wood","fire","earth","metal","water"))||input.counts().values().stream().anyMatch(v->v<0||v>8)
            ||!Set.of(6,8).contains(input.counts().values().stream().mapToInt(Integer::intValue).sum())) throw new IllegalArgumentException("counts");
        UUID id=UUID.randomUUID();
        db.update("insert into analysis_result(id,member_id,engine_version,summary,created_at) values(?,?,?,?,?)",id,user,input.engineVersion(),json.writeValueAsString(Map.of("counts",input.counts(),"top",input.top())),clock.instant());
        return Map.of("id",id,"provenance","CLIENT_UNVERIFIED");
    }
    @Transactional public List<Map<String,Object>> results(UUID user) {db.active(user);return db.rows("select id,engine_version,summary,provenance,created_at from analysis_result where member_id=? order by created_at desc",user);}
    @Transactional public List<Map<String,Object>> locker(UUID user) {
        db.active(user);return db.rows("select e.order_id,e.product_code,e.status,e.first_viewed_at,e.content_version,(case when e.content is null then false else true end) as content_ready from entitlement e join purchase_order o on o.id=e.order_id where o.member_id=? and e.status not in ('REFUNDED','REVOKED') order by o.created_at desc",user);
    }
    @Transactional public Map<String,Object> open(UUID user,UUID order,String product) {
        var o=db.owned(user,order);
        if(!Set.of("PAID","PARTIALLY_REFUNDED").contains(o.get("status"))) throw ApiException.conflict("ORDER_NOT_ACCESSIBLE");
        var e=db.one("select * from entitlement where order_id=? and product_code=?",order,product);
        if(!"ACTIVE".equals(e.get("status"))) throw ApiException.conflict("ENTITLEMENT_NOT_ACTIVE");
        if(e.get("content")==null) throw ApiException.conflict("CONTENT_PENDING");
        // Parse before recording: malformed/unavailable content never consumes the refund right.
        var content=json.readTree((String)e.get("content"));
        db.update("update entitlement set first_viewed_at=coalesce(first_viewed_at,?) where order_id=? and product_code=?",clock.instant(),order,product);
        return Map.of("content",content,"contentVersion",e.get("content_version"),"firstViewedAt",db.one("select first_viewed_at from entitlement where order_id=? and product_code=?",order,product).get("first_viewed_at"));
    }
    @Transactional public void publish(UUID admin,UUID order,String product,String version,String payload) {
        requireAdmin(admin);
        UUID owner=(UUID)db.one("select member_id from purchase_order where id=?",order).get("member_id");
        db.active(owner);var o=db.order(order);
        if(!"PAID".equals(o.get("status"))) throw ApiException.conflict("ORDER_NOT_PAID");
        var e=db.one("select * from entitlement where order_id=? and product_code=?",order,product);
        if(!"ACTIVE".equals(e.get("status"))||e.get("content")!=null) throw ApiException.conflict("CONTENT_IMMUTABLE");
        if(!json.readTree(payload).isObject()) throw new IllegalArgumentException("content");
        db.update("update entitlement set content=?,content_version=? where order_id=? and product_code=?",payload,version,order,product);
    }
    public void requireAdmin(UUID admin) {
        var a=db.one("select status,role from member_account where id=?",admin);
        if(!"ACTIVE".equals(a.get("status"))||!"ADMIN".equals(a.get("role"))) throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,"FORBIDDEN");
    }
}
