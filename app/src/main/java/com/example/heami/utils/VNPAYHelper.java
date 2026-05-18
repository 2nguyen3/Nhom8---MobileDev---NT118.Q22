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

/**
 * VNPAY v2.1.0 Payment URL Builder
 *
 * Quy tắc ký chữ của VNPAY (tương đương PHP sample chính thức):
 *   hashData = "fieldName=urlencode(value)&fieldName=urlencode(value)..."
 *   (fieldName KHÔNG encode vì toàn bộ là ký tự ASCII an toàn)
 *   (giá trị dùng URLEncoder.encode — khoảng trắng thành '+', giống php urlencode())
 *   SecureHash = HMAC-SHA512(hashSecret, hashData) — lowercase hex
 */
public class VNPAYHelper {

    private static final String TAG = "VNPAYHelper";

    public static String createPaymentUrl(String orderInfo, long amount, String ipAddress) {
        String vnp_TmnCode    = BuildConfig.VNP_TMN_CODE;
        String vnp_HashSecret = (BuildConfig.VNP_HASH_SECRET != null)
                ? BuildConfig.VNP_HASH_SECRET.trim() : "";

        // Kiểm tra config
        if (vnp_TmnCode == null || vnp_TmnCode.isEmpty() || "null".equals(vnp_TmnCode)) {
            Log.e(TAG, "VNP_TMN_CODE is missing/null");
            return "error:missing_config";
        }
        if (vnp_HashSecret.isEmpty()) {
            Log.e(TAG, "VNP_HASH_SECRET is missing/empty");
            return "error:missing_config";
        }

        Log.d(TAG, "TmnCode=" + vnp_TmnCode + " HashSecretLen=" + vnp_HashSecret.length());

        String vnp_Url       = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String vnp_ReturnUrl = "heami://vnpay_return";

        // Loại bỏ dấu tiếng Việt, đặc biệt, giới hạn 255 ký tự
        orderInfo = removeAccent(orderInfo);
        if (orderInfo.length() > 255) orderInfo = orderInfo.substring(0, 255);

        // Thời gian tạo giao dịch — dùng GMT+7
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        String vnp_CreateDate = sdf.format(cld.getTime());

        // Xây dựng danh sách tham số
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version",    "2.1.0");
        params.put("vnp_Command",    "pay");
        params.put("vnp_TmnCode",    vnp_TmnCode);
        params.put("vnp_Amount",     String.valueOf(amount * 100));
        params.put("vnp_CurrCode",   "VND");
        params.put("vnp_TxnRef",     String.valueOf(System.currentTimeMillis()));
        params.put("vnp_OrderInfo",  orderInfo);
        params.put("vnp_OrderType",  "other");
        params.put("vnp_Locale",     "vn");
        params.put("vnp_ReturnUrl",  vnp_ReturnUrl);
        params.put("vnp_IpAddr",     ipAddress);
        params.put("vnp_CreateDate", vnp_CreateDate);

        // Sắp xếp theo alphabet — bắt buộc với VNPAY
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query    = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) continue;

            try {
                // VNPAY v2.1.0 — giống hệt PHP urlencode():
                //   hashData: fieldName (raw) + "=" + URLEncoder(value) — '+' cho space
                //   query:    URLEncoder(name) + "=" + URLEncoder(value) — '+' cho space
                // KHÔNG replace '+' -> '%20' vì hashData phải khớp với phía VNPAY server (PHP)
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.name());
                String encodedName  = URLEncoder.encode(fieldName,  StandardCharsets.UTF_8.name());

                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }

                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedName).append('=').append(encodedValue);

            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Encoding error for: " + fieldName, e);
            }
        }

        Log.d(TAG, "hashData: " + hashData);

        String secureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        if (secureHash == null) {
            Log.e(TAG, "HMAC-SHA512 failed");
            return "error:hmac_failed";
        }

        Log.d(TAG, "secureHash: " + secureHash);

        // SecureHash lowercase — đây là output chuẩn của HMAC-SHA512 hex
        return vnp_Url + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    private static String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern p = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = p.matcher(temp).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D');
        // Chỉ giữ lại ký tự ASCII an toàn và khoảng trắng
        return result.replaceAll("[^a-zA-Z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static String hmacSHA512(final String key, final String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "HMAC-SHA512 error", e);
            return null;
        }
    }
}