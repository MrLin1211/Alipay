package com.example.paymentgateway.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateWapPayRequest {
    @NotBlank(message = "业务订单号不能为空")
    @Size(max = 64, message = "业务订单号不能超过64个字符")
    private String merchantOrderNo;

    @NotBlank(message = "订单标题不能为空")
    @Size(max = 128, message = "订单标题不能超过128个字符")
    private String subject;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于或等于0.01")
    @Digits(integer = 10, fraction = 2, message = "订单金额最多保留两位小数")
    private BigDecimal totalAmount;

    @Size(max = 512, message = "返回地址不能超过512个字符")
    private String returnUrl;

    @Size(max = 512, message = "业务通知地址不能超过512个字符")
    private String businessNotifyUrl;

    private Long customerUserId;

    @Size(max = 64, message = "用户昵称不能超过64个字符")
    private String customerDisplayName;

    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getBusinessNotifyUrl() { return businessNotifyUrl; }
    public void setBusinessNotifyUrl(String businessNotifyUrl) { this.businessNotifyUrl = businessNotifyUrl; }
    public Long getCustomerUserId() { return customerUserId; }
    public void setCustomerUserId(Long customerUserId) { this.customerUserId = customerUserId; }
    public String getCustomerDisplayName() { return customerDisplayName; }
    public void setCustomerDisplayName(String customerDisplayName) { this.customerDisplayName = customerDisplayName; }
}
