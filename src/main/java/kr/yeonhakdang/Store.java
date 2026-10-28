package kr.yeonhakdang;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class Store {
    private final JdbcClient jdbc;
    public Store(JdbcClient jdbc) { this.jdbc=jdbc; }
    private static Object[] parameters(Object[] args) {
        // PostgreSQL JDBC accepts OffsetDateTime for timestamptz, but not java.time.Instant.
        return Arrays.stream(args).map(v->v instanceof Instant i?i.atOffset(java.time.ZoneOffset.UTC):v).toArray();
    }
    public int update(String sql, Object... args) { return jdbc.sql(sql).params(parameters(args)).update(); }
    public List<Map<String,Object>> rows(String sql, Object... args) { return jdbc.sql(sql).params(parameters(args)).query().listOfRows(); }
    public Map<String,Object> one(String sql,Object... args) {
        var rows=rows(sql,args); if(rows.isEmpty()) throw ApiException.missing(); return rows.getFirst();
    }
    public Map<String,Object> member(UUID id) { return one("select * from member_account where id=? for update",id); }
    public void active(UUID id) {
        if(!"ACTIVE".equals(member(id).get("status"))) throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,"ACCOUNT_INACTIVE");
    }
    public Map<String,Object> order(UUID id) { return one("select * from purchase_order where id=? for update",id); }
    public Map<String,Object> owned(UUID owner,UUID orderId) {
        active(owner); var o=order(orderId);
        if(!owner.equals(o.get("member_id"))) throw ApiException.missing(); return o;
    }
    public static Instant instant(Object value) {
        if(value instanceof java.time.OffsetDateTime v) return v.toInstant();
        if(value instanceof Timestamp v) return v.toInstant();
        if(value instanceof Instant v) return v;
        return null;
    }
    public static int number(Map<String,Object> row,String key) { return ((Number)row.get(key)).intValue(); }
}
