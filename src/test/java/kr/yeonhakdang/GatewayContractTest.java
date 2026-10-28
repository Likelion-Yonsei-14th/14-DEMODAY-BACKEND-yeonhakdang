package kr.yeonhakdang;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class GatewayContractTest {
    @Test void officialGetAndCancelShapesOverLocalHttpOnly() throws Exception {
        String id="yhd-"+UUID.randomUUID();var received=new AtomicReference<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/payments/"+id,exchange->{
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("PortOne fixture-secret");
            String body;
            if("GET".equals(exchange.getRequestMethod())) {
                assertThat(exchange.getRequestURI().getQuery()).isEqualTo("storeId=store-fixture");
                body="{\"id\":\""+id+"\",\"status\":\"PAID\",\"storeId\":\"store-fixture\",\"currency\":\"KRW\",\"amount\":{\"total\":990,\"cancelled\":0},\"customData\":\"{\\\"orderId\\\":\\\"order-fixture\\\"}\",\"paidAt\":\"2026-10-28T12:00:00Z\",\"cancellations\":[]}";
            } else {
                received.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
                body="{\"cancellation\":{\"id\":\"c-fixture\",\"status\":\"REQUESTED\",\"totalAmount\":990}}";
            }
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {
            var json=new ObjectMapper();var gateway=new PortOneGateway(json,"fixture-secret","store-fixture","http://127.0.0.1:"+server.getAddress().getPort());
            var p=gateway.get(id);assertThat(p.total()).isEqualTo(990);assertThat(p.orderId()).isEqualTo("order-fixture");
            assertThat(gateway.cancel(id,990,990,"NOT_PROVIDED").status()).isEqualTo("REQUESTED");
            var cancel=json.readTree(received.get());assertThat(cancel.path("amount").asInt()).isEqualTo(990);assertThat(cancel.path("currentCancellableAmount").asInt()).isEqualTo(990);
        } finally {server.stop(0);}
    }
    @Test void providerUnavailableReturnsSanitizedError() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{byte[] bytes="private-provider-error".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(503,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
        try {
            var g=new PortOneGateway(new ObjectMapper(),"fixture-secret","store-fixture","http://127.0.0.1:"+server.getAddress().getPort());
            assertThatThrownBy(()->g.get("yhd-"+UUID.randomUUID())).isInstanceOf(ApiException.class).hasMessage("PORTONE_UNAVAILABLE");
        } finally {server.stop(0);}
    }
}
