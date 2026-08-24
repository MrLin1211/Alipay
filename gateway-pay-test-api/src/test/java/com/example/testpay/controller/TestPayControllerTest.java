package com.example.testpay.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPayControllerTest {

    @Test
    void appendsMerchantOrderNoToReturnUrl() {
        assertEquals(
                "https://testpay.example/pay-result.html?merchantOrderNo=TEST202608230001",
                TestPayController.appendMerchantOrderNo(
                        "https://testpay.example/pay-result.html",
                        "TEST202608230001"
                )
        );
    }

    @Test
    void replacesExistingMerchantOrderNoAndKeepsOtherQueryParameters() {
        assertEquals(
                "https://testpay.example/pay-result.html?source=app&merchantOrderNo=TEST-NEW",
                TestPayController.appendMerchantOrderNo(
                        "https://testpay.example/pay-result.html?source=app&merchantOrderNo=TEST-OLD",
                        "TEST-NEW"
                )
        );
    }
}
