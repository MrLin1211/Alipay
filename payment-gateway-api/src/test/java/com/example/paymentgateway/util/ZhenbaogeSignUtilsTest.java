package com.example.paymentgateway.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZhenbaogeSignUtilsTest {

    @Test
    void generatesVisitAuthFromOfficialDocumentVector() {
        assertEquals(
                "YONYDFShHBaK9iQzlDnAHh9aJVALqvSshb4K5S+IMBQf1dT/iHE7lKMSSUlDmABZ",
                ZhenbaogeSignUtils.visitAuth("12345678901", "987654321123456789987654", "1714988944")
        );
    }

    @Test
    void generatesSignFromOfficialDocumentVector() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("clientIp", "188.215.31.152");
        params.put("externalId", "11111111");
        params.put("goodsType", 9);
        params.put("merchantPayNotifyUrl", "http://www.test.cc/api/Notify");
        params.put("merchantSubject", "本地测试订单");
        params.put("merchantTradeNo", "2021030311490830019");
        params.put("payChannel", 1);
        params.put("totalAmount", "0.1");
        params.put("typeIndex", 1);

        assertEquals(
                "4264657cb61c406829a746860c4ca57c",
                ZhenbaogeSignUtils.sign(
                        params,
                        "YONYDFShHBaK9iQzlDnAHh9aJVALqvSshb4K5S+IMBQf1dT/iHE7lKMSSUlDmABZ",
                        "987654321123456789987654"
                )
        );
    }
}
