package com.example.paymentgateway.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RefundRequest {

    @NotBlank(message = "网关订单号不能为空")
    @Size(max = 64, message = "网关订单号不能超过64个字符")
    private String gatewayOrderNo;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于或等于0.01")
    @Digits(integer = 10, fraction = 2, message = "退款金额最多保留两位小数")
    private BigDecimal refundAmount;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 128, message = "退款原因不能超过128个字符")
    private String refundReason;

    public String getGatewayOrderNo() { return gatewayOrderNo; }
    public void setGatewayOrderNo(String gatewayOrderNo) { this.gatewayOrderNo = gatewayOrderNo; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
}
