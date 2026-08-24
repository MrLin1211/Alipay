package com.example.paymentgateway.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^https?://\\S+$", message = "返回地址必须是有效的HTTP或HTTPS地址")
    private String returnUrl;

    @Size(max = 512, message = "业务通知地址不能超过512个字符")
    private String businessNotifyUrl;

    private Integer typeIndex;

    private Integer goodsType;

    @Size(max = 64, message = "支付方式不能超过64个字符")
    private String payMethodType;

    @Size(max = 64, message = "客户端IP不能超过64个字符")
    private String clientIp;

    @Size(max = 512, message = "退出地址不能超过512个字符")
    private String quitUrl;

    private Integer isShort;

    private Integer isQr;

    @Size(max = 512, message = "附加信息不能超过512个字符")
    private String attachInfo;

    @Size(max = 64, message = "子商家编号不能超过64个字符")
    private String subExternalId;

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
    public Integer getTypeIndex() { return typeIndex; }
    public void setTypeIndex(Integer typeIndex) { this.typeIndex = typeIndex; }
    public Integer getGoodsType() { return goodsType; }
    public void setGoodsType(Integer goodsType) { this.goodsType = goodsType; }
    public String getPayMethodType() { return payMethodType; }
    public void setPayMethodType(String payMethodType) { this.payMethodType = payMethodType; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getQuitUrl() { return quitUrl; }
    public void setQuitUrl(String quitUrl) { this.quitUrl = quitUrl; }
    public Integer getIsShort() { return isShort; }
    public void setIsShort(Integer isShort) { this.isShort = isShort; }
    public Integer getIsQr() { return isQr; }
    public void setIsQr(Integer isQr) { this.isQr = isQr; }
    public String getAttachInfo() { return attachInfo; }
    public void setAttachInfo(String attachInfo) { this.attachInfo = attachInfo; }
    public String getSubExternalId() { return subExternalId; }
    public void setSubExternalId(String subExternalId) { this.subExternalId = subExternalId; }
}
