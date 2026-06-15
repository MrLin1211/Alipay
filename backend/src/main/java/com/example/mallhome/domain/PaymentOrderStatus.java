package com.example.mallhome.domain;

public final class PaymentOrderStatus {

    public static final String CREATED = "CREATED";
    public static final String CREATE_SUCCESS = "CREATE_SUCCESS";
    public static final String CREATE_FAILED = "CREATE_FAILED";
    public static final String SUCCESS = "SUCCESS";
    public static final String CLOSED = "CLOSED";
    public static final String FINISHED = "FINISHED";
    public static final String UNKNOWN_NOTIFY = "UNKNOWN_NOTIFY";

    private PaymentOrderStatus() {
    }

    public static String fromTradeStatus(String tradeStatus) {
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            return SUCCESS;
        }
        if ("TRADE_FINISHED".equals(tradeStatus)) {
            return FINISHED;
        }
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            return CLOSED;
        }
        return UNKNOWN_NOTIFY;
    }
}
