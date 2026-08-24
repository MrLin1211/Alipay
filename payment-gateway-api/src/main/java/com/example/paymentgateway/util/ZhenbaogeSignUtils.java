package com.example.paymentgateway.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class ZhenbaogeSignUtils {

    private static final String AES_METHOD = "AES/CBC/PKCS5Padding";

    private ZhenbaogeSignUtils() {
    }

    public static String visitAuth(String md5Key, String aesKey, String timestamp) {
        String md5 = md5Hex(md5Key + ":" + timestamp);
        return aesEncrypt(md5, aesKey);
    }

    public static String sign(Map<String, ?> params, String visitAuth, String aesKey) {
        String content = new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !"pltNotifySign".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return md5Hex(content + visitAuth + aesKey.substring(0, 12));
    }

    public static boolean verifyNotify(Map<String, String> params, String visitAuth, String aesKey) {
        String sign = params.get("pltNotifySign");
        return sign != null && sign.equalsIgnoreCase(sign(params, visitAuth, aesKey));
    }

    private static String aesEncrypt(String plainText, String aesKey) {
        try {
            Cipher cipher = Cipher.getInstance(AES_METHOD);
            SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(aesKey.substring(0, 16).getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("生成平台visitAuth失败", exception);
        }
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                String hex = Integer.toHexString(item & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("MD5签名失败", exception);
        }
    }
}
