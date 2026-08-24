package com.example.paymentgateway.service;

import com.example.paymentgateway.repository.GatewayJdbcRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlipayGatewayServiceTest {

    private final StubRepository repository = new StubRepository();
    private final AlipayGatewayService service = new AlipayGatewayService(repository, null, null);

    @Test
    void usesMerchantOrderNoFromCallbackWhenProvided() {
        String merchantOrderNo = service.resolveMerchantOrderNo(
                Map.of("merchantOrderNo", "BIZ202608230001"),
                "GW202608230001"
        );

        assertEquals("BIZ202608230001", merchantOrderNo);
        assertEquals(0, repository.findOneCalls);
    }

    @Test
    void resolvesMerchantOrderNoFromGatewayOrderWhenCallbackOmitsIt() {
        repository.result = Optional.of(Map.of("merchant_order_no", "BIZ202608230002"));

        String merchantOrderNo = service.resolveMerchantOrderNo(Map.of(), "GW202608230002");

        assertEquals("BIZ202608230002", merchantOrderNo);
        assertEquals(1, repository.findOneCalls);
        assertEquals("GW202608230002", repository.lastArgument);
    }

    @Test
    void usesPlatformTradeNoFromCallbackWhenProvided() {
        String platformTradeNo = service.resolvePlatformTradeNo(
                Map.of("platTradeNo", "XD202608230001"),
                "GW202608230003"
        );

        assertEquals("XD202608230001", platformTradeNo);
        assertEquals(0, repository.findOneCalls);
    }

    @Test
    void resolvesPlatformTradeNoFromGatewayOrderWhenCallbackOmitsIt() {
        repository.result = Optional.of(Map.of("platform_trade_no", "XD202608230002"));

        String platformTradeNo = service.resolvePlatformTradeNo(Map.of(), "GW202608230004");

        assertEquals("XD202608230002", platformTradeNo);
        assertEquals(1, repository.findOneCalls);
        assertEquals("GW202608230004", repository.lastArgument);
    }

    private static final class StubRepository extends GatewayJdbcRepository {
        private Optional<Map<String, Object>> result = Optional.empty();
        private int findOneCalls;
        private Object lastArgument;

        private StubRepository() {
            super(null);
        }

        @Override
        public Optional<Map<String, Object>> findOne(String sql, Object... args) {
            findOneCalls++;
            lastArgument = args.length == 0 ? null : args[0];
            return result;
        }
    }
}
