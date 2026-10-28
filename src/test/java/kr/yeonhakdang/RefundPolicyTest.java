package kr.yeonhakdang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class RefundPolicyTest {
    @ParameterizedTest
    @CsvSource({"none,3790","compat,1891","benefactor,2546","charm,3141","compat+benefactor,648","compat+charm,1243","benefactor+charm,1898","compat+benefactor+charm,0"})
    void allDocumentExamples(String opened,int amount) {
        var unopened=new HashSet<>(RefundPolicy.PRICES.keySet());
        unopened.removeAll(Arrays.asList(opened.split("\\+")));
        assertThat(RefundPolicy.packageAmount(unopened)).isEqualTo(amount);
    }
    @Test void exactSevenDayBoundary() {
        var paid=Instant.parse("2026-10-28T14:59:59Z");
        assertThat(RefundPolicy.inWindow(paid,paid.plus(Duration.ofDays(7)))).isTrue();
        assertThat(RefundPolicy.inWindow(paid,paid.plus(Duration.ofDays(7)).plusNanos(1))).isFalse();
        assertThat(RefundPolicy.inWindow(paid,paid.minusSeconds(1))).isFalse();
    }
}
