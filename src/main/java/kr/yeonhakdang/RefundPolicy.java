package kr.yeonhakdang;

import java.time.*;
import java.util.*;

public final class RefundPolicy {
    private RefundPolicy() {}
    public static final Map<String,Integer> PRICES=Map.of("compat",2900,"benefactor",1900,"charm",990);
    public static int packageAmount(Set<String> unopened) {
        if(!PRICES.keySet().containsAll(unopened)) throw new IllegalArgumentException("product");
        if(unopened.size()==3) return 3790;
        return (int)(3790L*unopened.stream().mapToInt(PRICES::get).sum()/5790);
    }
    public static boolean inWindow(Instant paidAt,Instant now) {
        return !now.isBefore(paidAt)&&!now.isAfter(paidAt.plus(Duration.ofDays(7)));
    }
}
