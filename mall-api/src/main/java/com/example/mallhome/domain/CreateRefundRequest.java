package com.example.mallhome.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateRefundRequest {

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于或等于0.01")
    @Digits(integer = 10, fraction = 2, message = "退款金额最多保留两位小数")
    private BigDecimal refundAmount;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 256, message = "退款原因不能超过256个字符")
    private String refundReason;

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
}
