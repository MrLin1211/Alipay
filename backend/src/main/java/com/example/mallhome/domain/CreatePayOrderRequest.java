package com.example.mallhome.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public class CreatePayOrderRequest {

    private String orderNo;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    @NotBlank
    @Size(max = 128)
    private String subject;

    private String clientIp;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer typeIndex = 2;

    @NotNull
    @Min(1)
    @Max(9)
    private Integer goodsType = 1;

    @Pattern(regexp = "ALIPAY_CN|ALIPAY|WECHATPAY|CARD")
    private String payMethodType;

    @Size(max = 256)
    private String attachInfo;

    @URL
    @Size(max = 512)
    private String quitUrl;

    @URL
    @Size(max = 512)
    private String returnUrl;

    @Size(max = 64)
    private String subExternalId;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getTypeIndex() {
        return typeIndex;
    }

    public void setTypeIndex(Integer typeIndex) {
        this.typeIndex = typeIndex;
    }

    public Integer getGoodsType() {
        return goodsType;
    }

    public void setGoodsType(Integer goodsType) {
        this.goodsType = goodsType;
    }

    public String getPayMethodType() {
        return payMethodType;
    }

    public void setPayMethodType(String payMethodType) {
        this.payMethodType = payMethodType;
    }

    public String getAttachInfo() {
        return attachInfo;
    }

    public void setAttachInfo(String attachInfo) {
        this.attachInfo = attachInfo;
    }

    public String getQuitUrl() {
        return quitUrl;
    }

    public void setQuitUrl(String quitUrl) {
        this.quitUrl = quitUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getSubExternalId() {
        return subExternalId;
    }

    public void setSubExternalId(String subExternalId) {
        this.subExternalId = subExternalId;
    }
}
