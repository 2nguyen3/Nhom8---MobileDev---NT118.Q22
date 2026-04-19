package com.example.heami.utils;

import com.example.heami.BuildConfig;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPAYHelper {

    private static final String TAG = "VNPAYHelper";

    public static String createPaymentUrl(String orderInfo, long amount, String ipAddress) {
        String vnp_Version    = "2.1.0";
        String vnp_Command    = "pay";
        String vnp_TmnCode    = BuildConfig.VNP_TMN_CODE;
        String vnp_HashSecret = BuildConfig.VNP_HASH_SECRET != null
                ? BuildConfig.VNP_HASH_SECRET.trim() : "";

        if (vnp_TmnCode == null || vnp_TmnCode.equals("null")
                || vnp_HashSecret.isEmpty()) {
            Log.e(TAG, "Missing VNPAY config in BuildConfig");
            return "error:missing_config";
        }

        String vnp_Url       = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String vnp_ReturnUrl = "heami://vnpay_return";

        // Loại bỏ dấu để tránh vấn đề encoding
        orderInfo = removeAccent(orderInfo);
        // Giới hạn 255 ký tự theo yêu cầu VNPAY
        if (orderInfo.length() > 255) orderInfo = orderInfo.substring(0, 255);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String vnp_CreateDate = formatter.format(cld.getTime());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version",   vnp_Version);
        vnp_Params.put("vnp_Command",   vnp_Command);
        vnp_Params.put("vnp_TmnCode",   vnp_TmnCode);
        vnp_Params.put("vnp_Amount",    String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode",  "VND");
        vnp_Params.put("vnp_TxnRef",    String.valueOf(System.currentTimeMillis()));
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale",    "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr",    ipAddress);
        vnp_Params.put("vnp_CreateDate",vnp_CreateDate);

        // Sắp xếp theo alphabet (bắt buộc với VNPAY)
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query    = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) continue;

            try {
                // ✅ FIX: Dùng URLEncoder trực tiếp, KHÔNG replace "+" -> "%20"
                // URLEncoder encode khoảng trắng thành "+", đây là chuẩn VNPAY dùng để verify
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.name());
                String encodedName  = URLEncoder.encode(fieldName,  StandardCharsets.UTF_8.name());

                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }

                // hashData và query dùng cùng một format
                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedName).append('=').append(encodedValue);

            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Encoding error for field: " + fieldName, e);
            }
        }

        // Debug: log hashData để so sánh nếu vẫn còn lỗi
        Log.d(TAG, "HashData to sign: " + hashData);

        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        if (vnp_SecureHash == null) {
            Log.e(TAG, "HMAC-SHA512 failed, check HashSecret");
            return "error:hmac_failed";
        }

        return vnp_Url + "?" + query + "&vnp_SecureHash=" + vnp_SecureHash;
    }

    private static String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D');
        return result.replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static String hmacSHA512(final String key, final String data) {
        try {
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) {
            Log.e(TAG, "HMAC-SHA512 exception", ex);
            return null;
        }
    }
}