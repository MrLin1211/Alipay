package com.example.mallhome.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class MallhomeSignUtils {

    private MallhomeSignUtils() {
    }

    public static String buildVisitAuth(String md5Key, String aesKey, String timeStamp) {
        validateAesKey(aesKey);
        // visitAuth = AES(MD5(md5Key + ":" + timeStamp), aesKey)，IV 使用 aesKey 前 16 位。
        String md5Value = CryptoUtils.md5LowerHex(md5Key + ":" + timeStamp);
        return CryptoUtils.aesCbcPkcs5Base64Encrypt(md5Value, aesKey, aesKey.substring(0, 16));
    }

    public static String buildSign(Map<String, String> paramsWithoutSign, String visitAuth, String aesKey) {
        validateAesKey(aesKey);
        // 签名规则：参数按字母升序拼接 key=value&key=value，再追加 visitAuth 和 aesKey 前 12 位。
        String sorted = new TreeMap<>(paramsWithoutSign).entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !"pltNotifySign".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        return CryptoUtils.md5LowerHex(sorted + visitAuth + aesKey.substring(0, 12));
    }

    public static boolean verifyVisitAuth(String md5Key, String aesKey, String timeStamp, String visitAuth) {
        if (timeStamp == null || visitAuth == null) {
            return false;
        }
        return buildVisitAuth(md5Key, aesKey, timeStamp).equals(visitAuth);
    }

    public static boolean verifyNotifySign(
            Map<String, String> notifyParams,
            String visitAuth,
            String aesKey,
            String pltNotifySign
    ) {
        if (pltNotifySign == null) {
            return false;
        }
        return buildSign(notifyParams, visitAuth, aesKey).equalsIgnoreCase(pltNotifySign);
    }

    private static void validateAesKey(String aesKey) {
        if (aesKey == null || aesKey.length() < 16) {
            throw new IllegalArgumentException("aesKey length must be at least 16");
        }
        int length = aesKey.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalArgumentException("AES key bytes length must be 16, 24 or 32");
        }
    }
}
