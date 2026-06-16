package com.example.paymentgateway.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class AlipaySignUtils {

    private AlipaySignUtils() {
    }

    public static String buildContent(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .filter(entry -> !"sign".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    public static String signRsa2(Map<String, String> params, String privateKeyPem) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(readPrivateKey(privateKeyPem));
            signature.update(buildContent(params).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception exception) {
            throw new IllegalStateException("支付宝RSA2签名失败", exception);
        }
    }

    public static boolean verifyRsa2(Map<String, String> params, String alipayPublicKeyPem, String sign) {
        if (sign == null || sign.isBlank()) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(readPublicKey(alipayPublicKeyPem));
            signature.update(buildContent(params).getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception exception) {
            return false;
        }
    }

    public static String formPostHtml(String gatewayUrl, Map<String, String> params) {
        String inputs = new TreeMap<>(params).entrySet().stream()
                .map(entry -> "<input type=\"hidden\" name=\"" + escapeHtml(entry.getKey()) + "\" value=\""
                        + escapeHtml(entry.getValue()) + "\"/>")
                .collect(Collectors.joining("\n"));
        return "<form id=\"alipay_submit\" name=\"alipay_submit\" action=\"" + escapeHtml(gatewayUrl)
                + "?charset=" + urlEncode(params.getOrDefault("charset", "utf-8"))
                + "\" method=\"POST\">\n"
                + inputs
                + "\n</form>\n<script>document.forms['alipay_submit'].submit();</script>";
    }

    private static PrivateKey readPrivateKey(String pem) throws Exception {
        String key = cleanPem(pem);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key)));
    }

    private static PublicKey readPublicKey(String pem) throws Exception {
        String key = cleanPem(pem);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
    }

    private static String cleanPem(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
