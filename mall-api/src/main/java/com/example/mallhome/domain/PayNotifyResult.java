package com.example.mallhome.domain;

public class PayNotifyResult {

    private final boolean success;
    private final String message;

    private PayNotifyResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static PayNotifyResult success() {
        return new PayNotifyResult(true, "success");
    }

    public static PayNotifyResult fail(String message) {
        return new PayNotifyResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
