package com.example.testpay.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateTestPayRequest {

    @NotBlank(message = "订单标题不能为空")
    @Size(max = 128, message = "订单标题不能超过128个字符")
    private String subject;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于或等于0.01")
    @Digits(integer = 10, fraction = 2, message = "支付金额最多保留两位小数")
    private BigDecimal totalAmount;

    private Integer typeIndex;

    @Size(max = 64, message = "支付方式不能超过64个字符")
    private String payMethodType;

    @Size(max = 512, message = "附加信息不能超过512个字符")
    private String attachInfo;

    @Size(max = 512, message = "同步跳转地址不能超过512个字符")
    @Pattern(regexp = "^https?://\\S+$", message = "同步跳转地址必须是有效的HTTP或HTTPS地址")
    private String returnUrl;

    @Size(max = 512, message = "中途退出地址不能超过512个字符")
    @Pattern(regexp = "^https?://\\S+$", message = "中途退出地址必须是有效的HTTP或HTTPS地址")
    private String quitUrl;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getTypeIndex() { return typeIndex; }
    public void setTypeIndex(Integer typeIndex) { this.typeIndex = typeIndex; }
    public String getPayMethodType() { return payMethodType; }
    public void setPayMethodType(String payMethodType) { this.payMethodType = payMethodType; }
    public String getAttachInfo() { return attachInfo; }
    public void setAttachInfo(String attachInfo) { this.attachInfo = attachInfo; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getQuitUrl() { return quitUrl; }
    public void setQuitUrl(String quitUrl) { this.quitUrl = quitUrl; }
}
